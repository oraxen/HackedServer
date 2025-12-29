package org.hackedserver.velocity.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.Config;
import org.hackedserver.core.config.GenericCheck;
import org.hackedserver.core.config.Message;
import org.hackedserver.core.lunar.LunarActionTrigger;
import org.hackedserver.core.lunar.LunarApolloHandshakeParser;
import org.hackedserver.core.lunar.LunarHandshakeProcessor;
import org.hackedserver.core.lunar.LunarHandshakeResult;
import org.hackedserver.velocity.logs.Logs;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientPluginResponse;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

public class CustomPayloadListener implements PacketListener {

    private final ProxyServer server;

    public CustomPayloadListener(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE ||
                event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE ||
                event.getPacketType() == PacketType.Login.Client.LOGIN_PLUGIN_RESPONSE) {

            User user = event.getUser();
            Player player = server.getPlayer(user.getUUID()).orElse(null);
            if (player == null)
                return;

            String channel;
            byte[] data;

            if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
                WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
                channel = packet.getChannelName();
                data = packet.getData();
            } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
                WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
                channel = packet.getChannelName();
                data = packet.getData();
            } else {
                WrapperLoginClientPluginResponse packet = new WrapperLoginClientPluginResponse(event);
                channel = "login:response";
                data = packet.getData();
            }

            String message = new String(data, StandardCharsets.UTF_8);

            if (Config.DEBUG.toBool()) {
                Logs.logComponent(Message.DEBUG_MESSAGE.toComponent(
                        Placeholder.unparsed("player", player.getUsername()),
                        Placeholder.unparsed("channel", channel),
                        Placeholder.unparsed("message", message)));
            }

            HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
            for (GenericCheck check : HackedServer.getChecks()) {
                if (check.pass(hackedPlayer, channel, message)) {
                    hackedPlayer.addGenericCheck(check);
                    for (Action action : check.getActions()) {
                        performActions(action, player,
                                Placeholder.unparsed("player", player.getUsername()),
                                Placeholder.parsed("name", check.getName()));
                    }
                }
            }

            if (LunarApolloHandshakeParser.CHANNEL.equalsIgnoreCase(channel)) {
                handleLunarApollo(player, data);
            }
        }
    }

    private void handleLunarApollo(Player player, byte[] data) {
        LunarApolloHandshakeParser.parseMods(data).ifPresent(mods -> {
            HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
            LunarHandshakeResult result = LunarHandshakeProcessor.process(hackedPlayer, mods);
            if (!result.hasTriggers()) {
                return;
            }
            for (LunarActionTrigger trigger : result.getTriggers()) {
                runActions(trigger.getActions(), player.getUniqueId(), player.getUsername(), trigger.getName());
            }
        });
    }

    private void runActions(List<Action> actions, UUID uuid, String playerName, String checkName) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (Action action : actions) {
            performActions(action, uuid,
                    Placeholder.unparsed("player", playerName),
                    Placeholder.parsed("name", checkName));
        }
    }

    private void performActions(Action action, Player player, TagResolver.Single... templates) {
        // Logs.logWarning("perform actions triggered for " + player.getUsername());
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : server.getAllPlayers()) {
                // Logs.logWarning("user name:" + admin.getUsername() + " permission:"
                // + admin.hasPermission("hackedserver.alert"));
                if (admin.hasPermission("hackedserver.alert")) {
                    // Logs.logWarning("user permission check passed for " + admin.getUsername());
                    admin.sendMessage(action.getAlert(templates));
                }
            }
        }

        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        String checkName = java.util.Arrays.stream(templates)
                .filter(t -> t.key().equals("name"))
                .findFirst()
                .map(t -> t.tag().toString())
                .orElse("<name>");

        for (String command : action.getConsoleCommands()) {
            server.getCommandManager().executeAsync(server.getConsoleCommandSource(),
                    command.replace("<player>", player.getUsername())
                            .replace("<name>", checkName));
        }

        for (String command : action.getPlayerCommands()) {
            server.getCommandManager().executeAsync(player,
                    command.replace("<player>", player.getUsername())
                            .replace("<name>", checkName));
        }
    }

    private void performActions(Action action, UUID uuid, TagResolver.Single... templates) {
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : server.getAllPlayers()) {
                if (admin.hasPermission("hackedserver.alert")) {
                    admin.sendMessage(action.getAlert(templates));
                }
            }
        }

        Player player = server.getPlayer(uuid).orElse(null);
        if (player == null || !player.isActive()) {
            HackedServer.getPlayer(uuid).queuePendingAction(() -> executeCommands(action, uuid, checkNameFromTemplates(templates)));
            return;
        }

        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        executeCommands(action, uuid, checkNameFromTemplates(templates));
    }

    private void executeCommands(Action action, UUID uuid, String checkName) {
        Player player = server.getPlayer(uuid).orElse(null);
        if (player == null || !player.isActive()) {
            return;
        }
        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        for (String command : action.getConsoleCommands()) {
            server.getCommandManager().executeAsync(server.getConsoleCommandSource(),
                    command.replace("<player>", player.getUsername())
                            .replace("<name>", checkName));
        }

        for (String command : action.getPlayerCommands()) {
            server.getCommandManager().executeAsync(player,
                    command.replace("<player>", player.getUsername())
                            .replace("<name>", checkName));
        }
    }

    private String checkNameFromTemplates(TagResolver.Single... templates) {
        return java.util.Arrays.stream(templates)
                .filter(t -> t.key().equals("name"))
                .findFirst()
                .map(t -> t.tag().toString())
                .orElse("<name>");
    }
}
