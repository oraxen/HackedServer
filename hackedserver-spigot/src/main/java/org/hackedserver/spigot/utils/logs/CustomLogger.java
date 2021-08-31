package org.hackedserver.spigot.utils.logs;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;

import java.util.logging.Level;
import java.util.logging.LogRecord;

public class CustomLogger extends PluginLogger {

    CustomLogger(Plugin context) {
        super(context);
    }

    @Override
    public void log(LogRecord logRecord) {
        if (logRecord != null && logRecord.getLevel() != Level.INFO) {
            try {
                logRecord.setMessage("HackedServer |" + " " + logRecord.getMessage());
            } catch (NullPointerException exception) {
                logRecord.setMessage("HackedServer | " + logRecord.getMessage());
            }
            super.log(logRecord);
        }
    }

    void newLog(Level level, String message) {
        super.log(new LogRecord(level, message));
    }

}
