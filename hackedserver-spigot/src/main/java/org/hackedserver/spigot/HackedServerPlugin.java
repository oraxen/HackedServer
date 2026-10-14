package org.hackedserver.spigot;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
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
import org.hackedserver.spigot.utils.HackedAudiences;
import org.hackedserver.spigot.utils.VersionUtil;
import org.hackedserver.spigot.utils.logs.Logs;
import org.jetbrains.annotations.Nullable;

public class HackedServerPlugin extends JavaPlugin {

    private HackedAudiences audiences;
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
        if (isPacketEventsPresent()) {
            packetEventsIntegration = createPacketEventsIntegration();
        }
    }

    @Override
    public void onEnable() {
        // Set up messaging first so every message below reaches the console
        audiences = HackedAudiences.create(this);
        Logs.onEnable(audiences);

        // Only trust packet libraries that actually enabled: a plugin that failed to start
        // (e.g. an outdated build on a new Minecraft version) is still returned by getPlugin()
        protocolLibAvailable = isPluginEnabled("ProtocolLib");

        Plugin packetEventsPlugin = Bukkit.getPluginManager().getPlugin("packetevents");
        if (packetEventsPlugin != null && !packetEventsPlugin.isEnabled()) {
            Logs.logWarning("packetevents " + packetEventsPlugin.getDescription().getVersion()
                    + " is installed but not enabled (it probably failed to start), ignoring it."
                    + (VersionUtil.isMinecraft26OrNewer() ? " Minecraft 26.x requires packetevents 2.14.0+." : ""));
            packetEventsIntegration = null;
        } else if (packetEventsIntegration == null && isPacketEventsPresent()) {
            // Re-check PacketEvents availability during onEnable() if not found during onLoad()
            // Paper's new plugin system may not have loaded packetevents' classes during our onLoad()
            packetEventsIntegration = createPacketEventsIntegration();
        }
        packetEventsAvailable = packetEventsIntegration != null && packetEventsIntegration.isUsable();
        if (!packetEventsAvailable) {
            packetEventsIntegration = null;
        }

        // Determine which packet library to use
        // Prefer ProtocolLib on standard Paper/Spigot, use PacketEvents on hybrid servers like Arclight
        boolean useProtocolLib = protocolLibAvailable && !(isHybridServer() && packetEventsAvailable);
        boolean usePacketEvents = packetEventsAvailable && !useProtocolLib;

        if (!useProtocolLib && !usePacketEvents) {
            if (HackedServerHopper.requiresRestart()) {
                Logs.logWarning("A packet library was downloaded but requires a server restart to load.");
                Logs.logWarning("Please restart your server to enable HackedServer functionality.");
            } else if (!HackedServerHopper.isEnabled()) {
                Logs.logWarning("No packet library (ProtocolLib or PacketEvents) is installed and auto-download is disabled.");
                Logs.logWarning("Please install one manually or enable auto_download_dependencies in config.toml");
            } else {
                Logs.logError("No packet library available! HackedServer will have limited functionality.");
                Logs.logError("Please install ProtocolLib or PacketEvents manually.");
            }
        }

        new Metrics(this, 2008);

        // Initialize bedrock detection (auto-detects Geyser/Floodgate APIs)
        BedrockDetector.initialize(getLogger());
        Bukkit.getPluginManager().registerEvents(new HackedPlayerListeners(), this);

        if (useProtocolLib) {
            // Load ProtocolLib integration in a separate class to avoid NoClassDefFoundError
            // when ProtocolLib is not installed
            try {
                protocolLibIntegration = new ProtocolLibIntegration(this);
                protocolLibIntegration.register();
                Logs.logStatus("Using ProtocolLib for packet interception");
            } catch (Throwable e) {
                protocolLibIntegration = null;
                Logs.logError("Failed to initialize ProtocolLib: " + e);
                // Try to fall back to PacketEvents
                if (packetEventsAvailable) {
                    Logs.logStatus("Falling back to PacketEvents...");
                    usePacketEvents = true;
                }
            }
        }

        if (usePacketEvents) {
            try {
                packetEventsIntegration.register();
                Logs.logStatus("Using PacketEvents for packet interception");
            } catch (Throwable e) {
                Logs.logError("Failed to initialize PacketEvents: " + e);
            }
        }

        lunarApolloListener = new LunarApolloListener(this);

        registerSignProbing();

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

    /**
     * Registers sign translation probing (active mod detection).
     * Probe replies are consumed at the packet level when PacketEvents is running, and read from
     * {@code SignChangeEvent} otherwise, so probing never depends on an optional library loading.
     */
    private void registerSignProbing() {
        // SignTranslationProber uses 1.20+ sign APIs (Side, SignSide, Player#openSign)
        // and Paper's Adventure sign lines
        if (!isSignApiAvailable() || !VersionUtil.isPaperServer()) {
            Logs.logStatus("Sign translation probing requires Paper 1.20+ - disabled on this server");
            return;
        }
        try {
            signTranslationProber = new SignTranslationProber();
            Bukkit.getPluginManager().registerEvents(signTranslationProber, this);
        } catch (Throwable e) {
            signTranslationProber = null;
            Logs.logWarning("Failed to register sign translation probing: " + e);
            return;
        }

        if (packetEventsIntegration != null && packetEventsIntegration.isReadyForListeners()) {
            try {
                packetEventsIntegration.registerSignListener(signTranslationProber);
                Logs.logStatus("Sign translation probing enabled (PacketEvents)");
                return;
            } catch (Throwable e) {
                Logs.logWarning("Failed to register PacketEvents sign listener, using sign events instead: " + e);
            }
        }
        Logs.logStatus("Sign translation probing enabled (sign events)");
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
        if (audiences != null) {
            Logs.onEnable(null);
            audiences.close();
            audiences = null;
        }
    }

    @Nullable
    private PacketEventsIntegration createPacketEventsIntegration() {
        try {
            PacketEventsIntegration integration = new PacketEventsIntegration(this);
            integration.load();
            return integration;
        } catch (Throwable e) {
            Logs.logWarning("Failed to initialize PacketEvents: " + e);
            return null;
        }
    }

    private static boolean isPluginEnabled(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
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
        } catch (ClassNotFoundException | LinkageError e) {
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

    public HackedAudiences getAudiences() {
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
