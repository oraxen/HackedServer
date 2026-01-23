package org.hackedserver.core.bedrock;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Detects Bedrock players using Geyser and/or Floodgate APIs.
 * Uses class isolation to avoid loading API classes when they're not present.
 */
public final class BedrockDetector {

    private static final String GEYSER_API_CLASS = "org.geysermc.geyser.api.GeyserApi";
    private static final String FLOODGATE_API_CLASS = "org.geysermc.floodgate.api.FloodgateApi";

    private static volatile BedrockProvider geyserProvider;
    private static volatile BedrockProvider floodgateProvider;
    private static volatile boolean initialized = false;

    private BedrockDetector() {
    }

    /**
     * Initialize the detector, loading available providers.
     * Safe to call multiple times (no-op after first call).
     *
     * @param logger optional logger for debug output
     */
    public static void initialize(Logger logger) {
        if (initialized) {
            return;
        }
        synchronized (BedrockDetector.class) {
            if (initialized) {
                return;
            }

            if (isClassPresent(GEYSER_API_CLASS)) {
                try {
                    geyserProvider = new GeyserProvider();
                    if (logger != null) {
                        logger.info("Geyser API detected - bedrock detection via Geyser enabled");
                    }
                } catch (Throwable t) {
                    if (logger != null) {
                        logger.warning("Failed to initialize Geyser provider: " + t.getMessage());
                    }
                }
            }

            if (isClassPresent(FLOODGATE_API_CLASS)) {
                try {
                    floodgateProvider = new FloodgateProvider();
                    if (logger != null) {
                        logger.info("Floodgate API detected - bedrock detection via Floodgate enabled");
                    }
                } catch (Throwable t) {
                    if (logger != null) {
                        logger.warning("Failed to initialize Floodgate provider: " + t.getMessage());
                    }
                }
            }

            initialized = true;
        }
    }

    /**
     * Check if any bedrock detection APIs are available.
     *
     * @return true if at least one API is available
     */
    public static boolean isAvailable() {
        if (!initialized) {
            initialize(null);
        }
        return geyserProvider != null || floodgateProvider != null;
    }

    /**
     * Detect if the player is a Bedrock player.
     *
     * @param uuid the player's UUID
     * @return the source name ("Geyser" or "Floodgate") if bedrock, null otherwise
     */
    public static String detectSource(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (!initialized) {
            initialize(null);
        }

        // Check Geyser first (more specific)
        if (geyserProvider != null && geyserProvider.isBedrockPlayer(uuid)) {
            return geyserProvider.getSourceName();
        }

        // Fall back to Floodgate
        if (floodgateProvider != null && floodgateProvider.isBedrockPlayer(uuid)) {
            return floodgateProvider.getSourceName();
        }

        return null;
    }

    /**
     * Check if a player is a Bedrock player.
     *
     * @param uuid the player's UUID
     * @return true if the player is a Bedrock player
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        return detectSource(uuid) != null;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Reset the detector state (for testing or reload).
     */
    public static void reset() {
        synchronized (BedrockDetector.class) {
            geyserProvider = null;
            floodgateProvider = null;
            initialized = false;
        }
    }
}
