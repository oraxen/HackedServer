package org.hackedserver.spigot.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.spigot.utils.logs.Logs;

public class CustomPayloadListener {

    private final ProtocolManager protocolManager;
    private final PacketAdapter adapter;

    public CustomPayloadListener(ProtocolManager protocolManager, JavaPlugin plugin) {
        this.protocolManager = protocolManager;
        this.adapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.CUSTOM_PAYLOAD) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                ByteBuf buf = (ByteBuf) packet.getModifier().read(1);
                String channel = packet.getMinecraftKeys().read(0).getFullKey();

                Logs.logError("channel: " + channel);
                Logs.logError("content: " + buf.toString(Charsets.UTF_8));
            }
        };
    }

    public void register() {
        protocolManager.addPacketListener(adapter);
    }

    public void unregister() {
        protocolManager.removePacketListener(adapter);
    }

}
