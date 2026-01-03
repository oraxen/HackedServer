package org.hackedserver.core.forge;

import java.util.Objects;

/**
 * Information about a Forge/NeoForge mod detected via channel registration.
 */
public final class ForgeModInfo {

    private final String modId;
    private final String version;

    public ForgeModInfo(String modId, String version) {
        this.modId = modId;
        this.version = version;
    }

    public ForgeModInfo(String modId) {
        this(modId, null);
    }

    public String getModId() {
        return modId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ForgeModInfo other = (ForgeModInfo) obj;
        return Objects.equals(modId, other.modId)
                && Objects.equals(version, other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, version);
    }

    @Override
    public String toString() {
        if (version != null && !version.isBlank()) {
            return modId + " (" + version + ")";
        }
        return modId;
    }
}
