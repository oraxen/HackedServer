package org.hackedserver.velocity.logs;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Logs {

    private static Logger LOGGER;
    private static ProxyServer SERVER;

    public static void onEnable(Logger logger, ProxyServer server) {
        LOGGER = logger;
        SERVER = server;
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
        SERVER.sendMessage(message);
    }

}
