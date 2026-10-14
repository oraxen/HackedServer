package org.hackedserver.spigot.probing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.hackedserver.spigot.listeners.PayloadProcessor;
import org.hackedserver.spigot.utils.Schedulers;
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
 * at the packet level (UPDATE_SIGN) when PacketEvents is available, or through
 * {@link SignChangeEvent} otherwise (e.g. ProtocolLib-only servers).
 * <p>
 * When the client opens the sign editor, translatable text components are
 * resolved to plain text. If a mod's translation key resolves to something
 * other than the raw key, that mod is installed.
 * <p>
 * Requires Paper (for the Adventure sign API). This class must not reference
 * PacketEvents types: it is registered as a Bukkit listener even when PacketEvents
 * is missing or broken (see {@code PacketEventsIntegration#registerSignListener}).
 */
public class SignTranslationProber implements Listener {

    private final Map<UUID, ProbeSession> activeSessions = new ConcurrentHashMap<>();

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

        boolean isAt(int x, int y, int z) {
            return signLocation.getBlockX() == x && signLocation.getBlockY() == y && signLocation.getBlockZ() == z;
        }
    }

    public void unregister() {
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

        Schedulers.runForEntity(player, () -> {
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
        if (session == null) {
            return;
        }
        Location signLoc = event.getBlock().getLocation();
        if (!session.isAt(signLoc.getBlockX(), signLoc.getBlockY(), signLoc.getBlockZ())) {
            return;
        }
        event.setCancelled(true);

        // Only reached when the UPDATE_SIGN packet was not consumed at the packet level
        // (no PacketEvents): read the client's resolved lines from the event instead.
        String[] lines = new String[4];
        for (int i = 0; i < lines.length; i++) {
            Component line = event.line(i);
            lines[i] = line != null ? PlainTextComponentSerializer.plainText().serialize(line) : "";
        }
        handleResponse(player.getUniqueId(), signLoc.getBlockX(), signLoc.getBlockY(), signLoc.getBlockZ(), lines);
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
            ProbeSession probe = new ProbeSession(signLoc, originalData, originalState, lineChecks, remainingChecks);
            activeSessions.put(player.getUniqueId(), probe);

            // Open sign editor after a short delay to let the block update propagate
            Schedulers.runForEntity(player, () -> {
                if (!player.isOnline()) {
                    if (activeSessions.remove(player.getUniqueId(), probe)) restoreBlock(probe);
                    return;
                }

                BlockState currentState = block.getState();
                if (!(currentState instanceof Sign currentSign)) {
                    Logs.logWarning("HackedServer | Sign disappeared before editor opened for " + player.getName());
                    if (activeSessions.remove(player.getUniqueId(), probe)) restoreBlock(probe);
                    return;
                }

                player.openSign(currentSign, Side.FRONT);

                if (Config.DEBUG.toBool()) {
                    Logs.logInfo("HackedServer | Opened sign editor for " + player.getName());
                }

                // Close the editor right away: the client answers with the lines it rendered when the
                // screen closes, so the probe neither waits for nor depends on the player
                Schedulers.runForEntity(player, () -> {
                    if (activeSessions.get(player.getUniqueId()) == probe && !probe.handled.get()) {
                        player.closeInventory();
                    }
                }, 2L);

                // Timeout: restore block but keep session for late packet arrivals
                // Compare by identity: a later probe batch may have replaced this session
                Schedulers.runForEntity(player, () -> {
                    ProbeSession s = activeSessions.get(player.getUniqueId());
                    if (s == probe && !s.handled.get()) {
                        s.timedOut = true;
                        if (Config.DEBUG.toBool()) {
                            Logs.logInfo("HackedServer | Sign probe timed out for " + player.getName() + " (waiting for late packet)");
                        }
                        restoreBlock(s);
                    }
                }, 400L); // 20 second timeout

                // Final cleanup: remove session after grace period for late packets
                Schedulers.runForEntity(player, () -> {
                    if (activeSessions.remove(player.getUniqueId(), probe) && !probe.handled.get()) {
                        if (Config.DEBUG.toBool()) {
                            Logs.logInfo("HackedServer | Sign probe final cleanup for " + player.getName());
                        }
                        restoreBlock(probe);
                        scheduleRemainingChecks(player, probe);
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

    /**
     * Handles the client's reply to a probe sign. Safe to call from a netty thread.
     *
     * @return true if the reply belongs to an active probe and must not reach the server
     */
    public boolean handleResponse(UUID playerUUID, int x, int y, int z, String[] lines) {
        ProbeSession session = activeSessions.get(playerUUID);
        if (session == null || session.handled.get()) {
            return false;
        }

        // Verify this UPDATE_SIGN is for the probe sign, not a legitimate sign edit
        if (!session.isAt(x, y, z)) {
            return false;
        }

        // Atomically claim the session to avoid race with timeout handler
        if (!session.handled.compareAndSet(false, true)) {
            return false;
        }
        if (!activeSessions.remove(playerUUID, session)) {
            return false;
        }

        Player player = Bukkit.getPlayer(playerUUID);

        if (Config.DEBUG.toBool()) {
            String playerName = player != null ? player.getName() : playerUUID.toString();
            Logs.logInfo("HackedServer | Sign probe response from " + playerName
                    + ": [\"" + String.join("\", \"", lines) + "\"]");
        }

        if (player == null) {
            restoreBlock(session);
            return true;
        }

        Schedulers.runForEntity(player, () -> {
            if (!session.timedOut) {
                restoreBlock(session);
            }

            Player onlinePlayer = Bukkit.getPlayer(playerUUID);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }

            HackedPlayer hackedPlayer = HackedServer.getPlayerIfPresent(playerUUID);
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
                    String probeCheckId = HackedServer.PROBE_CHECK_PREFIX + check.getId();
                    if (!hackedPlayer.hasGenericCheck(probeCheckId)) {
                        hackedPlayer.addGenericCheck(probeCheckId);
                        PayloadProcessor.runActions(onlinePlayer, check.getName(), check.getActions());
                    }
                }
            }

            scheduleRemainingChecks(onlinePlayer, session);
        }, 0L);
        return true;
    }

    private void restoreBlock(ProbeSession session) {
        if (!session.restored.compareAndSet(false, true)) {
            return;
        }
        try {
            Schedulers.runAtLocationNow(session.signLocation(), () -> restoreBlockNow(session));
        } catch (Exception e) {
            // Scheduling fails while the plugin is disabling on Folia
            Logs.logWarning("Failed to restore block after probe: " + e.getMessage());
        }
    }

    private void restoreBlockNow(ProbeSession session) {
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
        Schedulers.runForEntity(player, () -> {
            if (player.isOnline()) {
                startProbe(player, session.remainingChecks());
            }
        }, 5L);
    }

}
