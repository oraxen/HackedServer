package org.hackedserver.spigot.utils;

import org.bukkit.Bukkit;
import org.hackedserver.spigot.utils.logs.Logs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VersionUtil {

    private static final boolean IS_PAPER;
    private static final boolean IS_FOLIA;
    private static final int MINECRAFT_MAJOR;

    static {
        IS_PAPER = hasClass("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent")
                || hasClass("io.papermc.paper.threadedregions.RegionizedServer");
        IS_FOLIA = hasClass("io.papermc.paper.threadedregions.RegionizedServer");
        MINECRAFT_MAJOR = Bukkit.getServer() != null ? parseMajorVersion(Bukkit.getBukkitVersion()) : -1;
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

    /**
     * Minecraft switched to year-based versions after 1.21.11 (26.1, 26.2, 26.3...).
     */
    public static boolean isMinecraft26OrNewer() {
        return MINECRAFT_MAJOR >= 26;
    }

    /**
     * Parses the leading number of a Bukkit version such as {@code 1.21.11-R0.1-SNAPSHOT} (1)
     * or {@code 26.3-R0.1-SNAPSHOT} (26). Returns -1 when it cannot be parsed.
     */
    static int parseMajorVersion(@Nullable String bukkitVersion) {
        if (bukkitVersion == null) {
            return -1;
        }
        int end = 0;
        while (end < bukkitVersion.length() && Character.isDigit(bukkitVersion.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(bukkitVersion.substring(0, end));
        } catch (NumberFormatException e) {
            return -1;
        }
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
