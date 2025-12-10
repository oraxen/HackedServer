package org.hackedserver.spigot.listeners;

import org.bukkit.Bukkit;
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
import org.hackedserver.spigot.HackedHolder;
import org.hackedserver.spigot.HackedServerPlugin;

public class HackedPlayerListeners implements Listener {

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        HackedServer.registerPlayer(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        HackedPlayer hackedPlayer = HackedServer.getPlayer(event.getPlayer().getUniqueId());
        if (hackedPlayer.hasPendingActions()) {
            // Execute pending actions on the next tick to ensure player is fully initialized
            Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), 
                    hackedPlayer::executePendingActions, 1L);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClickInv(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof HackedHolder) event.setCancelled(true);
    }
}
