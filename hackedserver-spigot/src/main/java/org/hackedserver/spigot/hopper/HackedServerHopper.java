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
 *   <li>ProtocolLib - Required for HackedServer packet interception</li>
 * </ul>
 * Downloaded plugins are automatically loaded at runtime without requiring a server restart.
 */
public final class HackedServerHopper {

    private static boolean downloadComplete = false;
    private static boolean requiresRestart = false;
    private static boolean enabled = true;

    // Pattern to match ProtocolLib jar files
    // Matches: ProtocolLib.jar, ProtocolLib-5.4.0.jar, ProtocolLib-SNAPSHOT.jar, ProtocolLib-beta.jar
    private static final Pattern PROTOCOLLIB_PATTERN = Pattern.compile(
        "(?i)^protocollib([-_][\\w.-]+)?\\.jar$"
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

            // ProtocolLib is required for HackedServer packet interception
            if (!hasProtocolLib) {
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
     * Checks if a plugin jar file exists in the plugins folder using a regex pattern.
     * This is used during constructor phase when plugins haven't loaded yet.
     *
     * @param pattern regex pattern to match the plugin jar filename
     * @return true if a matching jar exists
     */
    private static boolean pluginJarExists(Pattern pattern) {
        java.io.File pluginsFolder = Bukkit.getPluginsFolder();
        if (pluginsFolder == null || !pluginsFolder.exists()) {
            return false;
        }

        java.io.File[] files = pluginsFolder.listFiles((dir, name) ->
            pattern.matcher(name).matches()
        );

        return files != null && files.length > 0;
    }
}
