package org.hackedserver.spigot;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.bedrock.BedrockDetector;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.Message;
import org.hackedserver.spigot.commands.BukkitCommandFallback;
import org.hackedserver.spigot.commands.CommandsManager;
import org.hackedserver.spigot.hopper.HackedServerHopper;
import org.hackedserver.spigot.listeners.HackedPlayerListeners;
import org.hackedserver.spigot.listeners.LunarApolloListener;
import org.hackedserver.spigot.probing.SignTranslationProber;
import org.hackedserver.spigot.protocol.PacketEventsIntegration;
import org.hackedserver.spigot.protocol.ProtocolLibIntegration;
import org.hackedserver.spigot.utils.logs.Logs;
import org.jetbrains.annotations.Nullable;

public class HackedServerPlugin extends JavaPlugin {

    private BukkitAudiences audiences;
    private static HackedServerPlugin instance;
    private boolean protocolLibAvailable = false;
    private boolean packetEventsAvailable = false;
    @Nullable
    private ProtocolLibIntegration protocolLibIntegration;
    @Nullable
    private PacketEventsIntegration packetEventsIntegration;
    @Nullable
    private LunarApolloListener lunarApolloListener;
    @Nullable
    private SignTranslationProber signTranslationProber;

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

        // Check for PacketEvents availability and initialize if present
        // Must be done in onLoad() before onEnable()
        packetEventsAvailable = isPacketEventsPresent();
        if (packetEventsAvailable) {
            try {
                packetEventsIntegration = new PacketEventsIntegration(this);
                packetEventsIntegration.load();
            } catch (Throwable e) {
                getLogger().warning("Failed to initialize PacketEvents: " + e.getMessage());
                packetEventsAvailable = false;
                packetEventsIntegration = null;
            }
        }
    }

    @Override
    public void onEnable() {
        // Check if ProtocolLib is available
        protocolLibAvailable = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;

        // Re-check PacketEvents availability during onEnable() if not found during onLoad()
        // Paper's new plugin system may not have loaded packetevents' classes during our onLoad()
        if (!packetEventsAvailable) {
            packetEventsAvailable = isPacketEventsPresent();
            if (packetEventsAvailable) {
                try {
                    packetEventsIntegration = new PacketEventsIntegration(this);
                    packetEventsIntegration.load();
                } catch (Throwable e) {
                    getLogger().warning("Failed to initialize PacketEvents: " + e.getMessage());
                    packetEventsAvailable = false;
                    packetEventsIntegration = null;
                }
            }
        }

        // Determine which packet library to use
        // Prefer ProtocolLib on standard Paper/Spigot, use PacketEvents on hybrid servers like Arclight
        boolean useProtocolLib = protocolLibAvailable && !isHybridServer();
        boolean usePacketEvents = packetEventsAvailable && !useProtocolLib;

        if (!useProtocolLib && !usePacketEvents) {
            if (HackedServerHopper.requiresRestart()) {
                getLogger().warning("A packet library was downloaded but requires a server restart to load.");
                getLogger().warning("Please restart your server to enable HackedServer functionality.");
            } else if (!HackedServerHopper.isEnabled()) {
                getLogger().warning("No packet library (ProtocolLib or PacketEvents) is installed and auto-download is disabled.");
                getLogger().warning("Please install one manually or enable auto_download_dependencies in config.toml");
            } else {
                getLogger().severe("No packet library available! HackedServer will have limited functionality.");
                getLogger().severe("Please install ProtocolLib or PacketEvents manually.");
            }
        }

        audiences = BukkitAudiences.create(this);
        Logs.onEnable(audiences);
        new Metrics(this, 2008);

        // Initialize bedrock detection (auto-detects Geyser/Floodgate APIs)
        BedrockDetector.initialize(getLogger());
        Bukkit.getPluginManager().registerEvents(new HackedPlayerListeners(), this);

        if (useProtocolLib) {
            // Load ProtocolLib integration in a separate class to avoid NoClassDefFoundError
            // when ProtocolLib is not installed
            getLogger().info("Using ProtocolLib for packet interception");
            try {
                protocolLibIntegration = new ProtocolLibIntegration(this);
                protocolLibIntegration.register();
            } catch (Throwable e) {
                getLogger().severe("Failed to initialize ProtocolLib: " + e.getMessage());
                // Try to fall back to PacketEvents
                if (packetEventsAvailable) {
                    getLogger().info("Falling back to PacketEvents...");
                    usePacketEvents = true;
                }
            }
        }

        if (usePacketEvents && packetEventsIntegration != null) {
            getLogger().info("Using PacketEvents for packet interception (better for hybrid servers like Arclight)");
            try {
                packetEventsIntegration.register();
            } catch (Throwable e) {
                getLogger().severe("Failed to initialize PacketEvents: " + e.getMessage());
            }
        }

        lunarApolloListener = new LunarApolloListener(this);

        // Register sign translation probing (active mod detection via packets)
        // Only register when PacketEvents is actually initialized (usePacketEvents or standalone plugin)
        // SignTranslationProber uses 1.20+ sign APIs (Side, SignSide, Player#openSign)
        boolean packetEventsInitialized = packetEventsIntegration != null
                && packetEventsIntegration.isReadyForListeners();
        if (packetEventsAvailable && packetEventsInitialized) {
            if (isSignApiAvailable()) {
                try {
                    signTranslationProber = new SignTranslationProber();
                    Bukkit.getPluginManager().registerEvents(signTranslationProber, this);
                    signTranslationProber.register();
                    getLogger().info("Sign translation probing enabled (PacketEvents)");
                } catch (Throwable e) {
                    signTranslationProber = null;
                    getLogger().warning("Failed to register sign translation probing: " + e.getMessage());
                }
            } else {
                getLogger().info("Sign translation probing requires 1.20+ server APIs - disabled on this version");
            }
        } else if (!packetEventsAvailable) {
            getLogger().warning("Sign translation probing requires PacketEvents - disabled");
        } else {
            getLogger().warning("Sign translation probing requires an initialized PacketEvents API - disabled");
        }

        // Try to load commands with CommandAPI, fall back to Bukkit commands if unavailable
        boolean commandsRegistered = false;
        try {
            new CommandsManager(this, audiences).loadCommands();
            commandsRegistered = true;
        } catch (LinkageError e) {
            // CommandAPI not available - will use Bukkit fallback below
        }
        if (!commandsRegistered) {
            BukkitCommandFallback fallback = new BukkitCommandFallback(this, audiences);
            var cmd = getCommand("hackedserver");
            if (cmd != null) {
                cmd.setExecutor(fallback);
                cmd.setTabCompleter(fallback);
            }
        }
        Logs.logComponent(Message.PLUGIN_LOADED.toComponent());

        Bukkit.getOnlinePlayers().forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
    }

    @Override
    public void onDisable() {
        if (protocolLibIntegration != null) {
            protocolLibIntegration.unregister();
        }
        if (packetEventsIntegration != null) {
            packetEventsIntegration.unregister();
        }
        if (lunarApolloListener != null) {
            lunarApolloListener.unregister();
        }
        if (signTranslationProber != null) {
            signTranslationProber.unregister();
        }
        HackedServer.clear();
    }

    /**
     * Check if PacketEvents is present on the server.
     */
    private boolean isPacketEventsPresent() {
        // Check for PacketEvents plugin
        if (Bukkit.getPluginManager().getPlugin("packetevents") != null) {
            return true;
        }
        // Also check if the PacketEvents classes are available (could be shaded)
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if the 1.20+ sign API (Side, SignSide, Player#openSign) is available.
     * These classes were added in Bukkit 1.20 and are required by SignTranslationProber.
     */
    private boolean isSignApiAvailable() {
        try {
            Class.forName("org.bukkit.block.sign.Side");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Detect if we're running on a hybrid server (Forge/Fabric + Bukkit).
     * These servers often have issues with ProtocolLib's packet injection.
     */
    private boolean isHybridServer() {
        String serverVersion = Bukkit.getVersion().toLowerCase();
        String serverName = Bukkit.getName().toLowerCase();

        // Check for known hybrid server software
        return serverVersion.contains("arclight") ||
                serverVersion.contains("mohist") ||
                serverVersion.contains("catserver") ||
                serverVersion.contains("magma") ||
                serverVersion.contains("crucible") ||
                serverVersion.contains("thermos") ||
                serverVersion.contains("kcauldron") ||
                serverVersion.contains("uranium") ||
                serverName.contains("arclight") ||
                serverName.contains("mohist") ||
                serverName.contains("catserver") ||
                serverName.contains("magma");
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

    public boolean isPacketEventsAvailable() {
        return packetEventsAvailable;
    }
}
