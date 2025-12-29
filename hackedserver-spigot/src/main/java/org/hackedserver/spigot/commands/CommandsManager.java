package org.hackedserver.spigot.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.LunarConfig;
import org.hackedserver.core.config.GenericCheck;
import org.hackedserver.core.config.Message;
import org.hackedserver.core.lunar.LunarModInfo;
import org.hackedserver.spigot.HackedHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

                    if (!hasGenericChecks && !hasLunarMods) {
                        Message.CHECK_NO_MODS.send(audiences.sender(sender));
                    } else if (hasGenericChecks) {
                        Message.CHECK_MODS.send(audiences.sender(sender));
                        hackedPlayer.getGenericChecks().forEach(checkId -> {
                            var check = HackedServer.getCheck(checkId);
                            String modName = check != null ? check.getName() : checkId;
                            Message.MOD_LIST_FORMAT.send(audiences.sender(sender),
                                    Placeholder.parsed("mod", modName));
                        });
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
                                        Objects.requireNonNull(
                                                Bukkit.getOfflinePlayer(hackedPlayer.getUuid()).getName())));
                    });
                });
    }

    private CommandAPICommand getInvCommand() {
        return new CommandAPICommand("inv")
                .withPermission("hackedserver.command.inv")
                .executesPlayer((player, args) -> {
                    Inventory inv = Bukkit.createInventory(new HackedHolder(player), 9, "HackedServer");
                    HackedServer.getPlayers().forEach(hackedPlayer -> {
                        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) head.getItemMeta();
                        assert meta != null;
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(hackedPlayer.getUuid()));
                        meta.setDisplayName(Bukkit.getOfflinePlayer(hackedPlayer.getUuid()).getName());

                        List<String> lore = new ArrayList<>();
                        List<GenericCheck> sortedChecks = new ArrayList<>(HackedServer.getChecks().stream()
                                .sorted(Comparator.comparing(GenericCheck::getName)).toList());
                        sortedChecks.remove(HackedServer.getCheck("fabric"));
                        sortedChecks.remove(HackedServer.getCheck("forge"));

                        lore.add(toLegacy(Component.text("Fabric: ", NamedTextColor.GOLD)
                                .append(hackedPlayer.getGenericChecks().contains("fabric")
                                        ? Component.text("true", NamedTextColor.GREEN)
                                        : Component.text("false", NamedTextColor.RED))));
                        lore.add(toLegacy(Component.text("Forge: ", NamedTextColor.GOLD)
                                .append(hackedPlayer.getGenericChecks().contains("forge")
                                        ? Component.text("true", NamedTextColor.GREEN)
                                        : Component.text("false", NamedTextColor.RED))));
                        lore.add(toLegacy(Component.text("--------------------", NamedTextColor.BLUE)));

                        for (GenericCheck check : sortedChecks.stream()
                                .filter(check -> hackedPlayer.getGenericChecks().contains(check.getId())).toList()) {
                            lore.add(toLegacy(Component.text(check.getName() + ": ", NamedTextColor.GOLD)
                                    .append(Component.text("true", NamedTextColor.GREEN))));
                            sortedChecks.remove(check);
                        }

                        for (GenericCheck check : sortedChecks.stream()
                                .filter(check -> !hackedPlayer.getGenericChecks().contains(check.getId())).toList()) {
                            lore.add(toLegacy(Component.text(check.getName() + ": ", NamedTextColor.GOLD)
                                    .append(Component.text("false", NamedTextColor.RED))));
                        }
                        meta.setLore(lore);
                        head.setItemMeta(meta);
                        inv.addItem(head);
                    });
                    player.openInventory(inv);
                });
    }

    private static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

}
