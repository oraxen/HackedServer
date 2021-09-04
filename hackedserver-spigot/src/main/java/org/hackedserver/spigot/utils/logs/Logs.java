package org.hackedserver.spigot.utils.logs;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class Logs {

    private static CustomLogger LOGGER;
    private static BukkitAudiences audiences;
    private static JavaPlugin plugin;

    public static void enableFilter(JavaPlugin currentPlugin) throws NoSuchFieldException, IllegalAccessException {
        plugin = currentPlugin;
        Field field = JavaPlugin.class.getDeclaredField("logger");
        field.setAccessible(true);
        LOGGER = new CustomLogger(plugin);
        field.set(plugin, LOGGER);
    }

    public static void onEnable(BukkitAudiences audiences) {
        Logs.audiences = audiences;
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

    public static void logComponent(Component message) {
        audiences.sender(Bukkit.getConsoleSender()).sendMessage(message);
    }

    public static CustomLogger getLogger() {
        return LOGGER;
    }

}
