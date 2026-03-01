package org.hackedserver.spigot.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.LunarConfig;
import org.hackedserver.core.config.GenericCheck;
import org.hackedserver.core.config.Message;
import org.hackedserver.core.forge.ForgeConfig;
import org.hackedserver.core.forge.ForgeModInfo;
import org.hackedserver.core.lunar.LunarModInfo;
import org.hackedserver.spigot.HackedHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommandsManager {

    private static final int ITEMS_PER_PAGE = 45;
    private static final int INV_SIZE = 54;
    private static final int NAV_PREV_SLOT = 45;
    private static final int NAV_INFO_SLOT = 49;
    private static final int NAV_NEXT_SLOT = 53;

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
                                        Objects.requireNonNull(
                                                Bukkit.getOfflinePlayer(hackedPlayer.getUuid()).getName())));
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
        List<HackedPlayer> players = HackedServer.getPlayers().stream()
                .sorted(Comparator.comparing(p -> {
                    String name = Bukkit.getOfflinePlayer(p.getUuid()).getName();
                    return name != null ? name.toLowerCase() : "";
                }))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) players.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        HackedHolder holder = new HackedHolder(page);
        Inventory inv = Bukkit.createInventory(holder, INV_SIZE, "HackedServer");
        holder.setInventory(inv);

        // Get all checks for categorization
        List<GenericCheck> loaderChecks = HackedServer.getChecks().stream()
                .filter(c -> "loader".equals(c.getCategory()))
                .sorted(Comparator.comparing(GenericCheck::getName))
                .collect(Collectors.toList());

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, players.size());

        for (int i = start; i < end; i++) {
            HackedPlayer hackedPlayer = players.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            assert meta != null;
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(hackedPlayer.getUuid()));

            String playerName = Bukkit.getOfflinePlayer(hackedPlayer.getUuid()).getName();
            if (playerName != null) {
                meta.setDisplayName(toLegacy(Component.text(playerName, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)));
            }

            List<String> lore = new ArrayList<>();

            // Loader section — show all loader checks with ✓/✗
            for (GenericCheck loader : loaderChecks) {
                boolean detected = hackedPlayer.getGenericChecks().contains(loader.getId());
                if (detected) {
                    lore.add(toLegacy(Component.text("✓ " + loader.getName() + " detected", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false)));
                } else {
                    lore.add(toLegacy(Component.text("✗ " + loader.getName() + " not detected", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
                }
            }

            // Separator
            lore.add(toLegacy(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)));

            // Non-loader detected checks
            List<GenericCheck> detectedNonLoader = HackedServer.getChecks().stream()
                    .filter(check -> hackedPlayer.getGenericChecks().contains(check.getId()))
                    .filter(check -> !"loader".equals(check.getCategory()))
                    .sorted(Comparator.comparing(GenericCheck::getName))
                    .collect(Collectors.toList());

            int totalNonLoader = (int) HackedServer.getChecks().stream()
                    .filter(check -> !"loader".equals(check.getCategory()))
                    .count();
            int cleanCount = totalNonLoader - detectedNonLoader.size();

            if (detectedNonLoader.isEmpty()) {
                lore.add(toLegacy(Component.text("✓ " + cleanCount + " checks passed", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)));
            } else {
                for (GenericCheck check : detectedNonLoader) {
                    lore.add(toLegacy(Component.text("⚠ " + check.getName(), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)));
                }
                lore.add(toLegacy(Component.text("")));
                lore.add(toLegacy(Component.text("✓ " + cleanCount + " other checks passed", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)));
            }

            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.setItem(i - start, head);
        }

        // Navigation bar (bottom row)
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            assert prevMeta != null;
            prevMeta.setDisplayName(toLegacy(Component.text("← Previous Page", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)));
            prev.setItemMeta(prevMeta);
            inv.setItem(NAV_PREV_SLOT, prev);
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        assert infoMeta != null;
        infoMeta.setDisplayName(toLegacy(Component.text("Page " + (page + 1) + "/" + totalPages, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
        List<String> infoLore = new ArrayList<>();
        infoLore.add(toLegacy(Component.text(players.size() + " players tracked", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(NAV_INFO_SLOT, info);

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            assert nextMeta != null;
            nextMeta.setDisplayName(toLegacy(Component.text("Next Page →", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)));
            next.setItemMeta(nextMeta);
            inv.setItem(NAV_NEXT_SLOT, next);
        }

        viewer.openInventory(inv);
    }

    private static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

}
