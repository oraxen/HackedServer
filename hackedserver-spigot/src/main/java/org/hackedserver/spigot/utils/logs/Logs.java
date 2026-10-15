package org.hackedserver.spigot.utils.logs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.spigot.utils.HackedAudiences;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class Logs {

    private static CustomLogger LOGGER;
    private static HackedAudiences audiences;

    public static void enableFilter(JavaPlugin plugin) throws NoSuchFieldException, IllegalAccessException {
        Field field = JavaPlugin.class.getDeclaredField("logger");
        field.setAccessible(true);
        LOGGER = new CustomLogger(plugin);
        field.set(plugin, LOGGER);
    }

    public static void onEnable(HackedAudiences audiences) {
        Logs.audiences = audiences;
    }

    public static void logInfo(String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    /**
     * Logs an important lifecycle message (e.g. which packet library is used).
     * Unlike regular INFO records, these are not dropped by {@link CustomLogger}.
     */
    public static void logStatus(String message) {
        LOGGER.newLog(Level.INFO, message);
    }

    public static void logError(String message) {
        LOGGER.newLog(Level.SEVERE, message);
    }

    public static void logWarning(String message) {
        LOGGER.newLog(Level.WARNING, message);
    }

    public static void logComponent(Component message) {
        if (audiences == null) {
            // Not enabled yet (or already disabled): degrade to a legacy formatted console line
            Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
            return;
        }
        audiences.console().sendMessage(message);
    }

    public static CustomLogger getLogger() {
        return LOGGER;
    }

}
