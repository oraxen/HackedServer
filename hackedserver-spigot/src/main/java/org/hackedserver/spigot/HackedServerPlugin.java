package org.hackedserver.spigot;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.Message;
import org.hackedserver.spigot.listeners.CustomPayloadListener;
import org.hackedserver.spigot.utils.logs.Logs;

public class HackedServerPlugin extends JavaPlugin {

    private ProtocolManager protocolManager;
    private CustomPayloadListener customPayloadListener;

    public HackedServerPlugin() throws NoSuchFieldException, IllegalAccessException {
        Logs.enableFilter(this);
        ConfigsManager.init(Logs.getLogger(), getDataFolder());
    }

    @Override
    public void onEnable() {
        Logs.onEnable();
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
}
