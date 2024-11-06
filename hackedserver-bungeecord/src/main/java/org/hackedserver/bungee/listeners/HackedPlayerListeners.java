package org.hackedserver.bungee.listeners;

import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.hackedserver.core.HackedServer;

public class HackedPlayerListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(LoginEvent event) {
        HackedServer.registerPlayer(event.getConnection().getUniqueId());
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent event) {
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
    }

}
