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
import org.hackedserver.core.config.LunarConfig;
import org.hackedserver.core.config.Message;
import org.hackedserver.core.forge.ForgeConfig;
import org.hackedserver.core.forge.ForgeModInfo;
import org.hackedserver.core.lunar.LunarModInfo;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.io.File;
import java.util.stream.Collectors;

public class HackedCommands {

    private final File dataFolder;
    private final CommandManager commandManager;
    private final ProxyServer server;

    public HackedCommands(File dataFolder, CommandManager commandManager, ProxyServer server) {
        this.dataFolder = dataFolder;
        this.commandManager = commandManager;
        this.server = server;
    }

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> hackedServerNode = LiteralArgumentBuilder
                .<CommandSource>literal("hackedserver")
                .requires(source -> source.hasPermission("hackedserver.command"))
                .executes(context -> {
                    Message.COMMANDS_HELP.send(context.getSource());
                    return 1;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(source -> source.hasPermission("hackedserver.command.reload"))
                        .executes(context -> {
                            ConfigsManager.reload(dataFolder);
                            server.getAllPlayers()
                                    .forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
                            Message.COMMANDS_RELOAD_SUCCESS.send(context.getSource());
                            return 1;
                        }))
                .then(LiteralArgumentBuilder.<CommandSource>literal("check")
                        .requires(source -> source.hasPermission("hackedserver.command.check"))
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("player", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    server.getAllPlayers().forEach(player -> builder.suggest(player.getUsername()));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String playerName = context.getArgument("player", String.class);
                                    Player player = server.getPlayer(playerName).orElse(null);
                                    if (player == null) {
                                        context.getSource().sendMessage(
                                                net.kyori.adventure.text.Component.text("Player not found: " + playerName));
                                        return 0;
                                    }
                                    HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
                                    boolean hasGenericChecks = !hackedPlayer.getGenericChecks().isEmpty();
                                    boolean showLunarMods = LunarConfig.isEnabled()
                                            && LunarConfig.shouldShowModsInCheck()
                                            && hackedPlayer.hasLunarModsData();
                                    boolean hasLunarMods = showLunarMods && !hackedPlayer.getLunarMods().isEmpty();

                                    if (hasGenericChecks) {
                                        Message.CHECK_MODS.send(context.getSource());
                                        for (String checkId : hackedPlayer.getGenericChecks()) {
                                            var check = HackedServer.getCheck(checkId);
                                            String modName = check != null ? check.getName() : checkId;
                                            Message.MOD_LIST_FORMAT.send(context.getSource(),
                                                    Placeholder.parsed("mod", modName));
                                        }
                                    } else if (!showLunarMods) {
                                        Message.CHECK_NO_MODS.send(context.getSource());
                                    }

                                    if (showLunarMods) {
                                        if (hasLunarMods) {
                                            Message.CHECK_LUNAR_MODS.send(context.getSource());
                                            for (LunarModInfo mod : hackedPlayer.getLunarMods()) {
                                                Message.LUNAR_MOD_LIST_FORMAT.send(context.getSource(),
                                                        Placeholder.parsed("mod", LunarConfig.formatMod(mod)));
                                            }
                                        } else {
                                            Message.CHECK_LUNAR_NO_MODS.send(context.getSource());
                                        }
                                    }

                                    // Display Forge mods
                                    boolean showForgeMods = ForgeConfig.isEnabled()
                                            && ForgeConfig.shouldShowModsInCheck()
                                            && hackedPlayer.hasForgeModsData();
                                    boolean hasForgeMods = showForgeMods && !hackedPlayer.getForgeMods().isEmpty();

                                    if (showForgeMods) {
                                        if (hasForgeMods) {
                                            Message.CHECK_FORGE_MODS.send(context.getSource());
                                            for (ForgeModInfo mod : hackedPlayer.getForgeMods()) {
                                                Message.FORGE_MOD_LIST_FORMAT.send(context.getSource(),
                                                        Placeholder.parsed("mod", ForgeConfig.formatMod(mod)));
                                            }
                                        } else {
                                            Message.CHECK_FORGE_NO_MODS.send(context.getSource());
                                        }
                                    }
                                    return 1;
                                })))
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .requires(source -> source.hasPermission("hackedserver.command.list"))
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
