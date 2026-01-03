package org.hackedserver.spigot.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

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
import org.hackedserver.spigot.HackedServerPlugin;
import org.hackedserver.spigot.utils.logs.Logs;

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
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                StructureModifier<?> modifier = packet.getModifier();

                Object value = modifier.read(0);
                if (value == null)
                    return;

                String channel;
                String message;

                try {
                    // Get the channel/id field directly
                    java.lang.reflect.Field idField = value.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    Object minecraftKey = idField.get(value);
                    channel = minecraftKey.toString();

                    // Get the data field from DiscardedPayload
                    java.lang.reflect.Field dataField = value.getClass().getDeclaredField("data");
                    dataField.setAccessible(true);
                    Object byteBuf = dataField.get(value);
                    message = readByteBuf(byteBuf);

                } catch (NoSuchFieldException e) {
                    channel = value.toString();
                    message = readByteBuf(modifier.read(1));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return;
                }

                if (Config.DEBUG.toBool()) {
                    Logs.logComponent(Message.DEBUG_MESSAGE.toComponent(
                            Placeholder.unparsed("player", player.getName()),
                            Placeholder.unparsed("channel", channel),
                            Placeholder.unparsed("message", message)));
                }

                // Generic checks
                HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
                for (GenericCheck check : HackedServer.getChecks()) {
                    if (check.pass(hackedPlayer, channel, message)) {
                        hackedPlayer.addGenericCheck(check);
                        for (Action action : check.getActions())
                            performActions(action, player, check.getName(),
                                    Placeholder.unparsed("player", player.getName()),
                                    Placeholder.parsed("name", check.getName()));
                    }
                }
            }

        };
    }

    public void register() {
        protocolManager.addPacketListener(adapter);
    }

    public void unregister() {
        protocolManager.removePacketListener(adapter);
    }

    private void performActions(Action action, Player player, String checkName, TagResolver.Single... templates) {
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : Bukkit.getOnlinePlayers())
                if (admin.hasPermission("hackedserver.alert"))
                    HackedServerPlugin.get().getAudiences().player(admin)
                            .sendMessage(action.getAlert(templates));
        }
        if (player.hasPermission("hackedserver.bypass"))
            return;

        // Check if player is fully online - if not, defer the actions
        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
        if (!isPlayerFullyOnline(player)) {
            hackedPlayer.queuePendingAction(() -> executeCommands(action, player, checkName));
            return;
        }

        executeCommands(action, player, checkName);
    }

    private boolean isPlayerFullyOnline(Player player) {
        // During login/config phase, the player object exists but isn't fully joined
        // Check if the player can be found in the online players list
        Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
        return onlinePlayer != null && onlinePlayer.isOnline();
    }

    private void executeCommands(Action action, Player player, String checkName) {
        long delayTicks = action.getDelayTicks();

        // Schedule the commands with the configured delay
        // This ensures the player is fully connected before executing actions like kick
        Runnable commandRunner = () -> {
            // Re-fetch the player to ensure we have the current online instance
            Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return; // Player logged out before we could execute
            }

            for (String command : action.getConsoleCommands())
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("<player>",
                                onlinePlayer.getName()).replace("<name>", checkName));
            for (String command : action.getPlayerCommands())
                Bukkit.dispatchCommand(onlinePlayer,
                        command.replace("<player>",
                                onlinePlayer.getName()).replace("<name>", checkName));
            for (String command : action.getOppedPlayerCommands()) {
                boolean op = onlinePlayer.isOp();
                onlinePlayer.setOp(true);
                Bukkit.dispatchCommand(onlinePlayer,
                        command.replace("<player>",
                                onlinePlayer.getName()).replace("<name>", checkName));
                onlinePlayer.setOp(op);
            }
        };

        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), commandRunner, delayTicks);
        } else {
            Bukkit.getScheduler().runTask(HackedServerPlugin.get(), commandRunner);
        }
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
