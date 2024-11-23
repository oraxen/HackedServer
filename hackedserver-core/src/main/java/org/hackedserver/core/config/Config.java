package org.hackedserver.core.config;

import org.tomlj.TomlParseResult;

public enum Config {

    LANG_FILE("settings.language"),
    DEBUG("settings.debug");

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

}
