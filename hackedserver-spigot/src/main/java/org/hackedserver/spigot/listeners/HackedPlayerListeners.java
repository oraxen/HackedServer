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
import org.hackedserver.spigot.utils.BedrockDetector;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class HackedPlayerListeners implements Listener {

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        HackedServer.registerPlayer(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
        if (hackedPlayer.hasPendingActions()) {
            // Execute pending actions after a short delay to ensure player is fully initialized
            // Use at least 1 tick to ensure the join event completes
            // Each action will then apply its own configured delay before executing commands
            Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(),
                    hackedPlayer::executePendingActions, 1L);
        }

        Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), () -> {
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
        if (inv.getHolder() instanceof HackedHolder) event.setCancelled(true);
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

        String source = BedrockDetector.detectSource(player);
        if (source == null) {
            return;
        }

        hackedPlayer.setBedrockDetected(true);
        String label = BedrockConfig.getLabel();
        PayloadProcessor.runActions(player, label, BedrockConfig.getBedrockActions(),
                Placeholder.unparsed("source", source));
    }
}
