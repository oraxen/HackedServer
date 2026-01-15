package org.hackedserver.spigot.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientPluginResponse;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
import org.hackedserver.spigot.HackedServerPlugin;
import org.hackedserver.spigot.utils.logs.Logs;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * PacketEvents-based custom payload listener for Spigot/Paper/Arclight servers.
 * This is an alternative to the ProtocolLib-based listener that works better
 * with hybrid server software like Arclight.
 */
public class PacketEventsPayloadListener extends PacketListenerAbstract {

    private final HackedServerPlugin plugin;

    public PacketEventsPayloadListener(HackedServerPlugin plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE ||
                event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE ||
                event.getPacketType() == PacketType.Login.Client.LOGIN_PLUGIN_RESPONSE) {

            User user = event.getUser();
            UUID playerUuid = user.getUUID();
            if (playerUuid == null) {
                return;
            }

            Player player = Bukkit.getPlayer(playerUuid);
            String playerName = user.getName() != null ? user.getName() : (player != null ? player.getName() : "Unknown");

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

            if (data == null) {
                data = new byte[0];
            }

            String message = new String(data, StandardCharsets.UTF_8);

            if (Config.DEBUG.toBool()) {
                Logs.logComponent(Message.DEBUG_MESSAGE.toComponent(
                        Placeholder.unparsed("player", playerName),
                        Placeholder.unparsed("channel", channel),
                        Placeholder.unparsed("message", message)));
            }

            // Generic checks
            HackedPlayer hackedPlayer = HackedServer.getPlayer(playerUuid);
            for (GenericCheck check : HackedServer.getChecks()) {
                if (check.pass(hackedPlayer, channel, message)) {
                    hackedPlayer.addGenericCheck(check);
                    for (Action action : check.getActions()) {
                        performActions(action, playerUuid, playerName, check.getName(),
                                Placeholder.unparsed("player", playerName),
                                Placeholder.parsed("name", check.getName()));
                    }
                }
            }

            // Forge/NeoForge detection
            if (ForgeConfig.isEnabled()) {
                processForgePacket(playerUuid, playerName, hackedPlayer, channel, message);
            }
        }
    }

    private void processForgePacket(UUID playerUuid, String playerName, HackedPlayer hackedPlayer, String channel, String message) {
        // Detect client type from minecraft:brand
        if (ForgeChannelParser.BRAND_CHANNEL.equalsIgnoreCase(channel)) {
            ForgeClientType clientType = ForgeChannelParser.parseClientType(message);
            if (clientType != null && hackedPlayer.getForgeClientType() == null) {
                hackedPlayer.setForgeClientType(clientType);
                ForgeHandshakeResult result = ForgeHandshakeProcessor.processClientType(hackedPlayer, clientType);
                if (result.hasTriggers()) {
                    runForgeActions(result.getTriggers(), playerUuid, playerName);
                }
            }
        }

        // Detect mods from minecraft:register
        if (ForgeChannelParser.REGISTER_CHANNEL.equalsIgnoreCase(channel)) {
            List<ForgeModInfo> mods = ForgeChannelParser.parseRegisteredChannels(message);
            if (!mods.isEmpty()) {
                ForgeHandshakeResult result = ForgeHandshakeProcessor.processMods(hackedPlayer, mods);
                if (result.hasTriggers()) {
                    runForgeActions(result.getTriggers(), playerUuid, playerName);
                }
            }
        }
    }

    private void runForgeActions(List<ForgeActionTrigger> triggers, UUID playerUuid, String playerName) {
        for (ForgeActionTrigger trigger : triggers) {
            for (Action action : trigger.getActions()) {
                performActions(action, playerUuid, playerName, trigger.getName(),
                        Placeholder.unparsed("player", playerName),
                        Placeholder.parsed("name", trigger.getName()));
            }
        }
    }

    private void performActions(Action action, UUID playerUuid, String playerName, String checkName, TagResolver.Single... templates) {
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("hackedserver.alert")) {
                    plugin.getAudiences().player(admin).sendMessage(action.getAlert(templates));
                }
            }
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.hasPermission("hackedserver.bypass")) {
            return;
        }

        // Check if player is fully online - if not, defer the actions
        HackedPlayer hackedPlayer = HackedServer.getPlayer(playerUuid);
        if (!isPlayerFullyOnline(playerUuid)) {
            hackedPlayer.queuePendingAction(() -> executeCommands(action, playerUuid, checkName));
            return;
        }

        executeCommands(action, playerUuid, checkName);
    }

    private boolean isPlayerFullyOnline(UUID playerUuid) {
        Player onlinePlayer = Bukkit.getPlayer(playerUuid);
        return onlinePlayer != null && onlinePlayer.isOnline();
    }

    private void executeCommands(Action action, UUID playerUuid, String checkName) {
        long delayTicks = action.getDelayTicks();

        Runnable commandRunner = () -> {
            Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }

            // Re-check bypass permission now that player is fully online
            if (onlinePlayer.hasPermission("hackedserver.bypass")) {
                return;
            }

            String playerName = onlinePlayer.getName();

            for (String command : action.getConsoleCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("<player>", playerName).replace("<name>", checkName));
            }
            for (String command : action.getPlayerCommands()) {
                Bukkit.dispatchCommand(onlinePlayer,
                        command.replace("<player>", playerName).replace("<name>", checkName));
            }
            for (String command : action.getOppedPlayerCommands()) {
                boolean op = onlinePlayer.isOp();
                onlinePlayer.setOp(true);
                Bukkit.dispatchCommand(onlinePlayer,
                        command.replace("<player>", playerName).replace("<name>", checkName));
                onlinePlayer.setOp(op);
            }
        };

        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, commandRunner, delayTicks);
        } else {
            Bukkit.getScheduler().runTask(plugin, commandRunner);
        }
    }
}
