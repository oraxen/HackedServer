package org.hackedserver.spigot.utils;

import dev.jorel.commandapi.CommandAPIConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;

public final class BukkitWrapper {

    private BukkitWrapper() {
    }

    public static CommandAPIConfig<?> createCommandApiConfig(JavaPlugin plugin) {
        // Prefer Paper config on Paper/Folia, otherwise fallback to Spigot config
        final String paperConfigClass = "dev.jorel.commandapi.CommandAPIPaperConfig";
        final String spigotConfigClass = "dev.jorel.commandapi.CommandAPISpigotConfig";

        if (VersionUtil.isPaperServer()) {
            CommandAPIConfig<?> paper = tryConstruct(paperConfigClass, plugin);
            if (paper != null)
                return paper.silentLogs(true);
        }

        CommandAPIConfig<?> spigot = tryConstruct(spigotConfigClass, plugin);
        if (spigot != null)
            return spigot.silentLogs(true);

        throw new IllegalStateException(
                "Neither CommandAPIPaperConfig nor CommandAPISpigotConfig are available on the classpath");
    }

    private static CommandAPIConfig<?> tryConstruct(String className, JavaPlugin plugin) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> ctor = clazz.getConstructor(JavaPlugin.class);
            Object instance = ctor.newInstance(plugin);
            return (CommandAPIConfig<?>) instance;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
