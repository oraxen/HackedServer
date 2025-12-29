package org.hackedserver.core.lunar;

import java.util.Collections;
import java.util.List;

public final class LunarHandshakeResult {

    private final List<LunarActionTrigger> triggers;

    public LunarHandshakeResult(List<LunarActionTrigger> triggers) {
        this.triggers = triggers == null ? Collections.emptyList() : List.copyOf(triggers);
    }

    public List<LunarActionTrigger> getTriggers() {
        return triggers;
    }

    public boolean hasTriggers() {
        return !triggers.isEmpty();
    }
}
