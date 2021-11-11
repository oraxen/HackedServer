package org.hackedserver.core;

import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.GenericCheck;

import java.util.*;

public class HackedServer {

    private final static Map<UUID, HackedPlayer> players = new HashMap<>();
    private final static Map<String, Action> actions = new HashMap<>();
    private final static Map<String, GenericCheck> genericChecks = new HashMap<>();

    public static void clear() {
        players.clear();
        actions.clear();
        genericChecks.clear();
    }

    public static void registerPlayer(UUID uuid, HackedPlayer player) {
        players.put(uuid, player);
    }

    public static void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public static void removePlayer(HackedPlayer player) {
        removePlayer(player.getUuid());
    }

    public static HackedPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public static void registerAction(Action action) {
        actions.put(action.getId(), action);
    }

    public static void removeAction(String id) {
        actions.remove(id);
    }

    public static void removeAction(Action action) {
        removeAction(action.getId());
    }

    public static Action getAction(String id) {
        return actions.get(id);
    }


    public static void registerCheck(GenericCheck check) {
        genericChecks.put(check.getId(), check);
    }

    public static void removeCheck(String id) {
        genericChecks.remove(id);
    }

    public static void removeCheck(GenericCheck check) {
        removeCheck(check.getId());
    }

    public static GenericCheck getCheck(String id) {
        return genericChecks.get(id);
    }

    public static Collection<GenericCheck> getChecks() {
        return genericChecks.values();
    }
}
