package org.hackedserver.spigot.utils;

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
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.GenericCheck;
import org.hackedserver.spigot.HackedHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class HackedInventoryView {

    private static final int ITEMS_PER_PAGE = 45;
    private static final int INV_SIZE = 54;
    private static final int NAV_PREV_SLOT = 45;
    private static final int NAV_INFO_SLOT = 49;
    private static final int NAV_NEXT_SLOT = 53;

    private HackedInventoryView() {
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

            lore.add(toLegacy(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)));

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
