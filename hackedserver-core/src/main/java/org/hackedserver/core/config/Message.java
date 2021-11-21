package org.hackedserver.core.config;


import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.jetbrains.annotations.NotNull;
import org.tomlj.TomlParseResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Message {

    PREFIX("general.prefix"),
    PLUGIN_LOADED("logs.loaded"),
    COMMANDS_HELP("commands.help"),
    COMMANDS_RELOAD_SUCCESS("commands.reload_success"),
    CHECK_NO_MODS("commands.check_no_mods"),
    CHECK_MODS("commands.check_mods"),
    MOD_LIST_FORMAT("commands.mod_list_format");

    private static TomlParseResult result;

    public static void setParseResult(TomlParseResult newResult) {
        result = newResult;
    }

    private final String path;

    Message(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String toString() {
        return result.getString(path);
    }

    public @NotNull
    final Component toComponent(final Template... placeholders) {
        List<Template> outputPlaceholders = new ArrayList<>(Arrays.asList(placeholders));
        if (this != PREFIX)
            outputPlaceholders.add(Template.of("prefix", Message.PREFIX.toComponent()));
        return MiniMessage.get().parse(toString(), outputPlaceholders.toArray());
    }

    public static Component parse(String message, Template[] placeholders) {
        List<Template> outputPlaceholders = new ArrayList<>(Arrays.asList(placeholders));
        outputPlaceholders.add(Template.of("prefix", Message.PREFIX.toComponent()));
        return MiniMessage.get().parse(message,
                outputPlaceholders);
    }

    public void send(Audience audience) {
        audience.sendMessage(toComponent());
    }

    public void send(Audience audience, final Template... placeholders) {
        audience.sendMessage(toComponent(placeholders));
    }

}