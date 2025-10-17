package org.hackedserver.spigot.utils;

import org.hackedserver.spigot.utils.logs.Logs;
import org.jetbrains.annotations.NotNull;

public final class VersionUtil {

    private static final boolean IS_PAPER;
    private static final boolean IS_FOLIA;

    static {
        IS_PAPER = hasClass("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent")
                || hasClass("io.papermc.paper.threadedregions.RegionizedServer");
        IS_FOLIA = hasClass("io.papermc.paper.threadedregions.RegionizedServer");
    }

    private VersionUtil() {
    }

    public enum NMSVersion {
        UNKNOWN
    }

    public static boolean isPaperServer() {
        return IS_PAPER;
    }

    public static boolean isFoliaServer() {
        return IS_FOLIA;
    }

    public static boolean isSupportedVersion(@NotNull NMSVersion serverVersion,
            @NotNull NMSVersion... supportedVersions) {
        for (NMSVersion version : supportedVersions) {
            if (version.equals(serverVersion))
                return true;
        }
        Logs.logWarning("No compatible Server version found!");
        return false;
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
