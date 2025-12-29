package org.hackedserver.core.config;

import org.hackedserver.core.HackedServer;
import org.hackedserver.core.lunar.LunarModInfo;
import org.jetbrains.annotations.Nullable;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LunarConfig {

    private static boolean enabled = true;
    private static boolean markLunarClient = true;
    private static boolean markFabric = true;
    private static boolean markForge = true;
    private static boolean showModsInCheck = true;
    private static boolean showModVersions = false;
    private static boolean showModTypes = false;
    private static List<Action> lunarClientActions = Collections.emptyList();
    private static List<Action> fabricActions = Collections.emptyList();
    private static List<Action> forgeActions = Collections.emptyList();
    private static Map<String, List<Action>> modActions = Collections.emptyMap();

    private LunarConfig() {
    }

    public static void load(@Nullable TomlParseResult result) {
        enabled = true;
        markLunarClient = true;
        markFabric = true;
        markForge = true;
        showModsInCheck = true;
        showModVersions = false;
        showModTypes = false;
        lunarClientActions = Collections.emptyList();
        fabricActions = Collections.emptyList();
        forgeActions = Collections.emptyList();
        modActions = Collections.emptyMap();

        if (result == null) {
            return;
        }

        Boolean enabledValue = result.getBoolean("enabled");
        if (enabledValue != null) {
            enabled = enabledValue;
        }

        TomlTable settings = result.getTable("settings");
        if (settings != null) {
            markLunarClient = getBoolean(settings, "mark_lunar_client", markLunarClient);
            markFabric = getBoolean(settings, "mark_fabric", markFabric);
            markForge = getBoolean(settings, "mark_forge", markForge);
            showModsInCheck = getBoolean(settings, "show_mods_in_check", showModsInCheck);
            showModVersions = getBoolean(settings, "show_mod_versions", showModVersions);
            showModTypes = getBoolean(settings, "show_mod_types", showModTypes);
        }

        TomlTable actionsTable = result.getTable("actions");
        if (actionsTable != null) {
            lunarClientActions = resolveActions(actionsTable.getArray("lunar_client"));
            fabricActions = resolveActions(actionsTable.getArray("fabric"));
            forgeActions = resolveActions(actionsTable.getArray("forge"));
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

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean shouldMarkLunarClient() {
        return markLunarClient;
    }

    public static boolean shouldMarkFabric() {
        return markFabric;
    }

    public static boolean shouldMarkForge() {
        return markForge;
    }

    public static boolean shouldShowModsInCheck() {
        return showModsInCheck;
    }

    public static String formatMod(LunarModInfo mod) {
        if (mod == null) {
            return "";
        }
        String label = mod.getDisplayName();
        if (label == null || label.isBlank()) {
            label = mod.getId();
        }
        if (label == null) {
            label = "unknown";
        }

        StringBuilder output = new StringBuilder(label);
        if (showModVersions && mod.getVersion() != null && !mod.getVersion().isBlank()) {
            output.append(" ").append(mod.getVersion());
        }
        if (showModTypes && mod.getType() != null && !mod.getType().isBlank()) {
            output.append(" (").append(mod.getType()).append(")");
        }
        return output.toString();
    }

    public static List<Action> getLunarClientActions() {
        return lunarClientActions;
    }

    public static List<Action> getFabricActions() {
        return fabricActions;
    }

    public static List<Action> getForgeActions() {
        return forgeActions;
    }

    public static List<Action> getModActions(String modId) {
        if (modId == null || modActions.isEmpty()) {
            return Collections.emptyList();
        }
        return modActions.getOrDefault(normalizeModId(modId), Collections.emptyList());
    }

    public static String normalizeModId(String modId) {
        return modId == null ? "" : modId.toLowerCase(Locale.ROOT);
    }
}
