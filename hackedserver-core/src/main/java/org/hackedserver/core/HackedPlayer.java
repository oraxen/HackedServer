package org.hackedserver.core;

import org.hackedserver.core.config.GenericCheck;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hackedserver.core.lunar.LunarModInfo;

public class HackedPlayer {

    private final UUID uuid;
    private final Set<String> genericChecks = new HashSet<>();
    private final Map<String, LunarModInfo> lunarMods = new LinkedHashMap<>();
    private volatile boolean lunarModsKnown = false;
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

    public void setLunarMods(Collection<LunarModInfo> mods) {
        synchronized (lunarMods) {
            lunarMods.clear();
            if (mods != null) {
                for (LunarModInfo mod : mods) {
                    if (mod == null || mod.getId() == null) {
                        continue;
                    }
                    lunarMods.put(mod.getId().toLowerCase(Locale.ROOT), mod);
                }
            }
            lunarModsKnown = true;
        }
    }

    public Collection<LunarModInfo> getLunarMods() {
        synchronized (lunarMods) {
            return List.copyOf(lunarMods.values());
        }
    }

    public boolean hasLunarModsData() {
        return lunarModsKnown;
    }

    public boolean hasLunarMod(String modId) {
        if (modId == null) {
            return false;
        }
        synchronized (lunarMods) {
            return lunarMods.containsKey(modId.toLowerCase(Locale.ROOT));
        }
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
