package org.hackedserver.spigot;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import dev.jorel.commandapi.CommandAPI;
import org.hackedserver.spigot.utils.BukkitWrapper;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.Message;
import org.hackedserver.spigot.commands.CommandsManager;
import org.hackedserver.spigot.listeners.CustomPayloadListener;
import org.hackedserver.spigot.listeners.HackedPlayerListeners;
import org.hackedserver.spigot.utils.logs.Logs;

public class HackedServerPlugin extends JavaPlugin {

    private ProtocolManager protocolManager;
    private CustomPayloadListener customPayloadListener;
    private BukkitAudiences audiences;
    private static HackedServerPlugin instance;

    public HackedServerPlugin() throws NoSuchFieldException, IllegalAccessException {
        instance = this;
        Logs.enableFilter(this);
        ConfigsManager.init(getDataFolder());
    }

    @Override
    public void onLoad() {
        // CommandAPI.onLoad(BukkitWrapper.createCommandApiConfig(this));
    }

    @Override
    public void onEnable() {
        // CommandAPI.onEnable();
        audiences = BukkitAudiences.create(this);
        Logs.onEnable(audiences);
        new Metrics(this, 2008);
        Bukkit.getPluginManager().registerEvents(new HackedPlayerListeners(), this);
        protocolManager = ProtocolLibrary.getProtocolManager();
        customPayloadListener = new CustomPayloadListener(protocolManager, this);
        customPayloadListener.register();
        new CommandsManager(this, audiences).loadCommands();
        Logs.logComponent(Message.PLUGIN_LOADED.toComponent());

        Bukkit.getOnlinePlayers().forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
    }

    @Override
    public void onDisable() {
        if (customPayloadListener != null) {
            customPayloadListener.unregister();
        }
        // CommandAPI.onDisable();
        HackedServer.clear();
    }

    public BukkitAudiences getAudiences() {
        return audiences;
    }

    public static HackedServerPlugin get() {
        return instance;
    }
}
