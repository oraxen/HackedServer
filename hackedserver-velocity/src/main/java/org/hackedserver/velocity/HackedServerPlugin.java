package org.hackedserver.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.velocity.commands.HackedCommands;
import org.hackedserver.velocity.listeners.CustomPayloadListener;
import org.hackedserver.velocity.listeners.HackedPlayerListeners;
import org.hackedserver.velocity.logs.Logs;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "hackedserver", name = "HackedServer", version = "${projectVersion}",
        url = "https://hackedserver.org/", description = "Detect forge, fabric, alert your staff and punish cheaters automatically", authors = {"Th0rgal"})
public class HackedServerPlugin {

    private final ProxyServer server;
    private final HackedCommands commands;

    @Inject
    public HackedServerPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        Logs.onEnable(logger, server);
        ConfigsManager.init(logger, dataDirectory.toFile());
        commands = new HackedCommands(logger, dataDirectory.toFile(), server.getCommandManager());
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new HackedPlayerListeners());
        server.getEventManager().register(this, new CustomPayloadListener(server));
        commands.create();
    }

}
