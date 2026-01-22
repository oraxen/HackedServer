package org.hackedserver.core.config;

import org.hackedserver.core.HackedServer;
import org.jetbrains.annotations.Nullable;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BedrockConfig {

    private static boolean enabled = false;
    private static String label = "Bedrock";
    private static List<Action> bedrockActions = Collections.emptyList();

    private BedrockConfig() {
    }

    public static void load(@Nullable TomlParseResult result) {
        enabled = false;
        label = "Bedrock";
        bedrockActions = Collections.emptyList();

        if (result == null) {
            return;
        }

        Boolean enabledValue = result.getBoolean("enabled");
        if (enabledValue != null) {
            enabled = enabledValue;
        }

        String labelValue = result.getString("label");
        if (labelValue != null && !labelValue.isBlank()) {
            label = labelValue;
        }

        TomlArray actionsArray = result.getArray("actions");
        if (actionsArray != null) {
            bedrockActions = resolveActions(actionsArray);
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

    public static String getLabel() {
        return label;
    }

    public static List<Action> getBedrockActions() {
        return bedrockActions;
    }
}
