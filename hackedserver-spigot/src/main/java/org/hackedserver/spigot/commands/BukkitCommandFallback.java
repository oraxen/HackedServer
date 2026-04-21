package org.hackedserver.spigot.commands;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.LunarConfig;
import org.hackedserver.core.config.Message;
import org.hackedserver.core.forge.ForgeConfig;
import org.hackedserver.core.forge.ForgeModInfo;
import org.hackedserver.core.lunar.LunarModInfo;
import org.hackedserver.spigot.utils.HackedInventoryView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fallback Bukkit command handler used when CommandAPI is not available.
 */
public class BukkitCommandFallback implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "check", "list", "inv");

    private final JavaPlugin plugin;
    private final BukkitAudiences audiences;

    public BukkitCommandFallback(JavaPlugin plugin, BukkitAudiences audiences) {
        this.plugin = plugin;
        this.audiences = audiences;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hackedserver.command")) {
            return true;
        }

        if (args.length == 0) {
            Message.COMMANDS_HELP_SPIGOT.send(audiences.sender(sender));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "check" -> handleCheck(sender, args);
            case "list" -> handleList(sender);
            case "inv" -> handleInv(sender);
            default -> Message.COMMANDS_HELP_SPIGOT.send(audiences.sender(sender));
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("hackedserver.command.reload")) return;
        ConfigsManager.reload(plugin.getDataFolder());
        Bukkit.getOnlinePlayers().forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
        Message.COMMANDS_RELOAD_SUCCESS.send(audiences.sender(sender));
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hackedserver.command.check")) return;
        if (args.length < 2) {
            audiences.sender(sender).sendMessage(
                    net.kyori.adventure.text.Component.text("Usage: /hackedserver check <player>"));
            return;
        }
        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            audiences.sender(sender).sendMessage(
                    net.kyori.adventure.text.Component.text("Player not found: " + args[1]));
            return;
        }

        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
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
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("hackedserver.command.list")) return;
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
                            java.util.Objects.requireNonNullElse(
                                    Bukkit.getOfflinePlayer(hackedPlayer.getUuid()).getName(),
                                    hackedPlayer.getUuid().toString())));
        });
    }

    private void handleInv(CommandSender sender) {
        if (!sender.hasPermission("hackedserver.command.inv")) return;
        if (!(sender instanceof Player player)) {
            audiences.sender(sender).sendMessage(
                    net.kyori.adventure.text.Component.text("This command can only be used by players."));
            return;
        }
        HackedInventoryView.openInvPage(player, 0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("hackedserver.command")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .filter(sub -> sender.hasPermission("hackedserver.command." + sub))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && "check".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("hackedserver.command.check")) return Collections.emptyList();
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
