package org.hackedserver.core.probing;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;

import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Packet-level sign translation prober that works on any platform
 * (Spigot, BungeeCord, Velocity) via PacketEvents.
 * <p>
 * Instead of placing a real sign block in the world (which requires
 * Bukkit world access), this sends fake packets to the client:
 * <ol>
 *   <li>Block Change - tells the client a sign exists at a position</li>
 *   <li>Block Entity Data - sends the sign's NBT with translatable text</li>
 *   <li>Open Sign Editor - opens the sign editor GUI</li>
 * </ol>
 * The client resolves translation keys and sends an UPDATE_SIGN packet
 * back with the resolved text.
 * <p>
 * This is entirely client-side - no actual blocks are modified in the world.
 * The sign is placed at a position far below the player where they can't see it.
 */
public class PacketSignProber {

    private static final Logger LOGGER = Logger.getLogger("HackedServer");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "HackedServer-ProbeScheduler");
        t.setDaemon(true);
        return t;
    });

    private final Map<UUID, ProbeSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Vector3i> playerPositions = new ConcurrentHashMap<>();
    private PacketListenerAbstract packetListener;

    /**
     * Callback interface for executing actions when a mod is detected.
     * Platform-specific implementations handle alerting, commands, etc.
     */
    @FunctionalInterface
    public interface ActionExecutor {
        void execute(UUID playerUUID, String playerName, String checkName, List<Action> actions);
    }

    private final ActionExecutor actionExecutor;
    private final boolean debugEnabled;

    private static final class ProbeSession {
        private final Vector3i signPosition;
        private final List<TranslationCheck> lineChecks;
        private final List<TranslationCheck> remainingChecks;
        private final AtomicBoolean handled = new AtomicBoolean(false);
        private volatile boolean timedOut = false;

        ProbeSession(Vector3i signPosition, List<TranslationCheck> lineChecks, List<TranslationCheck> remainingChecks) {
            this.signPosition = signPosition;
            this.lineChecks = lineChecks;
            this.remainingChecks = remainingChecks;
        }
    }

    public PacketSignProber(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
        this.debugEnabled = Config.DEBUG.toBool();
    }

    /**
     * Register the packet listener with PacketEvents.
     * Must be called after PacketEvents is initialized.
     */
    public void register() {
        packetListener = new PacketListenerAbstract(PacketListenerPriority.LOW) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
                    handleUpdateSign(event);
                }
            }

            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
                    try {
                        WrapperPlayServerPlayerPositionAndLook posPacket =
                                new WrapperPlayServerPlayerPositionAndLook(event);
                        UUID uuid = event.getUser().getUUID();
                        if (uuid != null) {
                            playerPositions.put(uuid, new Vector3i(
                                    floorBlock(posPacket.getX()),
                                    floorBlock(posPacket.getY()),
                                    floorBlock(posPacket.getZ())));
                        }
                    } catch (Exception ignored) {
                        // Best effort position tracking
                    }
                }
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    /**
     * Unregister the packet listener and clean up active sessions.
     */
    public void unregister() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
        }
        activeSessions.clear();
        playerPositions.clear();
        scheduler.shutdownNow();
    }

    /**
     * Start a sign probe for a player. Called after the player has fully joined
     * and is in the PLAY state.
     *
     * @param user The PacketEvents User object for the player
     * @param playerName The player's display name (for logging)
     */
    public void startProbe(User user, String playerName) {
        if (!ProbingConfig.isEnabled()) {
            return;
        }

        List<TranslationCheck> checks = ProbingConfig.getChecks();
        if (checks.isEmpty()) {
            return;
        }

        UUID playerUUID = user.getUUID();

        if (debugEnabled) {
            LOGGER.info("HackedServer | Scheduling packet sign probe for " + playerName
                    + " with " + checks.size() + " checks (delay: " + ProbingConfig.getDelayTicks() + " ticks)");
        }

        long delayMs = ProbingConfig.getDelayTicks() * 50L; // Convert ticks to milliseconds
        scheduler.schedule(() -> {
            try {
                doStartProbe(user, playerName, checks);
            } catch (Exception e) {
                LOGGER.warning("HackedServer | Failed to start packet sign probe for " + playerName + ": " + e.getMessage());
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Clean up any active probe session for a disconnecting player.
     */
    public void onPlayerDisconnect(UUID playerUUID) {
        playerPositions.remove(playerUUID);
        ProbeSession session = activeSessions.remove(playerUUID);
        if (session != null && debugEnabled) {
            LOGGER.info("HackedServer | Cleaned up packet probe session for " + playerUUID + " (disconnect)");
        }
    }

    private void doStartProbe(User user, String playerName, List<TranslationCheck> checks) {
        List<TranslationCheck> lineChecks = new ArrayList<>(checks.subList(0, Math.min(4, checks.size())));
        List<TranslationCheck> remainingChecks = checks.size() > 4
                ? new ArrayList<>(checks.subList(4, checks.size()))
                : List.of();
        UUID playerUUID = user.getUUID();

        // Use the player's known position to ensure the sign is in a loaded chunk.
        // Fall back to world origin if position is unknown (less reliable but still works near spawn).
        int minY = user.getMinWorldHeight();
        Vector3i knownPos = playerPositions.get(playerUUID);
        int signX = knownPos != null ? knownPos.x : 0;
        int signZ = knownPos != null ? knownPos.z : 0;
        int signY = knownPos != null
                ? Math.max(knownPos.y + ProbingConfig.getSignOffsetY(), minY)
                : minY;
        Vector3i signPos = new Vector3i(signX, signY, signZ);

        if (debugEnabled) {
            LOGGER.info("HackedServer | Starting packet sign probe for " + playerName
                    + " at " + signPos.x + ", " + signPos.y + ", " + signPos.z);
        }

        // 1. Send Block Change to place a virtual sign
        WrappedBlockState signState = WrappedBlockState.getByString(
                user.getClientVersion(), "minecraft:oak_sign");
        WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange(signPos, signState);
        user.sendPacket(blockChange);

        // 2. Send Block Entity Data with translatable text components
        NBTCompound signNBT = buildSignNBT(lineChecks);
        WrapperPlayServerBlockEntityData blockEntityData = new WrapperPlayServerBlockEntityData(
                signPos, BlockEntityTypes.SIGN, signNBT);
        user.sendPacket(blockEntityData);

        // 3. Register the probe session BEFORE opening the sign editor
        activeSessions.put(playerUUID, new ProbeSession(signPos, lineChecks, remainingChecks));

        // 4. Send Open Sign Editor after a short delay to let the block update propagate
        scheduler.schedule(() -> {
            try {
                if (!activeSessions.containsKey(playerUUID)) {
                    return; // Session was removed (player disconnected)
                }

                WrapperPlayServerOpenSignEditor openSign = new WrapperPlayServerOpenSignEditor(signPos, true);
                user.sendPacket(openSign);

                if (debugEnabled) {
                    LOGGER.info("HackedServer | Opened sign editor via packet for " + playerName);
                }

                // Timeout: clean up the virtual block but keep the session briefly to consume late UPDATE_SIGN packets.
                scheduler.schedule(() -> {
                    ProbeSession s = activeSessions.get(playerUUID);
                    if (s != null && !s.handled.get()) {
                        s.timedOut = true;
                        if (debugEnabled) {
                            LOGGER.info("HackedServer | Packet sign probe timed out for " + playerName
                                    + " (waiting for late packet)");
                        }
                        restoreVirtualBlock(user, signPos);
                    }
                }, 22000, TimeUnit.MILLISECONDS);

                scheduler.schedule(() -> {
                    ProbeSession s = activeSessions.remove(playerUUID);
                    if (s != null && !s.handled.get()) {
                        continueProbeIfNeeded(user, playerName, s);
                    }
                }, 24000, TimeUnit.MILLISECONDS);

            } catch (Exception e) {
                activeSessions.remove(playerUUID);
                LOGGER.warning("HackedServer | Failed to open sign editor for " + playerName + ": " + e.getMessage());
            }
        }, 250, TimeUnit.MILLISECONDS); // ~5 ticks delay
    }

    /**
     * Build the NBT compound for a sign with translatable text components.
     * <p>
     * Sign NBT format (1.20+):
     * <pre>
     * {
     *   "front_text": {
     *     "messages": [
     *       '{"translate":"key"}',
     *       '{"translate":"key"}',
     *       '{"translate":"key"}',
     *       '{"translate":"key"}'
     *     ],
     *     "color": "black",
     *     "has_glowing_text": 0b
     *   },
     *   "back_text": { ... same format ... },
     *   "is_waxed": 0b
     * }
     * </pre>
     */
    private NBTCompound buildSignNBT(List<TranslationCheck> checks) {
        NBTCompound root = new NBTCompound();

        // Front text with translatable components
        NBTCompound frontText = new NBTCompound();
        NBTList<NBTString> messages = NBTList.createStringList();

        for (int i = 0; i < 4; i++) {
            if (i < checks.size()) {
                TranslationCheck check = checks.get(i);
                String json;
                if (check.isBypassProtection()) {
                    // Wrap in %s substitution to bypass client-side protection
                    json = "{\"translate\":\"%s\",\"with\":[{\"translate\":\""
                            + escapeJson(check.getTranslationKey()) + "\"}]}";
                } else {
                    json = "{\"translate\":\"" + escapeJson(check.getTranslationKey()) + "\"}";
                }
                messages.addTag(new NBTString(json));

                if (debugEnabled) {
                    LOGGER.info("HackedServer | Packet probe line " + i + ": " + json
                            + " (bypass=" + check.isBypassProtection() + ")");
                }
            } else {
                messages.addTag(new NBTString("{\"text\":\"\"}"));
            }
        }

        frontText.setTag("messages", messages);
        frontText.setTag("color", new NBTString("black"));
        frontText.setTag("has_glowing_text", new NBTByte((byte) 0));
        root.setTag("front_text", frontText);

        // Back text (empty)
        NBTCompound backText = new NBTCompound();
        NBTList<NBTString> backMessages = NBTList.createStringList();
        for (int i = 0; i < 4; i++) {
            backMessages.addTag(new NBTString("{\"text\":\"\"}"));
        }
        backText.setTag("messages", backMessages);
        backText.setTag("color", new NBTString("black"));
        backText.setTag("has_glowing_text", new NBTByte((byte) 0));
        root.setTag("back_text", backText);

        root.setTag("is_waxed", new NBTByte((byte) 0));

        return root;
    }

    private void handleUpdateSign(PacketReceiveEvent event) {
        WrapperPlayClientUpdateSign updateSign = new WrapperPlayClientUpdateSign(event);
        UUID playerUUID = event.getUser().getUUID();

        ProbeSession session = activeSessions.get(playerUUID);
        if (session == null || session.handled.get()) {
            return;
        }

        // Verify this UPDATE_SIGN is for the probe sign, not a legitimate sign edit
        Vector3i signPos = updateSign.getBlockPosition();
        if (!signPos.equals(session.signPosition)) {
            return;
        }

        // Atomically claim the session to avoid race with timeout handler
        if (!session.handled.compareAndSet(false, true)) {
            return;
        }
        if (!activeSessions.remove(playerUUID, session)) {
            return;
        }

        // Cancel the packet so the backend server doesn't try to process it
        event.setCancelled(true);

        String[] lines = updateSign.getTextLines();
        User user = event.getUser();

        if (debugEnabled) {
            LOGGER.info("HackedServer | Packet probe response from " + user.getName()
                    + ": [\"" + String.join("\", \"", lines) + "\"]");
        }

        if (!session.timedOut) {
            restoreVirtualBlock(user, session.signPosition);
        }

        // Process the response
        HackedPlayer hackedPlayer = HackedServer.getPlayer(playerUUID);
        if (hackedPlayer == null) {
            return;
        }

        for (int i = 0; i < session.lineChecks.size() && i < lines.length; i++) {
            TranslationCheck check = session.lineChecks.get(i);
            String response = lines[i] != null ? lines[i].trim() : "";
            String expected = check.getExpectedVanillaResponse();

            if (debugEnabled) {
                LOGGER.info("HackedServer | Packet probe line " + i + " (" + check.getName()
                        + "): response=\"" + response + "\", expected=\"" + expected + "\"");
            }

            if (!response.isEmpty() && !response.equals(expected)) {
                if (debugEnabled) {
                    LOGGER.info("HackedServer | DETECTED: " + check.getName()
                            + " via packet sign translation probe");
                }
                String probeCheckId = "probe_" + check.getId();
                if (!hackedPlayer.hasGenericCheck(probeCheckId)) {
                    hackedPlayer.addGenericCheck(probeCheckId);
                    actionExecutor.execute(playerUUID, user.getName(), check.getName(), check.getActions());
                }
            }
        }

        continueProbeIfNeeded(user, user.getName(), session);
    }

    private void continueProbeIfNeeded(User user, String playerName, ProbeSession session) {
        if (!session.remainingChecks.isEmpty()) {
            doStartProbe(user, playerName, session.remainingChecks);
        }
    }

    private void restoreVirtualBlock(User user, Vector3i signPosition) {
        try {
            WrappedBlockState airState = WrappedBlockState.getByString(
                    user.getClientVersion(), "minecraft:air");
            user.sendPacket(new WrapperPlayServerBlockChange(signPosition, airState));
        } catch (Exception ignored) {
            // Best effort
        }
    }

    private static int floorBlock(double coordinate) {
        return (int) Math.floor(coordinate);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
