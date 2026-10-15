package org.hackedserver.spigot.listeners;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.lunar.LunarActionTrigger;
import org.hackedserver.core.lunar.LunarApolloHandshakeParser;
import org.hackedserver.core.lunar.LunarHandshakeProcessor;
import org.hackedserver.core.lunar.LunarHandshakeResult;

public final class LunarApolloListener implements PluginMessageListener {

    private final JavaPlugin plugin;

    public LunarApolloListener(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin,
                LunarApolloHandshakeParser.CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin,
                LunarApolloHandshakeParser.CHANNEL);
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin,
                LunarApolloHandshakeParser.CHANNEL, this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin,
                LunarApolloHandshakeParser.CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!LunarApolloHandshakeParser.CHANNEL.equalsIgnoreCase(channel)) {
            return;
        }

        LunarApolloHandshakeParser.parseMods(message).ifPresent(mods -> {
            HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
            LunarHandshakeResult result = LunarHandshakeProcessor.process(hackedPlayer, mods);
            if (!result.hasTriggers()) {
                return;
            }
            for (LunarActionTrigger trigger : result.getTriggers()) {
                PayloadProcessor.runActions(player, trigger.getName(), trigger.getActions());
            }
        });
    }
}
