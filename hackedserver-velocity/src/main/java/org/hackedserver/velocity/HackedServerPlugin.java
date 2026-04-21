package org.hackedserver.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;

import org.hackedserver.core.bedrock.BedrockDetector;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.probing.PacketSignProber;
import org.hackedserver.velocity.commands.HackedCommands;
import org.hackedserver.velocity.listeners.CustomPayloadListener;
import org.hackedserver.velocity.listeners.HackedPlayerListeners;
import org.hackedserver.velocity.logs.Logs;

import com.google.inject.Inject;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

@Plugin(
        id = "hackedserver",
        name = "HackedServer",
        version = "3.17.2"
)
public class HackedServerPlugin {

    private final ProxyServer server;
    private final HackedCommands commands;
    private final File folder;
    private PacketSignProber signProber;

    @Inject
    public HackedServerPlugin(
            ProxyServer server,
            Logger logger,
            PluginContainer pluginContainer,
            @DataDirectory Path dataDirectory
    ) {
        this.server = server;
        this.folder = dataDirectory.toFile();
        Logs.onEnable(logger, server);
        ConfigsManager.init(folder);
        // Initialize bedrock detection (null logger since Velocity uses SLF4J)
        BedrockDetector.initialize(null);
        if (BedrockDetector.isAvailable()) {
            logger.info("Geyser/Floodgate API detected - bedrock detection enabled");
        }
        commands = new HackedCommands(folder, server.getCommandManager(), server);
        PacketEvents.setAPI(VelocityPacketEventsBuilder.build(server, pluginContainer, logger, dataDirectory));
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Create sign prober with Velocity action executor
        signProber = new PacketSignProber((uuid, playerName, checkName, actions) -> {
            executeProbeActions(uuid, playerName, checkName, actions);
        });

        server.getEventManager().register(this, new HackedPlayerListeners(server, this, signProber));
        PacketEvents.getAPI().getEventManager().registerListener(
                new CustomPayloadListener(server), PacketListenerPriority.NORMAL);
        signProber.register();
        PacketEvents.getAPI().init();
        commands.create();

        ChannelRegistrar channelRegistrar = server.getChannelRegistrar();
        channelRegistrar.register(MinecraftChannelIdentifier.create("lunar", "apollo"));
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        // Unregister all event listeners for this plugin
        server.getEventManager().unregisterListeners(this);

        // Unregister sign prober before terminating PacketEvents
        if (signProber != null) {
            signProber.unregister();
        }

        // Terminate and reinit PacketEvents
        PacketEvents.getAPI().terminate();
        PacketEvents.getAPI().init();

        // Reload configs
        ConfigsManager.reload(folder);

        // Recreate commands
        commands.create();

        // Re-create sign prober with fresh config
        signProber = new PacketSignProber((uuid, playerName, checkName, actions) -> {
            executeProbeActions(uuid, playerName, checkName, actions);
        });

        // Re-register event listeners
        server.getEventManager().register(this, new HackedPlayerListeners(server, this, signProber));
        PacketEvents.getAPI().getEventManager().registerListener(
                new CustomPayloadListener(server), PacketListenerPriority.NORMAL);
        signProber.register();
    }

    private void executeProbeActions(UUID uuid, String playerName, String checkName, List<Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        TagResolver.Single[] templates = new TagResolver.Single[]{
                Placeholder.unparsed("player", playerName),
                Placeholder.parsed("name", checkName)
        };

        for (Action action : actions) {
            if (action.hasAlert()) {
                Logs.logComponent(action.getAlert(templates));
                for (com.velocitypowered.api.proxy.Player admin : server.getAllPlayers()) {
                    if (admin.hasPermission("hackedserver.alert")) {
                        admin.sendMessage(action.getAlert(templates));
                    }
                }
            }

            com.velocitypowered.api.proxy.Player player = server.getPlayer(uuid).orElse(null);
            if (player == null || !player.isActive()) {
                continue;
            }
            if (player.hasPermission("hackedserver.bypass")) {
                continue;
            }

            for (String command : action.getConsoleCommands()) {
                server.getCommandManager().executeAsync(server.getConsoleCommandSource(),
                        command.replace("<player>", playerName)
                                .replace("<name>", checkName));
            }
            for (String command : action.getPlayerCommands()) {
                server.getCommandManager().executeAsync(player,
                        command.replace("<player>", playerName)
                                .replace("<name>", checkName));
            }
        }
    }
}
