package org.hackedserver.core.lunar;

import org.hackedserver.core.config.Action;

import java.util.Collections;
import java.util.List;

public final class LunarActionTrigger {

    private final String name;
    private final List<Action> actions;

    public LunarActionTrigger(String name, List<Action> actions) {
        this.name = name;
        this.actions = actions == null ? Collections.emptyList() : List.copyOf(actions);
    }

    public String getName() {
        return name;
    }

    public List<Action> getActions() {
        return actions;
    }

    public boolean hasActions() {
        return !actions.isEmpty();
    }
}
