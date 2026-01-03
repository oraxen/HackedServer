package org.hackedserver.core.forge;

/**
 * Type of Forge-based client detected.
 */
public enum ForgeClientType {
    /**
     * Legacy Forge (before 1.20.2).
     */
    FORGE("Forge"),

    /**
     * NeoForge (1.20.2+).
     */
    NEOFORGE("NeoForge");

    private final String displayName;

    ForgeClientType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
