package org.hackedserver.spigot.hopper;

import md.thomas.hopper.Dependency;
import md.thomas.hopper.FailurePolicy;
import md.thomas.hopper.LogLevel;
import md.thomas.hopper.bukkit.BukkitHopper;
import md.thomas.hopper.version.UpdatePolicy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.hackedserver.core.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles automatic downloading of dependencies using Hopper.
 * <p>
 * This class registers and downloads:
 * <ul>
 *   <li>PacketEvents - Primary choice for hybrid servers (Arclight, Mohist, etc.)</li>
 *   <li>ProtocolLib - Fallback for standard Paper/Spigot servers</li>
 * </ul>
 * Downloaded plugins are automatically loaded at runtime without requiring a server restart.
 */
public final class HackedServerHopper {

    private static boolean downloadComplete = false;
    private static boolean requiresRestart = false;
    private static boolean enabled = true;
    private static Plugin registeredPlugin = null;

    // Pattern to match ProtocolLib jar files
    // Matches: ProtocolLib.jar, ProtocolLib-5.4.0.jar, ProtocolLib-SNAPSHOT.jar, ProtocolLib-beta.jar
    private static final Pattern PROTOCOLLIB_PATTERN = Pattern.compile(
        "(?i)^protocollib([-_][\\w.-]+)?\\.jar$"
    );

    // Pattern to match PacketEvents jar files
    // Matches: packetevents-spigot-2.11.1.jar, PacketEvents.jar, etc.
    private static final Pattern PACKETEVENTS_PATTERN = Pattern.compile(
        "(?i)^packetevents([-_][\\w.-]+)?\\.jar$"
    );

    private HackedServerHopper() {}

    /**
     * Registers dependencies with Hopper.
     * Should be called in the plugin constructor (after config is loaded).
     *
     * @param plugin the HackedServer plugin instance
     */
    public static void register(@NotNull Plugin plugin) {
        Logger logger = plugin.getLogger();

        // Reset all static flags to support plugin reload
        enabled = true;
        downloadComplete = false;
        requiresRestart = false;
        registeredPlugin = plugin;

        // Check if auto-download is disabled via system property (takes precedence)
        String prop = System.getProperty("hackedserver.autoDownloadDependencies");
        if ("false".equalsIgnoreCase(prop)) {
            enabled = false;
            logger.info("Auto-download of dependencies is disabled (via system property)");
            return;
        }

        // Check if auto-download is disabled via config (defaults to enabled)
        if (!Config.AUTO_DOWNLOAD_DEPENDENCIES.toBool(true)) {
            enabled = false;
            logger.info("Auto-download of dependencies is disabled (via config)");
            return;
        }

        BukkitHopper.register(plugin, deps -> {
            // We check for files in the plugins folder since plugins aren't loaded yet in constructor
            boolean hasProtocolLib = pluginJarExists(PROTOCOLLIB_PATTERN);
            boolean hasPacketEvents = pluginJarExists(PACKETEVENTS_PATTERN);
            boolean isHybrid = isHybridServer();

            // For hybrid servers (Arclight, Mohist, etc.), prefer PacketEvents
            // For standard servers, prefer ProtocolLib
            if (isHybrid) {
                // Hybrid server - download PacketEvents
                if (!hasPacketEvents) {
                    logger.info("Hybrid server detected - downloading PacketEvents for better compatibility");
                    // PacketEvents from Modrinth
                    deps.require(Dependency.modrinth("packetevents")
                        .name("packetevents")
                        .minVersion("2.7.0")
                        .updatePolicy(UpdatePolicy.MINOR)
                        .onFailure(FailurePolicy.WARN_SKIP)
                        .build());

                    // Fallback: GitHub releases
                    deps.require(Dependency.github("retrooper/packetevents")
                        .name("packetevents")
                        .minVersion("2.7.0")
                        .assetPattern("packetevents-spigot-.*\\.jar")
                        .updatePolicy(UpdatePolicy.MINOR)
                        .onFailure(FailurePolicy.WARN_SKIP)
                        .build());
                }
            } else {
                // Standard server - download ProtocolLib
                if (!hasProtocolLib && !hasPacketEvents) {
                    // Primary source: Hangar (PaperMC's plugin repository)
                    deps.require(Dependency.hangar("ProtocolLib")
                        .name("ProtocolLib")
                        .minVersion("5.3.0")
                        .updatePolicy(UpdatePolicy.MINOR)
                        .onFailure(FailurePolicy.WARN_SKIP)
                        .build());

                    // Fallback source: GitHub releases
                    deps.require(Dependency.github("dmulloy2/ProtocolLib")
                        .name("ProtocolLib")
                        .minVersion("5.3.0")
                        .assetPattern("ProtocolLib.jar")
                        .updatePolicy(UpdatePolicy.MINOR)
                        .onFailure(FailurePolicy.WARN_SKIP)
                        .build());
                }
            }
        });
    }

    /**
     * Downloads all registered dependencies and automatically loads them.
     * Should be called in the plugin's onLoad() method.
     * <p>
     * This method uses Hopper's auto-load feature to load downloaded plugins
     * at runtime without requiring a server restart.
     *
     * @param plugin the HackedServer plugin instance
     * @return true if all dependencies are satisfied and loaded, false if restart is required
     */
    public static boolean download(@NotNull Plugin plugin) {
        if (!enabled) {
            downloadComplete = true;
            return true;
        }

        Logger logger = plugin.getLogger();
        BukkitHopper.DownloadAndLoadResult result = BukkitHopper.downloadAndLoad(plugin, LogLevel.QUIET);

        downloadComplete = true;
        requiresRestart = !result.noRestartRequired();

        if (requiresRestart) {
            // Some plugins couldn't be auto-loaded - log details
            logger.warning("Some dependencies require a server restart to load:");
            for (var failed : result.loadResult().failed()) {
                logger.warning("  - " + failed.path().getFileName() + ": " + failed.error());
            }
        }

        return !requiresRestart;
    }

    /**
     * Checks if all dependencies are ready (downloaded and loaded).
     *
     * @param plugin the HackedServer plugin instance
     * @return true if ready
     */
    public static boolean isReady(@NotNull Plugin plugin) {
        return BukkitHopper.isReady(plugin);
    }

    /**
     * @return true if a restart is required to load newly downloaded dependencies
     */
    public static boolean requiresRestart() {
        return requiresRestart;
    }

    /**
     * @return true if the download phase has completed
     */
    public static boolean isDownloadComplete() {
        return downloadComplete;
    }

    /**
     * @return true if auto-download is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Detect if we're running on a hybrid server (Forge/Fabric + Bukkit).
     * These servers often have issues with ProtocolLib's packet injection.
     */
    private static boolean isHybridServer() {
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

    /**
     * Checks if a plugin jar file exists in the plugins folder using a regex pattern.
     * This is used during constructor phase when plugins haven't loaded yet.
     *
     * @param pattern regex pattern to match the plugin jar filename
     * @return true if a matching jar exists
     */
    private static boolean pluginJarExists(Pattern pattern) {
        java.io.File pluginsFolder = getPluginsFolder();
        if (pluginsFolder == null || !pluginsFolder.exists()) {
            return false;
        }

        java.io.File[] files = pluginsFolder.listFiles((dir, name) ->
            pattern.matcher(name).matches()
        );

        return files != null && files.length > 0;
    }

    /**
     * Gets the plugins folder in a way that's compatible with both Paper and Spigot/Arclight.
     * Paper has Bukkit.getPluginsFolder(), but Spigot/Arclight don't have this method.
     *
     * @return the plugins folder, or null if it cannot be determined
     */
    private static java.io.File getPluginsFolder() {
        // First try Paper's method (Bukkit.getPluginsFolder())
        try {
            return Bukkit.getPluginsFolder();
        } catch (NoSuchMethodError ignored) {
            // Paper method not available - fall through to alternatives
        }

        // Try using the registered plugin's data folder parent
        if (registeredPlugin != null) {
            java.io.File dataFolder = registeredPlugin.getDataFolder();
            if (dataFolder != null) {
                return dataFolder.getParentFile();
            }
        }

        // Last resort: assume standard "plugins" folder relative to working directory
        return new java.io.File("plugins");
    }
}
