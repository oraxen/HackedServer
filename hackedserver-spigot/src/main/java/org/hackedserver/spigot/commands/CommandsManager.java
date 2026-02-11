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
                    Inventory inv = Bukkit.createInventory(new HackedHolder(player), 9, "HackedServer");
                    HackedServer.getPlayers().forEach(hackedPlayer -> {
                        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) head.getItemMeta();
                        assert meta != null;
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(hackedPlayer.getUuid()));
                        meta.setDisplayName(Bukkit.getOfflinePlayer(hackedPlayer.getUuid()).getName());

                        List<String> lore = new ArrayList<>();

                        // Get all checks and categorize them
                        List<GenericCheck> allChecks = new ArrayList<>(HackedServer.getChecks().stream()
                                .sorted(Comparator.comparing(GenericCheck::getName)).toList());

                        // Separate detected checks from all checks
                        List<GenericCheck> detectedChecks = allChecks.stream()
                                .filter(check -> hackedPlayer.getGenericChecks().contains(check.getId()))
                                .collect(Collectors.toList());

                        int detectedCount = detectedChecks.size();
                        int totalChecks = allChecks.size();
                        int cleanCount = totalChecks - detectedCount;

                        // Add separator line
                        lore.add(toLegacy(Component.text("━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)));

                        if (detectedCount == 0) {
                            // Clean player - show positive message
                            lore.add(toLegacy(Component.text("✓ CLEAN", NamedTextColor.GREEN)));
                            lore.add(toLegacy(Component.text("No mods detected", NamedTextColor.GRAY)));
                        } else {
                            // Modded player - show summary and detected items
                            lore.add(toLegacy(Component.text("⚠ MODDED ", NamedTextColor.GOLD)
                                    .append(Component.text("(" + detectedCount + " detected)", NamedTextColor.GRAY))));
                            lore.add(toLegacy(Component.text(""))); // Blank line

                            // Show detected items with severity colors
                            lore.add(toLegacy(Component.text("Detected:", NamedTextColor.YELLOW)));

                            for (GenericCheck check : detectedChecks) {
                                boolean isHighRisk = isHighRiskCheck(check);
                                String bullet = isHighRisk ? "  ● " : "  ○ ";
                                NamedTextColor color = isHighRisk ? NamedTextColor.RED : NamedTextColor.YELLOW;

                                lore.add(toLegacy(Component.text(bullet + check.getName(), color)));
                            }

                            lore.add(toLegacy(Component.text(""))); // Blank line

                            // Show summary of clean checks
                            lore.add(toLegacy(Component.text("✓ " + cleanCount + " other checks passed", NamedTextColor.GREEN)));
                        }

                        lore.add(toLegacy(Component.text("━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)));

                        meta.setLore(lore);
                        head.setItemMeta(meta);
                        inv.addItem(head);
                    });
                    player.openInventory(inv);
                });
    }

    /**
     * Determines if a check represents a high-risk mod/client
     * High risk includes cheat clients and suspicious mods
     */
    private static boolean isHighRiskCheck(GenericCheck check) {
        String name = check.getName().toLowerCase();
        // Consider performance/utility mods as lower risk (yellow)
        // Everything else as higher risk (red)
        return !name.contains("optifine")
                && !name.contains("journeymap")
                && !name.contains("minimap")
                && !name.contains("damage indicator")
                && !name.contains("armor status")
                && !name.contains("potion status")
                && !name.contains("fps")
                && !name.contains("schematic");
    }

    private static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

}
