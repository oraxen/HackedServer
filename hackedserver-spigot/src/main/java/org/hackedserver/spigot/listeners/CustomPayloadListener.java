package org.hackedserver.spigot.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

import org.bukkit.entity.Player;
import org.hackedserver.spigot.HackedServerPlugin;

import java.nio.charset.StandardCharsets;

public class CustomPayloadListener {

    private final ProtocolManager protocolManager;
    private final PacketAdapter adapter;

    public CustomPayloadListener(ProtocolManager protocolManager, HackedServerPlugin plugin) {
        this.protocolManager = protocolManager;
        this.adapter = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Login.Client.CUSTOM_PAYLOAD,
                PacketType.Configuration.Client.CUSTOM_PAYLOAD,
                PacketType.Play.Client.CUSTOM_PAYLOAD) {

            @Override
            public void onPacketReceiving(PacketEvent event) {
                try {
                    handlePacket(event);
                } catch (Exception e) {
                    // Never let packet processing errors disconnect the player
                    plugin.getLogger().warning("Error processing custom payload packet: " + e.getMessage());
                }
            }

            private void handlePacket(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                PacketContainer packet = event.getPacket();
                StructureModifier<?> modifier = packet.getModifier();

                Object value = modifier.read(0);
                if (value == null)
                    return;

                String channel;
                String message;

                // Resolve channel first, then data separately, so that a
                // successful channel read is never overwritten by a later failure.
                try {
                    java.lang.reflect.Field idField = value.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    Object minecraftKey = idField.get(value);
                    channel = minecraftKey.toString();
                } catch (NoSuchFieldException | IllegalArgumentException e) {
                    // IllegalArgumentException: "object is not an instance of declaring class"
                    // can occur on hybrid servers (Arclight, Mohist) where Forge patches
                    // packet classes and class loader isolation causes reflection mismatches
                    channel = value.toString();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    java.lang.reflect.Field dataField = value.getClass().getDeclaredField("data");
                    dataField.setAccessible(true);
                    Object byteBuf = dataField.get(value);
                    message = readByteBuf(byteBuf);
                } catch (NoSuchFieldException | IllegalArgumentException e) {
                    // Fallback: read data from modifier on hybrid servers
                    message = readByteBuf(modifier.read(1));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return;
                }

                if (event.isPlayerTemporary()) {
                    PendingPayloads.queue(player, channel, message);
                    return;
                }

                PayloadProcessor.process(player, channel, message);
            }

        };
    }

    public void register() {
        protocolManager.addPacketListener(adapter);
    }

    public void unregister() {
        protocolManager.removePacketListener(adapter);
    }

    private String readByteBuf(Object byteBuf) {
        if (byteBuf == null)
            return "unknown";

        try {
            // Handle common buffer representations across MC versions
            if (byteBuf instanceof io.netty.buffer.ByteBuf nettyBuf) {
                int readable = nettyBuf.readableBytes();
                byte[] bytes = new byte[readable];
                nettyBuf.getBytes(nettyBuf.readerIndex(), bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            }

            if (byteBuf instanceof byte[] bytes) {
                return new String(bytes, StandardCharsets.UTF_8);
            }

            if (byteBuf instanceof java.nio.ByteBuffer nioBuf) {
                java.nio.ByteBuffer duplicate = nioBuf.asReadOnlyBuffer();
                duplicate.rewind();
                byte[] bytes = new byte[duplicate.remaining()];
                duplicate.get(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            }

            // Fallback to reflective toString(Charset) for older representations
            java.lang.reflect.Method toString = byteBuf.getClass().getMethod("toString",
                    java.nio.charset.Charset.class);
            return (String) toString.invoke(byteBuf, StandardCharsets.UTF_8);
        } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException
                | IllegalAccessException ex) {
            ex.printStackTrace();
            return byteBuf.toString();
        }
    }

}
