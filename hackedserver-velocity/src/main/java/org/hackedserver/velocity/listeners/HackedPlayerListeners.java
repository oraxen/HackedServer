package org.hackedserver.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.bedrock.BedrockDetector;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.BedrockConfig;
import org.hackedserver.velocity.logs.Logs;

public class HackedPlayerListeners {

    private final ProxyServer server;

    public HackedPlayerListeners(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        HackedServer.registerPlayer(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
        if (hackedPlayer.hasPendingActions()) {
            hackedPlayer.executePendingActions();
        }
        handleBedrockDetection(player, hackedPlayer);
    }

    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
    }

    private void handleBedrockDetection(Player player, HackedPlayer hackedPlayer) {
        if (!BedrockConfig.isEnabled()) {
            return;
        }
        if (player == null || !player.isActive()) {
            return;
        }
        if (hackedPlayer.isBedrockDetected()) {
            return;
        }

        String source = BedrockDetector.detectSource(player.getUniqueId());
        if (source == null) {
            return;
        }

        hackedPlayer.setBedrockDetected(true);
        String label = BedrockConfig.getLabel();
        for (Action action : BedrockConfig.getBedrockActions()) {
            performBedrockAction(action, player, label, source);
        }
    }

    private void performBedrockAction(Action action, Player player, String checkName, String source) {
        TagResolver.Single[] templates = new TagResolver.Single[]{
                Placeholder.unparsed("player", player.getUsername()),
                Placeholder.parsed("name", checkName),
                Placeholder.unparsed("source", source)
        };

        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : server.getAllPlayers()) {
                if (admin.hasPermission("hackedserver.alert")) {
                    admin.sendMessage(action.getAlert(templates));
                }
            }
        }

        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        for (String command : action.getConsoleCommands()) {
            server.getCommandManager().executeAsync(server.getConsoleCommandSource(),
                    command.replace("<player>", player.getUsername())
                            .replace("<name>", checkName)
                            .replace("<source>", source));
        }
        for (String command : action.getPlayerCommands()) {
            server.getCommandManager().executeAsync(player,
                    command.replace("<player>", player.getUsername())
                            .replace("<name>", checkName)
                            .replace("<source>", source));
        }
    }
}
