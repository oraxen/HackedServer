package org.hackedserver.spigot;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.Message;
import org.hackedserver.spigot.commands.CommandsManager;
import org.hackedserver.spigot.hopper.HackedServerHopper;
import org.hackedserver.spigot.listeners.HackedPlayerListeners;
import org.hackedserver.spigot.protocol.ProtocolLibIntegration;
import org.hackedserver.spigot.utils.logs.Logs;
import org.jetbrains.annotations.Nullable;

public class HackedServerPlugin extends JavaPlugin {

    private BukkitAudiences audiences;
    private static HackedServerPlugin instance;
    private boolean protocolLibAvailable = false;
    @Nullable
    private ProtocolLibIntegration protocolLibIntegration;

    public HackedServerPlugin() throws NoSuchFieldException, IllegalAccessException {
        instance = this;
        Logs.enableFilter(this);
        ConfigsManager.init(getDataFolder());
        // Register dependencies with Hopper for auto-download
        HackedServerHopper.register(this);
    }

    @Override
    public void onLoad() {
        // Download and load dependencies via Hopper
        HackedServerHopper.download(this);
        // CommandAPI.onLoad(BukkitWrapper.createCommandApiConfig(this));
    }

    @Override
    public void onEnable() {
        // Check if ProtocolLib is available
        protocolLibAvailable = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;

        if (!protocolLibAvailable) {
            if (HackedServerHopper.requiresRestart()) {
                getLogger().warning("ProtocolLib was downloaded but requires a server restart to load.");
                getLogger().warning("Please restart your server to enable HackedServer functionality.");
            } else if (!HackedServerHopper.isEnabled()) {
                getLogger().warning("ProtocolLib is not installed and auto-download is disabled.");
                getLogger().warning("Please install ProtocolLib manually or enable auto_download_dependencies in config.toml");
            } else {
                getLogger().severe("ProtocolLib is not installed and could not be auto-downloaded!");
                getLogger().severe("Please install ProtocolLib manually or check your network connection.");
            }
            // Still enable the plugin but with limited functionality
        }

        // CommandAPI.onEnable();
        audiences = BukkitAudiences.create(this);
        Logs.onEnable(audiences);
        new Metrics(this, 2008);
        Bukkit.getPluginManager().registerEvents(new HackedPlayerListeners(), this);

        if (protocolLibAvailable) {
            // Load ProtocolLib integration in a separate class to avoid NoClassDefFoundError
            // when ProtocolLib is not installed
            protocolLibIntegration = new ProtocolLibIntegration(this);
            protocolLibIntegration.register();
        }

        new CommandsManager(this, audiences).loadCommands();
        Logs.logComponent(Message.PLUGIN_LOADED.toComponent());

        Bukkit.getOnlinePlayers().forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
    }

    @Override
    public void onDisable() {
        if (protocolLibIntegration != null) {
            protocolLibIntegration.unregister();
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

    public boolean isProtocolLibAvailable() {
        return protocolLibAvailable;
    }
}
