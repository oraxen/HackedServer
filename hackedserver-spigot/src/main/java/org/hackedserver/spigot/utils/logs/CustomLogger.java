package org.hackedserver.spigot.utils.logs;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class CustomLogger extends PluginLogger {

    // The server's own logger for this plugin. It already names the plugin in
    // every line ("[HackedServer] ..."), so records are forwarded to it as is.
    private final Logger delegate;

    CustomLogger(Plugin context, Logger delegate) {
        super(context);
        this.delegate = delegate;
    }

    @Override
    public void log(LogRecord logRecord) {
        if (logRecord != null && logRecord.getLevel() != Level.INFO) {
            forward(logRecord);
        }
    }

    void newLog(Level level, String message) {
        forward(new LogRecord(level, message));
    }

    private void forward(LogRecord logRecord) {
        logRecord.setLoggerName(delegate.getName());
        delegate.log(logRecord);
    }

}
