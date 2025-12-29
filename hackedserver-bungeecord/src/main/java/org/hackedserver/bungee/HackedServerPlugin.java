package org.hackedserver.bungee;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.hackedserver.bungee.apollo.ApolloIntegration;
import org.hackedserver.bungee.commands.CommandsManager;
import org.hackedserver.bungee.listeners.CustomPayloadListener;
import org.hackedserver.bungee.listeners.HackedPlayerListeners;
import org.hackedserver.bungee.logs.Logs;
import org.hackedserver.core.config.ConfigsManager;

public class HackedServerPlugin extends Plugin {

    private BungeeAudiences audiences;
    private static HackedServerPlugin instance;
    private ApolloIntegration apolloIntegration;

    @Override
    public void onEnable() {
        instance = this;
        audiences = BungeeAudiences.create(this);
        Logs.onEnable(getLogger(), audiences);
        ConfigsManager.init(getDataFolder());
        PluginManager pluginManager = this.getProxy().getPluginManager();
        pluginManager.registerListener(this, new HackedPlayerListeners());
        pluginManager.registerListener(this, new CustomPayloadListener());
        pluginManager.registerCommand(this, new CommandsManager(this.getProxy(), getDataFolder()));

        if (isApolloAvailable()) {
            apolloIntegration = new ApolloIntegration(getProxy());
            apolloIntegration.register();
        }
    }

    @Override
    public void onDisable() {
        if (apolloIntegration != null) {
            apolloIntegration.unregister();
        }
    }

    public static HackedServerPlugin get() {
        return instance;
    }

    public BungeeAudiences getAudiences() {
        return audiences;
    }

    private boolean isApolloAvailable() {
        if (getProxy().getPluginManager().getPlugin("Apollo") == null) {
            return false;
        }
        try {
            Class.forName("com.lunarclient.apollo.event.EventBus");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
