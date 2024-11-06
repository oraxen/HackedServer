package org.hackedserver.bungee.logs;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ProxyServer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logs {

    private static Logger LOGGER;
    private static BungeeAudiences AUDIENCES;

    public static void onEnable(Logger logger, BungeeAudiences audiences) {
        LOGGER = logger;
        AUDIENCES = audiences;
    }

    public static void logInfo(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void logError(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    public static void logWarning(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void logComponent(Component message) {
        AUDIENCES.sender(ProxyServer.getInstance().getConsole()).sendMessage(message);
    }

}
