package org.hackedserver.core.config;

import org.tomlj.TomlParseResult;

public enum Config {

    LANG_FILE("settings.language"),
    DEBUG("settings.debug"),
    SKIP_DUPLICATES("settings.skip_duplicates"),
    AUTO_DOWNLOAD_DEPENDENCIES("settings.auto_download_dependencies"),
    ACTION_DELAY_TICKS("settings.action_delay_ticks"),

    JOIN_WEBHOOK_ENABLED("join_webhook.enabled"),
    JOIN_WEBHOOK_URL("join_webhook.url"),
    JOIN_WEBHOOK_CONTENT("join_webhook.content"),
    JOIN_WEBHOOK_EMBED_TITLE("join_webhook.embed_title"),
    JOIN_WEBHOOK_EMBED_DESCRIPTION("join_webhook.embed_description"),
    JOIN_WEBHOOK_EMBED_COLOR("join_webhook.embed_color"),
    JOIN_WEBHOOK_EMBED_FOOTER("join_webhook.embed_footer");

    private static TomlParseResult result;

    public static void setParseResult(TomlParseResult newResult) {
        result = newResult;
    }

    private final String path;

    Config(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String toString() {
        return result.getString(path);
    }

    public String getStringOrDefault(String defaultValue) {
        if (result == null) {
            return defaultValue;
        }
        String value = result.getString(path);
        return value != null ? value : defaultValue;
    }

    public boolean toBool() {
        return toBool(false);
    }

    public boolean toBool(boolean defaultValue) {
        Boolean value = result.getBoolean(path);
        return value != null ? value : defaultValue;
    }

    public long toLong() {
        return toLong(0L);
    }

    public long toLong(long defaultValue) {
        Long value = result.getLong(path);
        return value != null ? value : defaultValue;
    }

}
