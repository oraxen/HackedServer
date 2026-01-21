package org.hackedserver.spigot.utils;

import org.bukkit.entity.Player;
import org.hackedserver.core.config.BedrockConfig;

import java.lang.reflect.Method;
import java.util.UUID;

public final class BedrockDetector {

    private static volatile boolean geyserChecked;
    private static volatile Method geyserApiMethod;
    private static volatile Method geyserIsBedrockMethod;

    private static volatile boolean floodgateChecked;
    private static volatile Method floodgateGetInstanceMethod;
    private static volatile Method floodgateIsBedrockMethod;

    private BedrockDetector() {
    }

    public static String detectSource(Player player) {
        if (player == null) {
            return null;
        }
        UUID uuid = player.getUniqueId();
        if (uuid == null) {
            return null;
        }

        if (BedrockConfig.useGeyserApi() && isGeyserBedrock(uuid)) {
            return "Geyser";
        }
        if (BedrockConfig.useFloodgateApi() && isFloodgateBedrock(uuid)) {
            return "Floodgate";
        }
        return null;
    }

    private static boolean isGeyserBedrock(UUID uuid) {
        Method apiMethod = getGeyserApiMethod();
        Method isBedrockMethod = getGeyserIsBedrockMethod();
        if (apiMethod == null || isBedrockMethod == null) {
            return false;
        }
        try {
            Object api = apiMethod.invoke(null);
            if (api == null) {
                return false;
            }
            Object result = isBedrockMethod.invoke(api, uuid);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isFloodgateBedrock(UUID uuid) {
        Method getInstance = getFloodgateGetInstanceMethod();
        Method isBedrockMethod = getFloodgateIsBedrockMethod();
        if (getInstance == null || isBedrockMethod == null) {
            return false;
        }
        try {
            Object api = getInstance.invoke(null);
            if (api == null) {
                return false;
            }
            Object result = isBedrockMethod.invoke(api, uuid);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method getGeyserApiMethod() {
        if (!geyserChecked) {
            synchronized (BedrockDetector.class) {
                if (!geyserChecked) {
                    try {
                        Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
                        geyserApiMethod = apiClass.getMethod("api");
                        geyserIsBedrockMethod = apiClass.getMethod("isBedrockPlayer", UUID.class);
                    } catch (Throwable ignored) {
                        geyserApiMethod = null;
                        geyserIsBedrockMethod = null;
                    }
                    geyserChecked = true;
                }
            }
        }
        return geyserApiMethod;
    }

    private static Method getGeyserIsBedrockMethod() {
        if (!geyserChecked) {
            getGeyserApiMethod();
        }
        return geyserIsBedrockMethod;
    }

    private static Method getFloodgateGetInstanceMethod() {
        if (!floodgateChecked) {
            synchronized (BedrockDetector.class) {
                if (!floodgateChecked) {
                    try {
                        Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                        floodgateGetInstanceMethod = apiClass.getMethod("getInstance");
                        floodgateIsBedrockMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
                    } catch (Throwable ignored) {
                        floodgateGetInstanceMethod = null;
                        floodgateIsBedrockMethod = null;
                    }
                    floodgateChecked = true;
                }
            }
        }
        return floodgateGetInstanceMethod;
    }

    private static Method getFloodgateIsBedrockMethod() {
        if (!floodgateChecked) {
            getFloodgateGetInstanceMethod();
        }
        return floodgateIsBedrockMethod;
    }
}
