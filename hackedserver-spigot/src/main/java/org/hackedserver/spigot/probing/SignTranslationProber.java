package org.hackedserver.spigot.probing;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Config;
import org.hackedserver.core.probing.ProbingConfig;
import org.hackedserver.core.probing.TranslationCheck;
import org.hackedserver.spigot.HackedServerPlugin;
import org.hackedserver.spigot.listeners.PayloadProcessor;
import org.hackedserver.spigot.utils.logs.Logs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Active probing via the sign translation vulnerability (MC-265322).
 * <p>
 * Places a real sign block in the world with translatable text components
 * set via the Bukkit Sign API. The server natively serializes these into
 * the correct NBT format in the BlockEntityData packet. The sign editor
 * is opened via player.openSign(), and the client's response is intercepted
 * at the packet level (UPDATE_SIGN) using PacketEvents.
 * <p>
 * When the client opens the sign editor, translatable text components are
 * resolved to plain text. If a mod's translation key resolves to something
 * other than the raw key, that mod is installed.
 * <p>
 * Requires PacketEvents 2.x on the server and Paper (for Adventure API).
 */
public class SignTranslationProber implements Listener {

    private final Map<UUID, ProbeSession> activeSessions = new ConcurrentHashMap<>();
    private PacketListenerAbstract packetListener;

    private static final class ProbeSession {
        private final Location signLocation;
        private final BlockData originalBlockData;
        private final BlockState originalBlockState;
        private final List<TranslationCheck> lineChecks;
        private final List<TranslationCheck> remainingChecks;
        private volatile boolean timedOut = false;
        private final AtomicBoolean handled = new AtomicBoolean(false);
        private final AtomicBoolean restored = new AtomicBoolean(false);

        ProbeSession(Location signLocation, BlockData originalBlockData, BlockState originalBlockState,
                     List<TranslationCheck> lineChecks, List<TranslationCheck> remainingChecks) {
            this.signLocation = signLocation;
            this.originalBlockData = originalBlockData;
            this.originalBlockState = originalBlockState;
            this.lineChecks = lineChecks;
            this.remainingChecks = remainingChecks;
        }

        Location signLocation() { return signLocation; }
        BlockData originalBlockData() { return originalBlockData; }
        BlockState originalBlockState() { return originalBlockState; }
        List<TranslationCheck> lineChecks() { return lineChecks; }
        List<TranslationCheck> remainingChecks() { return remainingChecks; }
    }

    public void register() {
        packetListener = new PacketListenerAbstract(PacketListenerPriority.LOW) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
                    if (Config.DEBUG.toBool()) {
                        WrapperPlayClientUpdateSign dbg = new WrapperPlayClientUpdateSign(event);
                        UUID uid = event.getUser().getUUID();
                        boolean hasSession = activeSessions.containsKey(uid);
                        Logs.logInfo("HackedServer | Received UPDATE_SIGN packet from "
                                + uid + " (hasSession=" + hasSession + ") lines: [\""
                                + String.join("\", \"", dbg.getTextLines()) + "\"]");
                    }
                    handleUpdateSign(event);
                }
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    public void unregister() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
        }
        for (var entry : activeSessions.entrySet()) {
            restoreBlock(entry.getValue());
        }
        activeSessions.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!ProbingConfig.isEnabled()) {
            return;
        }

        List<TranslationCheck> checks = ProbingConfig.getChecks();
        if (checks.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        if (Config.DEBUG.toBool()) {
            Logs.logInfo("HackedServer | Scheduling sign probe for " + player.getName()
                    + " with " + checks.size() + " checks (delay: " + ProbingConfig.getDelayTicks() + " ticks)");
        }

        Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), () -> {
            if (!player.isOnline()) {
                return;
            }
            startProbe(player, checks);
        }, ProbingConfig.getDelayTicks());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ProbeSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            restoreBlock(session);
            if (Config.DEBUG.toBool()) {
                Logs.logInfo("HackedServer | Cleaned up probe session for " + event.getPlayer().getName() + " (quit)");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        ProbeSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            Location probeLoc = session.signLocation();
            Location signLoc = event.getBlock().getLocation();
            if (signLoc.getBlockX() == probeLoc.getBlockX()
                    && signLoc.getBlockY() == probeLoc.getBlockY()
                    && signLoc.getBlockZ() == probeLoc.getBlockZ()) {
                event.setCancelled(true);
            }
        }
    }

    private void startProbe(Player player, List<TranslationCheck> checks) {
        List<TranslationCheck> lineChecks = new ArrayList<>(checks.subList(0, Math.min(4, checks.size())));
        List<TranslationCheck> remainingChecks = checks.size() > 4
                ? new ArrayList<>(checks.subList(4, checks.size()))
                : List.of();

        Location playerLoc = player.getLocation();
        int signX = playerLoc.getBlockX();
        int signY = playerLoc.getBlockY() + ProbingConfig.getSignOffsetY();
        int signZ = playerLoc.getBlockZ();

        if (playerLoc.getWorld() != null) {
            int minY = playerLoc.getWorld().getMinHeight();
            if (signY < minY) {
                signY = minY;
            }
        }

        Location signLoc = new Location(playerLoc.getWorld(), signX, signY, signZ);
        Block block = signLoc.getBlock();

        BlockData originalData = block.getBlockData().clone();
        BlockState originalState = block.getState();

        if (Config.DEBUG.toBool()) {
            Logs.logInfo("HackedServer | Starting sign probe for " + player.getName()
                    + " at " + signX + ", " + signY + ", " + signZ);
        }

        // Place sign block (don't apply physics to avoid breaking it)
        block.setType(Material.OAK_SIGN, false);

        try {
            BlockState state = block.getState();
            if (!(state instanceof Sign sign)) {
                Logs.logWarning("HackedServer | Block at probe location is not a sign for " + player.getName());
                block.setBlockData(originalData, false);
                return;
            }

            // Set translatable text components on the sign using the Bukkit/Adventure API.
            // The server will natively serialize these into the correct JSON format
            // in the BlockEntityData packet sent to the client.
            SignSide frontSide = sign.getSide(Side.FRONT);
            for (int i = 0; i < 4; i++) {
                if (i < lineChecks.size()) {
                    TranslationCheck check = lineChecks.get(i);
                    Component line;
                    if (check.isBypassProtection()) {
                        // Wrap in %s substitution to bypass Meteor's sign translation mixin
                        line = Component.translatable("%s", Component.translatable(check.getTranslationKey()));
                    } else {
                        line = Component.translatable(check.getTranslationKey());
                    }
                    frontSide.line(i, line);

                    if (Config.DEBUG.toBool()) {
                        Logs.logInfo("HackedServer | Set sign line " + i + " to translatable: " + check.getTranslationKey()
                                + " (bypass=" + check.isBypassProtection() + ")");
                    }
                } else {
                    frontSide.line(i, Component.empty());
                }
            }
            sign.setWaxed(false);
            sign.update(true, false);

            // Register the probe session
            activeSessions.put(player.getUniqueId(),
                    new ProbeSession(signLoc, originalData, originalState, lineChecks, remainingChecks));

            // Open sign editor after a short delay to let the block update propagate
            Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), () -> {
                if (!player.isOnline()) {
                    ProbeSession s = activeSessions.remove(player.getUniqueId());
                    if (s != null) restoreBlock(s);
                    return;
                }

                BlockState currentState = block.getState();
                if (!(currentState instanceof Sign currentSign)) {
                    Logs.logWarning("HackedServer | Sign disappeared before editor opened for " + player.getName());
                    ProbeSession s = activeSessions.remove(player.getUniqueId());
                    if (s != null) restoreBlock(s);
                    return;
                }

                player.openSign(currentSign, Side.FRONT);

                if (Config.DEBUG.toBool()) {
                    Logs.logInfo("HackedServer | Opened sign editor for " + player.getName());
                }

                // Timeout: restore block but keep session for late packet arrivals
                Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), () -> {
                    ProbeSession s = activeSessions.get(player.getUniqueId());
                    if (s != null && !s.handled.get()) {
                        s.timedOut = true;
                        if (Config.DEBUG.toBool()) {
                            Logs.logInfo("HackedServer | Sign probe timed out for " + player.getName() + " (waiting for late packet)");
                        }
                        restoreBlock(s);
                    }
                }, 400L); // 20 second timeout

                // Final cleanup: remove session after grace period for late packets
                Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), () -> {
                    ProbeSession s = activeSessions.remove(player.getUniqueId());
                    if (s != null && !s.handled.get()) {
                        if (Config.DEBUG.toBool()) {
                            Logs.logInfo("HackedServer | Sign probe final cleanup for " + player.getName());
                        }
                        restoreBlock(s);
                        scheduleRemainingChecks(player, s);
                    }
                }, 440L); // 2 second grace period after timeout
            }, 5L);

        } catch (Exception e) {
            Logs.logWarning("Failed to start translation probe: " + e.getMessage());
            ProbeSession s = activeSessions.remove(player.getUniqueId());
            if (s != null) restoreBlock(s);
            else block.setBlockData(originalData, false);
        }
    }

    private void handleUpdateSign(PacketReceiveEvent event) {
        WrapperPlayClientUpdateSign updateSign = new WrapperPlayClientUpdateSign(event);
        UUID playerUUID = event.getUser().getUUID();

        ProbeSession session = activeSessions.get(playerUUID);
        if (session == null || session.handled.get()) {
            return;
        }

        // Verify this UPDATE_SIGN is for the probe sign, not a legitimate sign edit
        com.github.retrooper.packetevents.util.Vector3i signPos = updateSign.getBlockPosition();
        Location probeLoc = session.signLocation();
        if (signPos.x != probeLoc.getBlockX()
                || signPos.y != probeLoc.getBlockY()
                || signPos.z != probeLoc.getBlockZ()) {
            return;
        }

        // Atomically claim the session to avoid race with timeout handler
        if (!session.handled.compareAndSet(false, true)) {
            return;
        }
        if (!activeSessions.remove(playerUUID, session)) {
            return;
        }

        event.setCancelled(true);

        String[] lines = updateSign.getTextLines();

        if (Config.DEBUG.toBool()) {
            Player player = Bukkit.getPlayer(playerUUID);
            String playerName = player != null ? player.getName() : playerUUID.toString();
            Logs.logInfo("HackedServer | Sign probe response from " + playerName
                    + ": [\"" + String.join("\", \"", lines) + "\"]");
        }

        Bukkit.getScheduler().runTask(HackedServerPlugin.get(), () -> {
            if (!session.timedOut) {
                restoreBlock(session);
            }

            Player onlinePlayer = Bukkit.getPlayer(playerUUID);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }

            HackedPlayer hackedPlayer = HackedServer.getPlayer(playerUUID);
            if (hackedPlayer == null) {
                return;
            }

            for (int i = 0; i < session.lineChecks().size() && i < lines.length; i++) {
                TranslationCheck check = session.lineChecks().get(i);
                String response = lines[i] != null ? lines[i].trim() : "";
                String expected = check.getExpectedVanillaResponse();

                if (Config.DEBUG.toBool()) {
                    Logs.logInfo("HackedServer | Probe line " + i + " (" + check.getName()
                            + "): response=\"" + response + "\", expected=\"" + expected + "\"");
                }

                if (!response.isEmpty() && !response.equals(expected)) {
                    if (Config.DEBUG.toBool()) {
                        Logs.logInfo("HackedServer | DETECTED: " + check.getName()
                                + " via sign translation probe");
                    }
                    String probeCheckId = "probe_" + check.getId();
                    if (!hackedPlayer.hasGenericCheck(probeCheckId)) {
                        hackedPlayer.addGenericCheck(probeCheckId);
                        PayloadProcessor.runActions(onlinePlayer, check.getName(), check.getActions());
                    }
                }
            }

            scheduleRemainingChecks(onlinePlayer, session);
        });
    }

    private void restoreBlock(ProbeSession session) {
        if (!session.restored.compareAndSet(false, true)) {
            return;
        }
        try {
            Block block = session.signLocation().getBlock();
            block.setBlockData(session.originalBlockData(), false);
            // Restore tile entity data (chest contents, sign text, etc.)
            if (session.originalBlockState() != null) {
                session.originalBlockState().update(true, false);
            }
        } catch (Exception e) {
            Logs.logWarning("Failed to restore block after probe: " + e.getMessage());
        }
    }

    private void scheduleRemainingChecks(Player player, ProbeSession session) {
        if (session.remainingChecks().isEmpty() || !player.isOnline()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), () -> {
            if (player.isOnline()) {
                startProbe(player, session.remainingChecks());
            }
        }, 5L);
    }

}
