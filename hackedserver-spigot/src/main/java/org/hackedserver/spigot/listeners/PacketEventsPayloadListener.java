package org.hackedserver.spigot.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientPluginResponse;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hackedserver.spigot.HackedServerPlugin;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * PacketEvents-based custom payload listener for Spigot/Paper/Folia/Arclight servers.
 * This is an alternative to the ProtocolLib-based listener that works better
 * with hybrid server software like Arclight and on Minecraft 26.x.
 * <p>
 * Payloads received before the Bukkit player exists (login and configuration phase, where
 * 1.20.2+ clients send {@code minecraft:brand} and {@code minecraft:register}) are queued and
 * processed on join, exactly like the ProtocolLib listener does.
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
            try {
                handlePacket(event);
            } catch (Exception e) {
                // Never let packet processing errors disconnect the player.
                // This can happen on hybrid servers (Arclight, Mohist) where Forge-patched
                // classes cause reflection mismatches inside PacketEvents wrappers.
                plugin.getLogger().warning("Error processing custom payload packet: " + e.getMessage());
            }
        }
    }

    private void handlePacket(PacketReceiveEvent event) {
        User user = event.getUser();
        UUID playerUuid = user.getUUID();
        if (playerUuid == null) {
            return;
        }

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

        String message = new String(data != null ? data : new byte[0], StandardCharsets.UTF_8);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            PendingPayloads.queue(playerUuid, channel, message);
            return;
        }

        PayloadProcessor.process(player, channel, message);
    }
}
