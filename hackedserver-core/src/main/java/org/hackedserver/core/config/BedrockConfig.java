package org.hackedserver.core.config;

import org.hackedserver.core.HackedServer;
import org.jetbrains.annotations.Nullable;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BedrockConfig {

    private static boolean enabled = false;
    private static boolean useGeyserApi = true;
    private static boolean useFloodgateApi = true;
    private static String label = "Bedrock";
    private static List<Action> bedrockActions = Collections.emptyList();

    private BedrockConfig() {
    }

    public static void load(@Nullable TomlParseResult result) {
        enabled = false;
        useGeyserApi = true;
        useFloodgateApi = true;
        label = "Bedrock";
        bedrockActions = Collections.emptyList();

        if (result == null) {
            return;
        }

        Boolean enabledValue = result.getBoolean("enabled");
        if (enabledValue != null) {
            enabled = enabledValue;
        }

        TomlTable settings = result.getTable("settings");
        if (settings != null) {
            useGeyserApi = getBoolean(settings, "use_geyser_api", useGeyserApi);
            useFloodgateApi = getBoolean(settings, "use_floodgate_api", useFloodgateApi);
            String labelValue = settings.getString("label");
            if (labelValue != null && !labelValue.isBlank()) {
                label = labelValue;
            }
        }

        TomlTable actionsTable = result.getTable("actions");
        if (actionsTable != null) {
            bedrockActions = resolveActions(actionsTable.getArray("bedrock"));
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

    public static boolean useGeyserApi() {
        return useGeyserApi;
    }

    public static boolean useFloodgateApi() {
        return useFloodgateApi;
    }

    public static String getLabel() {
        return label;
    }

    public static List<Action> getBedrockActions() {
        return bedrockActions;
    }
}
