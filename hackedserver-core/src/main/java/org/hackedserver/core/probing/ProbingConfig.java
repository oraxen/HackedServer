package org.hackedserver.core.probing;

import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.jetbrains.annotations.Nullable;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for active probing via sign translation vulnerability.
 */
public final class ProbingConfig {

    private static boolean enabled = true;
    private static long delayTicks = 40;
    private static int signOffsetY = -5;
    private static List<TranslationCheck> checks = Collections.emptyList();

    private ProbingConfig() {
    }

    public static void load(@Nullable TomlParseResult result) {
        enabled = true;
        delayTicks = 40;
        signOffsetY = -5;
        checks = Collections.emptyList();

        if (result == null) {
            return;
        }

        Boolean enabledValue = result.getBoolean("enabled");
        if (enabledValue != null) {
            enabled = enabledValue;
        }

        TomlTable settings = result.getTable("settings");
        if (settings != null) {
            Long delay = settings.getLong("delay_ticks");
            if (delay != null) {
                delayTicks = Math.max(1L, delay);
            }
            Long offset = settings.getLong("sign_offset_y");
            if (offset != null) {
                signOffsetY = offset.intValue();
            }
        }

        TomlTable checksTable = result.getTable("checks");
        if (checksTable != null) {
            List<TranslationCheck> loaded = new ArrayList<>();
            for (String key : checksTable.keySet()) {
                TomlTable checkTable = checksTable.getTable(key);
                if (checkTable == null) {
                    continue;
                }

                String name = checkTable.getString("name");
                String translationKey = checkTable.getString("translation_key");
                if (name == null || translationKey == null) {
                    continue;
                }

                Boolean bypass = checkTable.getBoolean("bypass_protection");
                boolean bypassProtection = bypass != null && bypass;

                List<Action> actions = resolveActions(checkTable.getArray("actions"));

                loaded.add(new TranslationCheck(key, name, translationKey, bypassProtection, actions));
            }
            checks = Collections.unmodifiableList(loaded);
        }
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

    public static long getDelayTicks() {
        return delayTicks;
    }

    public static int getSignOffsetY() {
        return signOffsetY;
    }

    public static List<TranslationCheck> getChecks() {
        return checks;
    }
}
