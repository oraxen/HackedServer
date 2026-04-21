package org.hackedserver.bungee.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.hackedserver.bungee.HackedServerPlugin;
import org.hackedserver.bungee.logs.Logs;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.bedrock.BedrockDetector;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.BedrockConfig;
import org.hackedserver.core.probing.PacketSignProber;

import org.hackedserver.core.utils.JoinWebhook;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class HackedPlayerListeners implements Listener {

    private static final long JOIN_WEBHOOK_DELAY_SECONDS = 1L;
    private final Supplier<PacketSignProber> signProberSupplier;

    public HackedPlayerListeners(Supplier<PacketSignProber> signProberSupplier) {
        this.signProberSupplier = signProberSupplier;
    }

    @EventHandler
    public void onPlayerJoin(LoginEvent event) {
        HackedServer.registerPlayer(event.getConnection().getUniqueId());
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());

        ProxyServer.getInstance().getScheduler().schedule(HackedServerPlugin.get(), () -> {
            JoinWebhook.send(player.getName(), player.getUniqueId());
        }, JOIN_WEBHOOK_DELAY_SECONDS, TimeUnit.SECONDS);

        if (hackedPlayer.hasPendingActions()) {
            hackedPlayer.executePendingActions();
        }
        handleBedrockDetection(player, hackedPlayer);

        // Start sign translation probe if enabled
        PacketSignProber signProber = signProberSupplier.get();
        if (signProber != null && !player.hasPermission("hackedserver.bypass")) {
            try {
                User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
                if (user != null) {
                    signProber.startProbe(user, player.getName());
                }
            } catch (RuntimeException e) {
                HackedServerPlugin.get().getLogger().warning("Failed to start sign translation probe for "
                        + player.getName() + ": " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent event) {
        PacketSignProber signProber = signProberSupplier.get();
        if (signProber != null) {
            signProber.onPlayerDisconnect(event.getPlayer().getUniqueId());
        }
        HackedServer.removePlayer(event.getPlayer().getUniqueId());
    }

    private void handleBedrockDetection(ProxiedPlayer player, HackedPlayer hackedPlayer) {
        if (!BedrockConfig.isEnabled()) {
            return;
        }
        if (player == null || !player.isConnected()) {
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

    private void performBedrockAction(Action action, ProxiedPlayer player, String checkName, String source) {
        TagResolver.Single[] templates = new TagResolver.Single[]{
                Placeholder.unparsed("player", player.getName()),
                Placeholder.parsed("name", checkName),
                Placeholder.unparsed("source", source)
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

        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        for (String command : action.getConsoleCommands()) {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    ProxyServer.getInstance().getConsole(),
                    command.replace("<player>", player.getName())
                            .replace("<name>", checkName)
                            .replace("<source>", source));
        }
        for (String command : action.getPlayerCommands()) {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    player,
                    command.replace("<player>", player.getName())
                            .replace("<name>", checkName)
                            .replace("<source>", source));
        }
    }
}
