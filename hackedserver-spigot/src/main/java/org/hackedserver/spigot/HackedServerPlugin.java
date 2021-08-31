package org.hackedserver.spigot;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.spigot.listeners.CustomPayloadListener;
import org.hackedserver.spigot.utils.logs.Logs;

public class HackedServerPlugin extends JavaPlugin {

    private ProtocolManager protocolManager;
    private CustomPayloadListener customPayloadListener;

    public HackedServerPlugin() throws NoSuchFieldException, IllegalAccessException {
        Logs.enableFilter(this);
    }

    @Override
    public void onEnable() {
        new Metrics(this, 2008);
        protocolManager = ProtocolLibrary.getProtocolManager();
        customPayloadListener = new CustomPayloadListener(protocolManager, this);
        customPayloadListener.register();
    }

    @Override
    public void onDisable() {
        customPayloadListener.unregister();
    }
}
