package org.hackedserver.spigot.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.Message;
import org.hackedserver.core.config.LunarConfig;
import org.hackedserver.core.forge.ForgeConfig;
import org.hackedserver.core.forge.ForgeModInfo;
import org.hackedserver.core.lunar.LunarModInfo;
import org.hackedserver.spigot.utils.HackedInventoryView;

import java.util.Objects;
import java.util.stream.Collectors;

public class CommandsManager {

    private final JavaPlugin plugin;
    private final BukkitAudiences audiences;

    public CommandsManager(JavaPlugin plugin, BukkitAudiences audiences) {
        this.plugin = plugin;
        this.audiences = audiences;
    }

    public void loadCommands() {
        new CommandAPICommand("hackedserver")
                .withAliases("hs")
                .withPermission("hackedserver.command")
                .withSubcommands(getReloadCommand(), getCheckCommand(), getListCommand(), getInvCommand())
                .executes((sender, args) -> {
                    Message.COMMANDS_HELP_SPIGOT.send(audiences.sender(sender));
                }).register();
    }

    private CommandAPICommand getReloadCommand() {
        return new CommandAPICommand("reload")
                .withPermission("hackedserver.command.reload")
                .executes((sender, args) -> {
                    ConfigsManager.reload(plugin.getDataFolder());
                    Bukkit.getOnlinePlayers().forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
                    Message.COMMANDS_RELOAD_SUCCESS.send(audiences.sender(sender));
                });
    }

    private CommandAPICommand getCheckCommand() {
        return new CommandAPICommand("check")
                .withPermission("hackedserver.command.check")
                .withArguments(new EntitySelectorArgument.OnePlayer("player"))
                .executes((sender, args) -> {
                    HackedPlayer hackedPlayer = HackedServer.getPlayer(((Player) args.get("player")).getUniqueId());
                    boolean hasGenericChecks = !hackedPlayer.getGenericChecks().isEmpty();
                    boolean showLunarMods = LunarConfig.isEnabled()
                            && LunarConfig.shouldShowModsInCheck()
                            && hackedPlayer.hasLunarModsData();
                    boolean hasLunarMods = showLunarMods && !hackedPlayer.getLunarMods().isEmpty();

                    if (hasGenericChecks) {
                        Message.CHECK_MODS.send(audiences.sender(sender));
                        hackedPlayer.getGenericChecks().forEach(checkId -> {
                            var check = HackedServer.getCheck(checkId);
                            String modName = check != null ? check.getName() : checkId;
                            Message.MOD_LIST_FORMAT.send(audiences.sender(sender),
                                    Placeholder.parsed("mod", modName));
                        });
                    } else if (!showLunarMods) {
                        Message.CHECK_NO_MODS.send(audiences.sender(sender));
                    }

                    if (showLunarMods) {
                        if (hasLunarMods) {
                            Message.CHECK_LUNAR_MODS.send(audiences.sender(sender));
                            for (LunarModInfo mod : hackedPlayer.getLunarMods()) {
                                Message.LUNAR_MOD_LIST_FORMAT.send(audiences.sender(sender),
                                        Placeholder.parsed("mod", LunarConfig.formatMod(mod)));
                            }
                        } else {
                            Message.CHECK_LUNAR_NO_MODS.send(audiences.sender(sender));
                        }
                    }

                    // Display Forge mods
                    boolean showForgeMods = ForgeConfig.isEnabled()
                            && ForgeConfig.shouldShowModsInCheck()
                            && hackedPlayer.hasForgeModsData();
                    boolean hasForgeMods = showForgeMods && !hackedPlayer.getForgeMods().isEmpty();

                    if (showForgeMods) {
                        if (hasForgeMods) {
                            Message.CHECK_FORGE_MODS.send(audiences.sender(sender));
                            for (ForgeModInfo mod : hackedPlayer.getForgeMods()) {
                                Message.FORGE_MOD_LIST_FORMAT.send(audiences.sender(sender),
                                        Placeholder.parsed("mod", ForgeConfig.formatMod(mod)));
                            }
                        } else {
                            Message.CHECK_FORGE_NO_MODS.send(audiences.sender(sender));
                        }
                    }
                });
    }

    private CommandAPICommand getListCommand() {
        return new CommandAPICommand("list")
                .withPermission("hackedserver.command.list")
                .executes((sender, args) -> {
                    var playersWithChecks = HackedServer.getPlayers().stream()
                            .filter(player -> !player.getGenericChecks().isEmpty())
                            .collect(Collectors.toList());

                    if (playersWithChecks.isEmpty()) {
                        Message.CHECK_PLAYERS_EMPTY.send(audiences.sender(sender));
                        return;
                    }

                    Message.CHECK_PLAYERS.send(audiences.sender(sender));
                    playersWithChecks.forEach(hackedPlayer -> {
                        Message.PLAYER_LIST_FORMAT.send(audiences.sender(sender),
                                Placeholder.parsed("player",
                                        Objects.requireNonNullElse(
                                                Bukkit.getOfflinePlayer(hackedPlayer.getUuid()).getName(),
                                                hackedPlayer.getUuid().toString())));
                    });
                });
    }

    private CommandAPICommand getInvCommand() {
        return new CommandAPICommand("inv")
                .withPermission("hackedserver.command.inv")
                .executesPlayer((player, args) -> {
                    openInvPage(player, 0);
                });
    }

    public static void openInvPage(Player viewer, int page) {
        HackedInventoryView.openInvPage(viewer, page);
    }

}
