package org.hackedserver.bungee;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.bungee.factory.BungeePacketEventsBuilder;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.hackedserver.bungee.commands.CommandsManager;
import org.hackedserver.bungee.listeners.CustomPayloadListener;
import org.hackedserver.bungee.listeners.HackedPlayerListeners;
import org.hackedserver.bungee.logs.Logs;
import org.hackedserver.core.bedrock.BedrockDetector;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.lunar.LunarApolloHandshakeParser;
import org.hackedserver.core.probing.PacketSignProber;

import java.util.List;
import java.util.UUID;

public class HackedServerPlugin extends Plugin {

    private BungeeAudiences audiences;
    private static HackedServerPlugin instance;
    private PacketSignProber signProber;

    @Override
    public void onEnable() {
        instance = this;
        audiences = BungeeAudiences.create(this);
        Logs.onEnable(getLogger(), audiences);
        ConfigsManager.init(getDataFolder());
        BedrockDetector.initialize(getLogger());

        // Initialize PacketEvents for sign translation probing
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
        } catch (ClassNotFoundException e) {
            getLogger().severe("PacketEvents is required but not installed! HackedServer will not function.");
            getLogger().severe("Please install PacketEvents on your BungeeCord proxy.");
            return;
        }

        try {
            PacketEvents.setAPI(BungeePacketEventsBuilder.build(this));
            PacketEvents.getAPI().load();
        } catch (Throwable e) {
            getLogger().severe("Failed to initialize PacketEvents. HackedServer will not function: " + e.getMessage());
            return;
        }

        PluginManager pluginManager = this.getProxy().getPluginManager();
        pluginManager.registerListener(this, new HackedPlayerListeners(this::getSignProber));
        pluginManager.registerListener(this, new CustomPayloadListener());
        pluginManager.registerCommand(this, new CommandsManager(this.getProxy(), getDataFolder()));
        this.getProxy().registerChannel(LunarApolloHandshakeParser.CHANNEL);

        try {
            // Create sign prober with BungeeCord action executor
            signProber = new PacketSignProber((uuid, playerName, checkName, actions) -> {
                executeProbeActions(uuid, playerName, checkName, actions);
            });
            signProber.register();
            PacketEvents.getAPI().init();
        } catch (Throwable e) {
            getLogger().severe("Failed to register PacketEvents listeners. HackedServer will not function: " + e.getMessage());
            if (signProber != null) {
                signProber.unregister();
                signProber = null;
            }
        }
    }

    @Override
    public void onDisable() {
        if (signProber != null) {
            signProber.unregister();
        }
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
        this.getProxy().unregisterChannel(LunarApolloHandshakeParser.CHANNEL);
    }

    public static HackedServerPlugin get() {
        return instance;
    }

    public PacketSignProber getSignProber() {
        return signProber;
    }

    public BungeeAudiences getAudiences() {
        return audiences;
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
                for (ProxiedPlayer admin : ProxyServer.getInstance().getPlayers()) {
                    if (admin.hasPermission("hackedserver.alert")) {
                        audiences.player(admin).sendMessage(action.getAlert(templates));
                    }
                }
            }

            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
            if (player == null || !player.isConnected()) {
                continue;
            }
            if (player.hasPermission("hackedserver.bypass")) {
                continue;
            }

            for (String command : action.getConsoleCommands()) {
                ProxyServer.getInstance().getPluginManager().dispatchCommand(
                        ProxyServer.getInstance().getConsole(),
                        command.replace("<player>", playerName)
                                .replace("<name>", checkName));
            }
            for (String command : action.getPlayerCommands()) {
                ProxyServer.getInstance().getPluginManager().dispatchCommand(
                        player,
                        command.replace("<player>", playerName)
                                .replace("<name>", checkName));
            }
        }
    }
}
