package org.hackedserver.core;

import org.hackedserver.core.config.GenericCheck;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HackedPlayer {

    private final UUID uuid;
    private final Set<String> genericChecks = new HashSet<>();
    private final Queue<Runnable> pendingActions = new ConcurrentLinkedQueue<>();

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

    /**
     * Queues an action to be executed when the player fully joins the server.
     */
    public void queuePendingAction(Runnable action) {
        pendingActions.add(action);
    }

    /**
     * Checks if there are any pending actions waiting to be executed.
     */
    public boolean hasPendingActions() {
        return !pendingActions.isEmpty();
    }

    /**
     * Executes and clears all pending actions.
     */
    public void executePendingActions() {
        Runnable action;
        while ((action = pendingActions.poll()) != null) {
            action.run();
        }
    }

}
