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
import org.hackedserver.core.config.GenericCheck;

import java.nio.charset.StandardCharsets;

public class CustomPayloadListener implements Listener {

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (event.getSender() instanceof ProxiedPlayer player) {
            String channel = event.getTag();
            HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
            String message = new String(event.getData(), StandardCharsets.UTF_8);
            for (GenericCheck check : HackedServer.getChecks())
                if (check.pass(channel, message)) {
                    hackedPlayer.addGenericCheck(check);
                    for (Action action : check.getActions())
                        performActions(action, player, Placeholder.unparsed("player",
                                player.getName()), Placeholder.parsed("name", check.getName()));
                }
        }
    }

    private void performActions(Action action, ProxiedPlayer player, TagResolver.Single... templates) {
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (ProxiedPlayer admin : ProxyServer.getInstance().getPlayers())
                if (admin.hasPermission("hackedserver.alert"))
                    HackedServerPlugin.get().getAudiences().player(player)
                            .sendMessage(action.getAlert(templates));
        }
        for (String command : action.getConsoleCommands())
            ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(),
                    command.replace("<player>",
                            player.getName()));
        for (String command : action.getPlayerCommands())
            ProxyServer.getInstance().getPluginManager().dispatchCommand(player,
                    command.replace("<player>",
                            player.getName()));
    }

}
