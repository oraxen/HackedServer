package org.hackedserver.spigot.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;

public class HackedPlayerListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        HackedServer.registerPlayer(event.getUniqueId(), new HackedPlayer(event.getUniqueId()));
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
    }

}
