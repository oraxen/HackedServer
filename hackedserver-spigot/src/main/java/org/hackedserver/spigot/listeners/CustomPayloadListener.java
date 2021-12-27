package org.hackedserver.spigot.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.GenericCheck;
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
                ByteBuf buf = (ByteBuf) packet.getModifier().read(1);
                String channel = packet.getMinecraftKeys().read(0).getFullKey();
                HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
                String message = buf.toString(StandardCharsets.UTF_8);
                for (GenericCheck check : HackedServer.getChecks())
                    if (check.pass(channel, message)) {
                        hackedPlayer.addGenericCheck(check);
                        for (Action action : check.getActions()) {
                            performActions(action, player, Template.of("player",
                                    player.getName()), Template.of("name", check.getName()));
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

    private void performActions(Action action, Player player, Template... templates) {
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : Bukkit.getOnlinePlayers())
                if (admin.hasPermission("hackedserver.alert"))
                    HackedServerPlugin.get().getAudiences().player(player)
                            .sendMessage(action.getAlert(templates));
        }
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
