package org.hackedserver.velocity.logs;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class Logs {

    private static Logger LOGGER;
    private static ProxyServer SERVER;

    public static void onEnable(Logger logger, ProxyServer server) {
        LOGGER = logger;
        SERVER = server;
    }

    public static void logInfo(String message) {
        LOGGER.info(message);
    }

    public static void logError(String message) {
        LOGGER.error(message);
    }

    public static void logWarning(String message) {
        LOGGER.warn(message);
    }

    public static void logComponent(Component message) {
        SERVER.getConsoleCommandSource().sendMessage(message);
    }

}
