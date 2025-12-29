package org.hackedserver.bungee.listeners;

import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;

public class HackedPlayerListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(LoginEvent event) {
        HackedServer.registerPlayer(event.getConnection().getUniqueId());
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        HackedPlayer hackedPlayer = HackedServer.getPlayer(event.getPlayer().getUniqueId());
        if (hackedPlayer.hasPendingActions()) {
            hackedPlayer.executePendingActions();
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent event) {
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
    }

}
