package org.hackedserver.spigot.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.spigot.HackedHolder;
import org.jetbrains.annotations.NotNull;

public class HackedPlayerListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        HackedServer.registerPlayer(event.getUniqueId());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClickInv(InventoryClickEvent event) {
        HackedPlayer player = HackedServer.getPlayer(event.getWhoClicked().getUniqueId());
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof HackedHolder) event.setCancelled(true);

    }
}
