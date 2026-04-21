package org.hackedserver.spigot.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.BedrockConfig;
import org.hackedserver.spigot.HackedHolder;
import org.hackedserver.spigot.HackedServerPlugin;
import org.hackedserver.core.bedrock.BedrockDetector;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.hackedserver.spigot.utils.HackedInventoryView;

import org.hackedserver.core.utils.JoinWebhook;

import java.util.Map;

public class HackedPlayerListeners implements Listener {

    private static final long JOIN_WEBHOOK_DELAY_TICKS = 20L;

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        HackedServer.registerPlayer(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());

        // Capture player data on the main thread before scheduling the async webhook task.
        // Accessing Bukkit Player objects from async threads is unsafe and can cause errors.
        final String playerName = player.getName();
        final java.util.UUID playerUuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLaterAsynchronously(HackedServerPlugin.get(), () -> {
            JoinWebhook.send(playerName, playerUuid);
        }, JOIN_WEBHOOK_DELAY_TICKS);

        if (hackedPlayer.hasPendingActions()) {
            // Execute pending actions after a short delay to ensure player is fully initialized
            // Use at least 1 tick to ensure the join event completes
            // Each action will then apply its own configured delay before executing commands
            Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(),
                    hackedPlayer::executePendingActions, 1L);
        }

        Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), () -> {
            if (!player.isOnline()) {
                return;
            }
            for (PendingPayloads.PendingPayload payload : PendingPayloads.drainFor(player)) {
                PayloadProcessor.process(player, payload.getChannel(), payload.getMessage());
            }
            handleBedrockDetection(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
        PendingPayloads.clearFor(event.getPlayer());
    }

    @EventHandler
    public void onClickInv(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof HackedHolder holder)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        Player player = (Player) event.getWhoClicked();
        if (slot == 45 && holder.getPage() > 0) {
            HackedInventoryView.openInvPage(player, holder.getPage() - 1);
        } else if (slot == 53 && inv.getItem(53) != null) {
            HackedInventoryView.openInvPage(player, holder.getPage() + 1);
        }
    }

    private void handleBedrockDetection(Player player) {
        if (!BedrockConfig.isEnabled()) {
            return;
        }
        if (player == null || !player.isOnline()) {
            return;
        }
        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
        if (hackedPlayer.isBedrockDetected()) {
            return;
        }

        String source = BedrockDetector.detectSource(player.getUniqueId());
        if (source == null) {
            return;
        }

        hackedPlayer.setBedrockDetected(true);
        String label = BedrockConfig.getLabel();
        PayloadProcessor.runActions(player, label, BedrockConfig.getBedrockActions(),
                Map.of("source", source),
                Placeholder.unparsed("source", source));
    }
}
