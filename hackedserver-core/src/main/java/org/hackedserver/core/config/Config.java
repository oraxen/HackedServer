package org.hackedserver.core.config;

import org.tomlj.TomlParseResult;

public enum Config {

    LANG_FILE("settings.language"),
    DEBUG("settings.debug"),
    SKIP_DUPLICATES("settings.skip_duplicates"),
    AUTO_DOWNLOAD_DEPENDENCIES("settings.auto_download_dependencies"),
    ACTION_DELAY_TICKS("settings.action_delay_ticks");

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

    public boolean toBool() {
        return Boolean.TRUE.equals(result.getBoolean(path));
    }

    public long toLong() {
        return toLong(0L);
    }

    public long toLong(long defaultValue) {
        Long value = result.getLong(path);
        return value != null ? value : defaultValue;
    }

}
