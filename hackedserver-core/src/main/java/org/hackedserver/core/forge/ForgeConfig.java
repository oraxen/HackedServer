package org.hackedserver.core.forge;

import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.jetbrains.annotations.Nullable;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for Forge/NeoForge mod detection.
 */
public final class ForgeConfig {

    private static boolean enabled = true;
    private static boolean markForge = true;
    private static boolean markNeoForge = true;
    private static boolean showModsInCheck = true;
    private static boolean showModVersions = false;
    private static List<Action> forgeActions = Collections.emptyList();
    private static List<Action> neoforgeActions = Collections.emptyList();
    private static Map<String, List<Action>> modActions = Collections.emptyMap();
    private static Set<String> whitelistedMods = Collections.emptySet();
    private static Set<String> blacklistedMods = Collections.emptySet();
    private static String whitelistedColor = "<green>";
    private static String blacklistedColor = "<red>";

    private ForgeConfig() {
    }

    public static void load(@Nullable TomlParseResult result) {
        enabled = true;
        markForge = true;
        markNeoForge = true;
        showModsInCheck = true;
        showModVersions = false;
        forgeActions = Collections.emptyList();
        neoforgeActions = Collections.emptyList();
        modActions = Collections.emptyMap();
        whitelistedMods = Collections.emptySet();
        blacklistedMods = Collections.emptySet();
        whitelistedColor = "<green>";
        blacklistedColor = "<red>";

        if (result == null) {
            return;
        }

        Boolean enabledValue = result.getBoolean("enabled");
        if (enabledValue != null) {
            enabled = enabledValue;
        }

        TomlTable settings = result.getTable("settings");
        if (settings != null) {
            markForge = getBoolean(settings, "mark_forge", markForge);
            markNeoForge = getBoolean(settings, "mark_neoforge", markNeoForge);
            showModsInCheck = getBoolean(settings, "show_mods_in_check", showModsInCheck);
            showModVersions = getBoolean(settings, "show_mod_versions", showModVersions);
        }

        TomlTable actionsTable = result.getTable("actions");
        if (actionsTable != null) {
            forgeActions = resolveActions(actionsTable.getArray("forge"));
            neoforgeActions = resolveActions(actionsTable.getArray("neoforge"));
        }

        TomlTable modActionsTable = result.getTable("mod_actions");
        if (modActionsTable != null) {
            Map<String, List<Action>> resolved = new HashMap<>();
            for (String key : modActionsTable.keySet()) {
                TomlArray array = modActionsTable.getArray(key);
                if (array == null) {
                    continue;
                }
                List<Action> actions = resolveActions(array);
                if (!actions.isEmpty()) {
                    resolved.put(normalizeModId(key), actions);
                }
            }
            modActions = resolved;
        }

        TomlTable categoryTable = result.getTable("category");
        if (categoryTable != null) {
            TomlTable whitelistTable = categoryTable.getTable("whitelisted");
            if (whitelistTable != null) {
                String color = whitelistTable.getString("color");
                if (color != null) {
                    whitelistedColor = color;
                }
                whitelistedMods = parseModList(whitelistTable.getArray("mods"));
            }

            TomlTable blacklistTable = categoryTable.getTable("blacklisted");
            if (blacklistTable != null) {
                String color = blacklistTable.getString("color");
                if (color != null) {
                    blacklistedColor = color;
                }
                blacklistedMods = parseModList(blacklistTable.getArray("mods"));
            }
        }
    }

    private static boolean getBoolean(TomlTable table, String key, boolean defaultValue) {
        Boolean value = table.getBoolean(key);
        return value != null ? value : defaultValue;
    }

    private static List<Action> resolveActions(@Nullable TomlArray array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<Action> actions = new ArrayList<>();
        for (Object value : array.toList()) {
            if (!(value instanceof String actionName)) {
                continue;
            }
            Action action = HackedServer.getAction(actionName);
            if (action != null) {
                actions.add(action);
            }
        }
        return Collections.unmodifiableList(actions);
    }

    private static Set<String> parseModList(@Nullable TomlArray array) {
        if (array == null) {
            return Collections.emptySet();
        }
        Set<String> mods = new HashSet<>();
        for (Object value : array.toList()) {
            if (value instanceof String modId) {
                mods.add(normalizeModId(modId));
            }
        }
        return Collections.unmodifiableSet(mods);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean shouldMarkForge() {
        return markForge;
    }

    public static boolean shouldMarkNeoForge() {
        return markNeoForge;
    }

    public static boolean shouldShowModsInCheck() {
        return showModsInCheck;
    }

    public static boolean shouldShowModVersions() {
        return showModVersions;
    }

    public static List<Action> getForgeActions() {
        return forgeActions;
    }

    public static List<Action> getNeoForgeActions() {
        return neoforgeActions;
    }

    public static List<Action> getModActions(String modId) {
        if (modId == null || modActions.isEmpty()) {
            return Collections.emptyList();
        }
        return modActions.getOrDefault(normalizeModId(modId), Collections.emptyList());
    }

    public static boolean isWhitelisted(String modId) {
        return modId != null && whitelistedMods.contains(normalizeModId(modId));
    }

    public static boolean isBlacklisted(String modId) {
        return modId != null && blacklistedMods.contains(normalizeModId(modId));
    }

    public static String getWhitelistedColor() {
        return whitelistedColor;
    }

    public static String getBlacklistedColor() {
        return blacklistedColor;
    }

    public static String formatMod(ForgeModInfo mod) {
        if (mod == null) {
            return "";
        }
        String modId = mod.getModId();
        if (modId == null) {
            modId = "unknown";
        }

        StringBuilder output = new StringBuilder();

        // Apply color based on category
        if (isBlacklisted(modId)) {
            output.append(blacklistedColor);
        } else if (isWhitelisted(modId)) {
            output.append(whitelistedColor);
        }

        output.append(modId);

        if (showModVersions && mod.getVersion() != null && !mod.getVersion().isBlank()) {
            output.append(" ").append(mod.getVersion());
        }

        // Close color tag if we opened one
        if (isBlacklisted(modId) || isWhitelisted(modId)) {
            // MiniMessage auto-closes tags, but we'll be explicit
        }

        return output.toString();
    }

    public static String normalizeModId(String modId) {
        return modId == null ? "" : modId.toLowerCase(Locale.ROOT);
    }
}
