package org.hackedserver.core.lunar;

import java.util.Locale;
import java.util.Objects;

public final class LunarModInfo {

    private final String id;
    private final String displayName;
    private final String version;
    private final String type;

    public LunarModInfo(String id, String displayName, String version, String type) {
        this.id = id;
        this.displayName = displayName;
        this.version = version;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public boolean isFabric() {
        return hasType("FABRIC");
    }

    public boolean isForge() {
        return hasType("FORGE");
    }

    private boolean hasType(String token) {
        if (type == null) {
            return false;
        }
        return type.toUpperCase(Locale.ROOT).contains(token);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LunarModInfo other = (LunarModInfo) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(displayName, other.displayName)
                && Objects.equals(version, other.version)
                && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, version, type);
    }
}
