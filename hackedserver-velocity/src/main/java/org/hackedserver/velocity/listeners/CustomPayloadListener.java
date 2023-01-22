package org.hackedserver.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.GenericCheck;
import org.hackedserver.velocity.logs.Logs;

import java.nio.charset.StandardCharsets;

public class CustomPayloadListener {

    private final ProxyServer server;

    public CustomPayloadListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPluginMessageReceived(PluginMessageEvent event) {

        if (event.getSource() instanceof Player player) {
            String channel = event.getIdentifier().getId();
            HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
            String message = new String(event.getData(), StandardCharsets.UTF_8);
            for (GenericCheck check : HackedServer.getChecks())
                if (check.pass(channel, message)) {
                    hackedPlayer.addGenericCheck(check);
                    for (Action action : check.getActions())
                        performActions(action, player, Placeholder.unparsed("player",
                                player.getUsername()), Placeholder.parsed("name", check.getName()));
                }
        }
    }

    private void performActions(Action action, Player player, TagResolver.Single... templates) {
        if (player.hasPermission("hackedserver.bypass")) return;
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : server.getAllPlayers())
                if (admin.hasPermission("hackedserver.alert"))
                    admin.sendMessage(action.getAlert(templates));
        }
        for (String command : action.getConsoleCommands())
            server.getCommandManager().executeAsync(server.getConsoleCommandSource(),
                    command.replace("<player>",
                            player.getUsername()));
        for (String command : action.getPlayerCommands())
            server.getCommandManager().executeAsync(player,
                    command.replace("<player>",
                            player.getUsername()));
    }

}
