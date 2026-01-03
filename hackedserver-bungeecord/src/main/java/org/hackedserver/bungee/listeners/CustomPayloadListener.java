package org.hackedserver.bungee.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.hackedserver.bungee.HackedServerPlugin;
import org.hackedserver.bungee.logs.Logs;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.Config;
import org.hackedserver.core.config.GenericCheck;
import org.hackedserver.core.config.Message;
import org.hackedserver.core.forge.ForgeActionTrigger;
import org.hackedserver.core.forge.ForgeChannelParser;
import org.hackedserver.core.forge.ForgeClientType;
import org.hackedserver.core.forge.ForgeConfig;
import org.hackedserver.core.forge.ForgeHandshakeProcessor;
import org.hackedserver.core.forge.ForgeHandshakeResult;
import org.hackedserver.core.forge.ForgeModInfo;
import org.hackedserver.core.lunar.LunarActionTrigger;
import org.hackedserver.core.lunar.LunarApolloHandshakeParser;
import org.hackedserver.core.lunar.LunarHandshakeProcessor;
import org.hackedserver.core.lunar.LunarHandshakeResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class CustomPayloadListener implements Listener {

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (event.getSender() instanceof ProxiedPlayer player) {
            String channel = event.getTag();
            HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
            String message = new String(event.getData(), StandardCharsets.UTF_8);

            if (Config.DEBUG.toBool()) {
                Logs.logComponent(Message.DEBUG_MESSAGE.toComponent(
                        Placeholder.unparsed("player", player.getName()),
                        Placeholder.unparsed("channel", channel),
                        Placeholder.unparsed("message", message)));
            }

            for (GenericCheck check : HackedServer.getChecks())
                if (check.pass(hackedPlayer, channel, message)) {
                    hackedPlayer.addGenericCheck(check);
                    for (Action action : check.getActions())
                        performActions(action, player, check.getName(),
                                Placeholder.unparsed("player", player.getName()),
                                Placeholder.parsed("name", check.getName()));
                }

            if (LunarApolloHandshakeParser.CHANNEL.equalsIgnoreCase(channel)) {
                handleLunarApollo(player, event.getData());
            }

            // Forge/NeoForge detection
            if (ForgeConfig.isEnabled()) {
                processForgePacket(player, hackedPlayer, channel, message);
            }
        }
    }

    private void processForgePacket(ProxiedPlayer player, HackedPlayer hackedPlayer, String channel, String message) {
        // Detect client type from minecraft:brand
        if (ForgeChannelParser.BRAND_CHANNEL.equalsIgnoreCase(channel)) {
            ForgeClientType clientType = ForgeChannelParser.parseClientType(message);
            if (clientType != null && hackedPlayer.getForgeClientType() == null) {
                hackedPlayer.setForgeClientType(clientType);
                ForgeHandshakeResult result = ForgeHandshakeProcessor.processClientType(hackedPlayer, clientType);
                if (result.hasTriggers()) {
                    runForgeActions(result.getTriggers(), player.getUniqueId(), player.getName());
                }
            }
        }

        // Detect mods from minecraft:register
        if (ForgeChannelParser.REGISTER_CHANNEL.equalsIgnoreCase(channel)) {
            List<ForgeModInfo> mods = ForgeChannelParser.parseRegisteredChannels(message);
            if (!mods.isEmpty()) {
                ForgeHandshakeResult result = ForgeHandshakeProcessor.processMods(hackedPlayer, mods);
                if (result.hasTriggers()) {
                    runForgeActions(result.getTriggers(), player.getUniqueId(), player.getName());
                }
            }
        }
    }

    private void runForgeActions(List<ForgeActionTrigger> triggers, UUID uuid, String playerName) {
        for (ForgeActionTrigger trigger : triggers) {
            runActions(trigger.getActions(), uuid, playerName, trigger.getName());
        }
    }

    private void handleLunarApollo(ProxiedPlayer player, byte[] data) {
        LunarApolloHandshakeParser.parseMods(data).ifPresent(mods -> {
            HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
            LunarHandshakeResult result = LunarHandshakeProcessor.process(hackedPlayer, mods);
            if (!result.hasTriggers()) {
                return;
            }
            for (LunarActionTrigger trigger : result.getTriggers()) {
                runActions(trigger.getActions(), player.getUniqueId(), player.getName(), trigger.getName());
            }
        });
    }

    private void runActions(List<Action> actions, UUID uuid, String playerName, String checkName) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (Action action : actions) {
            performActions(action, uuid, playerName, checkName);
        }
    }

    private void performActions(Action action, ProxiedPlayer player, String checkName, TagResolver.Single... templates) {
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (ProxiedPlayer admin : ProxyServer.getInstance().getPlayers())
                if (admin.hasPermission("hackedserver.alert"))
                    HackedServerPlugin.get().getAudiences().player(admin)
                            .sendMessage(action.getAlert(templates));
        }
        if (player.hasPermission("hackedserver.bypass"))
            return;

        for (String command : action.getConsoleCommands())
            ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(),
                    command.replace("<player>",
                            player.getName()).replace("<name>", checkName));
        for (String command : action.getPlayerCommands())
            ProxyServer.getInstance().getPluginManager().dispatchCommand(player,
                    command.replace("<player>",
                            player.getName()).replace("<name>", checkName));
    }

    private void performActions(Action action, UUID uuid, String playerName, String checkName) {
        TagResolver.Single[] templates = new TagResolver.Single[]{
                Placeholder.unparsed("player", playerName),
                Placeholder.parsed("name", checkName)
        };
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (ProxiedPlayer admin : ProxyServer.getInstance().getPlayers()) {
                if (admin.hasPermission("hackedserver.alert")) {
                    HackedServerPlugin.get().getAudiences().player(admin)
                            .sendMessage(action.getAlert(templates));
                }
            }
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player == null || !player.isConnected()) {
            HackedServer.getPlayer(uuid).queuePendingAction(() -> executeCommands(action, uuid, checkName));
            return;
        }

        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        executeCommands(action, uuid, checkName);
    }

    private void executeCommands(Action action, UUID uuid, String checkName) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player == null) {
            return;
        }
        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        for (String command : action.getConsoleCommands()) {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    ProxyServer.getInstance().getConsole(),
                    command.replace("<player>", player.getName())
                            .replace("<name>", checkName));
        }
        for (String command : action.getPlayerCommands()) {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    player,
                    command.replace("<player>", player.getName())
                            .replace("<name>", checkName));
        }
    }

}
