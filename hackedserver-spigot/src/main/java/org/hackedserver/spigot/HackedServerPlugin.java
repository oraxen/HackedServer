package org.hackedserver.spigot;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.Message;
import org.hackedserver.spigot.listeners.CustomPayloadListener;
import org.hackedserver.spigot.utils.logs.Logs;

public class HackedServerPlugin extends JavaPlugin {

    private ProtocolManager protocolManager;
    private CustomPayloadListener customPayloadListener;
    private BukkitAudiences audiences;
    private static HackedServerPlugin instance;

    public HackedServerPlugin() throws NoSuchFieldException, IllegalAccessException {
        instance = this;
        Logs.enableFilter(this);
        ConfigsManager.init(Logs.getLogger(), getDataFolder());
    }

    @Override
    public void onEnable() {
        audiences = BukkitAudiences.create(this);
        Logs.onEnable(audiences);
        new Metrics(this, 2008);
        protocolManager = ProtocolLibrary.getProtocolManager();
        customPayloadListener = new CustomPayloadListener(protocolManager, this);
        customPayloadListener.register();
        Logs.logComponent(Message.PLUGIN_LOADED.toComponent());
    }

    @Override
    public void onDisable() {
        customPayloadListener.unregister();
    }

    public BukkitAudiences getAudiences() {
        return audiences;
    }

    public static HackedServerPlugin get() {
        return instance;
    }
}
