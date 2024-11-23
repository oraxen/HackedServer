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
import org.hackedserver.core.config.Config;
import org.hackedserver.core.config.Action;
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
        this.adapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.CUSTOM_PAYLOAD) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                StructureModifier<?> modifier = packet.getModifier();

                // Get the first field which contains the DiscardedPayload
                Object value = modifier.read(0);
                if (value != null && value.toString().contains("DiscardedPayload")) {
                    try {
                        // Get the channel/id field directly
                        java.lang.reflect.Field idField = value.getClass().getDeclaredField("id");
                        idField.setAccessible(true);
                        Object minecraftKey = idField.get(value);
                        String channel = minecraftKey.toString(); // MinecraftKey has a proper toString()

                        // Get the data field from DiscardedPayload
                        java.lang.reflect.Field dataField = value.getClass().getDeclaredField("data");
                        dataField.setAccessible(true);
                        Object byteBuf = dataField.get(value);

                        // Get the array field from ByteBuf
                        java.lang.reflect.Method toString = byteBuf.getClass().getMethod("toString",
                                java.nio.charset.Charset.class);
                        String message = (String) toString.invoke(byteBuf, StandardCharsets.UTF_8);
                        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
                        for (GenericCheck check : HackedServer.getChecks())
                            if (check.pass(channel, message)) {
                                hackedPlayer.addGenericCheck(check);
                                for (Action action : check.getActions())
                                    performActions(action, player, Placeholder.unparsed("player",
                                            player.getName()), Placeholder.parsed("name", check.getName()));
                            }

                    } catch (Exception e) {
                        e.printStackTrace();
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

    private void performActions(Action action, Player player, TagResolver.Single... templates) {
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : Bukkit.getOnlinePlayers())
                if (admin.hasPermission("hackedserver.alert"))
                    HackedServerPlugin.get().getAudiences().player(admin)
                            .sendMessage(action.getAlert(templates));
        }
        if (player.hasPermission("hackedserver.bypass"))
            return;
        for (String command : action.getConsoleCommands())
            Bukkit.getScheduler().runTask(HackedServerPlugin.get(),
                    () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            command.replace("<player>",
                                    player.getName())));
        for (String command : action.getPlayerCommands())
            Bukkit.getScheduler().runTask(HackedServerPlugin.get(),
                    () -> Bukkit.dispatchCommand(player,
                            command.replace("<player>",
                                    player.getName())));
        for (String command : action.getOppedPlayerCommands()) {
            boolean op = player.isOp();
            player.setOp(true);
            Bukkit.getScheduler().runTask(HackedServerPlugin.get(),
                    () -> Bukkit.dispatchCommand(player,
                            command.replace("<player>",
                                    player.getName())));
            player.setOp(op);
        }
    }

}
