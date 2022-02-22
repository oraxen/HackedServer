package org.hackedserver.core.config;


import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
    MOD_LIST_FORMAT("commands.mod_list_format"),
    CHECK_PLAYERS("commands.check_players"),
    PLAYER_LIST_FORMAT("commands.player_list_format");

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
    final Component toComponent(final TagResolver.Single... placeholders) {
        List<TagResolver.Single> outputPlaceholders = new ArrayList<>(Arrays.asList(placeholders));
        if (this != PREFIX)
            outputPlaceholders.add(Placeholder.component("prefix", Message.PREFIX.toComponent()));
        return MiniMessage.miniMessage().deserialize(toString(), TagResolver.resolver(outputPlaceholders));
    }

    public static Component parse(String message, TagResolver.Single... placeholders) {
        List<TagResolver.Single> outputPlaceholders = new ArrayList<>(Arrays.asList(placeholders));
        outputPlaceholders.add(Placeholder.component("prefix", Message.PREFIX.toComponent()));
        return MiniMessage.miniMessage().deserialize(message,
                TagResolver.resolver(outputPlaceholders));
    }

    public void send(Audience audience) {
        audience.sendMessage(toComponent());
    }

    public void send(Audience audience, final TagResolver.Single... placeholders) {
        audience.sendMessage(toComponent(placeholders));
    }

}