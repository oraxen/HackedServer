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

import java.io.File;
import java.util.logging.Logger;
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
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("hackedserver")
                .executes(context -> {
                    try {
                        String arg = context.getArgument("type", String.class);
                        switch (arg) {
                            case "reload" -> {
                                ConfigsManager.reload(logger, dataFolder);
                                server.getAllPlayers()
                                        .forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
                                Message.COMMANDS_RELOAD_SUCCESS.send(context.getSource());
                            }
                            case "check" -> {
                                try {
                                    Player player = context.getArgument("player", Player.class);
                                    HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
                                    if (hackedPlayer.getGenericChecks().isEmpty())
                                        Message.CHECK_NO_MODS.send(context.getSource());
                                    else {
                                        Message.CHECK_MODS.send(context.getSource());
                                        for (String checkId : hackedPlayer.getGenericChecks()) {
                                            Message.MOD_LIST_FORMAT.send(context.getSource(),
                                                    Placeholder.parsed("mod",
                                                            HackedServer.getCheck(checkId).getName()));
                                        }
                                    }
                                } catch (Exception exception) {

                                }
                            }
                            case "list" -> {
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
                            }
                            default -> {
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        Message.COMMANDS_HELP.send(context.getSource());
                    }
                    return 0;
                })
                .build();

        return new BrigadierCommand(node);
    }

    public void create() {
        commandManager.register(createBrigadierCommand());
    }

}
