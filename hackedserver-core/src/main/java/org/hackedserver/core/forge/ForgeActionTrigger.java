package org.hackedserver.core.forge;

import org.hackedserver.core.config.Action;

import java.util.List;

/**
 * Represents an action trigger for Forge/NeoForge detection.
 */
public final class ForgeActionTrigger {

    private final String name;
    private final List<Action> actions;

    public ForgeActionTrigger(String name, List<Action> actions) {
        this.name = name;
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public List<Action> getActions() {
        return actions;
    }
}
