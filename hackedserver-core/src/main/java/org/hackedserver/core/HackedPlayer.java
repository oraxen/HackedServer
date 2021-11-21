package org.hackedserver.core;

import org.hackedserver.core.config.GenericCheck;

import java.util.*;

public class HackedPlayer {

    private final UUID uuid;
    private final Set<String> genericChecks = new HashSet<>();

    public HackedPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void addGenericCheck(String id) {
        genericChecks.add(id);
    }

    public void addGenericCheck(GenericCheck check) {
        genericChecks.add(check.getId());
    }

    public boolean hasGenericCheck(String id) {
        return genericChecks.contains(id);
    }

    public boolean hasGenericCheck(GenericCheck check) {
        return genericChecks.contains(check.getId());
    }

    public Set<String> getGenericChecks() {
        return genericChecks;
    }

}
