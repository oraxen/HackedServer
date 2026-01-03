package org.hackedserver.core.forge;

import java.util.List;

/**
 * Result of processing a Forge/NeoForge handshake.
 */
public final class ForgeHandshakeResult {

    private final List<ForgeActionTrigger> triggers;

    public ForgeHandshakeResult(List<ForgeActionTrigger> triggers) {
        this.triggers = triggers != null ? triggers : List.of();
    }

    public List<ForgeActionTrigger> getTriggers() {
        return triggers;
    }

    public boolean hasTriggers() {
        return !triggers.isEmpty();
    }
}
