package org.hackedserver.spigot.utils.logs;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class Logs {

    private static CustomLogger LOGGER;

    public static void enableFilter(JavaPlugin plugin) throws NoSuchFieldException, IllegalAccessException {
        Field field = JavaPlugin.class.getDeclaredField("logger");
        field.setAccessible(true);
        LOGGER = new CustomLogger(plugin);
        field.set(plugin, LOGGER);
    }

    public static void logInfo(String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public static void logError(String message) {
        LOGGER.newLog(Level.SEVERE, message);
    }

    public static void logWarning(String message) {
        LOGGER.newLog(Level.WARNING, message);
    }

}
