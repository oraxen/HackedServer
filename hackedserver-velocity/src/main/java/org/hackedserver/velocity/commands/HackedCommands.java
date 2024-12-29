package org.hackedserver.velocity.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.Message;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.io.File;
import org.slf4j.Logger;
import java.util.stream.Collectors;

public class HackedCommands {

    private final Logger logger;
    private final File dataFolder;
    private final CommandManager commandManager;
    private final ProxyServer server;

    public HackedCommands(Logger logger, File dataFolder, CommandManager commandManager, ProxyServer server) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.commandManager = commandManager;
        this.server = server;
    }

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> hackedServerNode = LiteralArgumentBuilder
                .<CommandSource>literal("hackedserver")
                .executes(context -> {
                    Message.COMMANDS_HELP.send(context.getSource());
                    return 1;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .executes(context -> {
                            ConfigsManager.reload(dataFolder);
                            server.getAllPlayers()
                                    .forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
                            Message.COMMANDS_RELOAD_SUCCESS.send(context.getSource());
                            return 1;
                        }))
                .then(LiteralArgumentBuilder.<CommandSource>literal("check")
                        .requires(source -> source.hasPermission("hackedserver.check"))
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("player", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    server.getAllPlayers().forEach(player -> builder.suggest(player.getUsername()));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String playerName = context.getArgument("player", String.class);
                                    Player player = server.getPlayer(playerName).orElse(null);
                                    // todo: check if player is null and emit an error
                                    HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
                                    if (hackedPlayer.getGenericChecks().isEmpty()) {
                                        Message.CHECK_NO_MODS.send(context.getSource());
                                    } else {
                                        Message.CHECK_MODS.send(context.getSource());
                                        for (String checkId : hackedPlayer.getGenericChecks()) {
                                            Message.MOD_LIST_FORMAT.send(context.getSource(),
                                                    Placeholder.parsed("mod",
                                                            HackedServer.getCheck(checkId).getName()));
                                        }
                                    }
                                    return 1;
                                })))
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(context -> {
                            var playersWithChecks = HackedServer.getPlayers().stream()
                                    .filter(player -> !player.getGenericChecks().isEmpty())
                                    .collect(Collectors.toList());

                            if (playersWithChecks.isEmpty()) {
                                Message.CHECK_PLAYERS_EMPTY.send(context.getSource());
                                return 0;
                            }

                            Message.CHECK_PLAYERS.send(context.getSource());
                            playersWithChecks.forEach(hackedPlayer -> {
                                Message.PLAYER_LIST_FORMAT.send(context.getSource(),
                                        Placeholder.parsed("player",
                                                server.getPlayer(hackedPlayer.getUuid())
                                                        .map(Player::getUsername)
                                                        .orElse("Unknown")));
                            });
                            return 0;
                        }))
                .build();

        return new BrigadierCommand(hackedServerNode);
    }

    public void create() {
        commandManager.register("hs", createBrigadierCommand());
    }

}
