package org.hackedserver.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import org.hackedserver.core.HackedServer;

public class HackedPlayerListeners {

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        HackedServer.registerPlayer(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
    }

}
