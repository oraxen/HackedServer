package org.hackedserver.spigot.utils.logs;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import java.util.function.BiConsumer;

public final class ConsoleAdapter implements BiConsumer<Boolean, String> {

    public static final ConsoleAdapter INSTANCE = new ConsoleAdapter();

    private final ConsoleCommandSender sender = Bukkit.getConsoleSender();

    private ConsoleAdapter() {
    }

    /*
     *
     */

    @Override
    public void accept(Boolean flag, String message) {
        send(message);
    }

    public ConsoleAdapter send(String message) {
        sender.sendMessage("HackedServer | " + message);
        return this;
    }

}
