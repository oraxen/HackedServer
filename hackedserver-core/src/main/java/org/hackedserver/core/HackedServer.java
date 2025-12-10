package org.hackedserver.core;

import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.GenericCheck;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HackedServer {

    private final static Map<UUID, HackedPlayer> players = new ConcurrentHashMap<>();
    private final static Map<String, Action> actions = new HashMap<>();
    private final static Map<String, GenericCheck> genericChecks = new HashMap<>();
    private final static Map<HackedPlayer, Set<MessagePayload>> messageHistory = new ConcurrentHashMap<>();

    public static void clear() {
        players.clear();
        actions.clear();
        genericChecks.clear();
        messageHistory.clear();
    }

    public static void registerPlayer(UUID uuid) {
        // Use computeIfAbsent to avoid replacing an existing player that may have
        // been created by getPlayer() during packet handling, which would lose
        // any pending actions that were queued.
        players.computeIfAbsent(uuid, HackedPlayer::new);
    }

    public static void removePlayer(UUID uuid) {
        HackedPlayer player = players.get(uuid);
        if (player != null) {
            messageHistory.remove(player);
        }
        players.remove(uuid);
    }

    public static void removePlayer(HackedPlayer player) {
        removePlayer(player.getUuid());
    }

    public static HackedPlayer getPlayer(UUID uuid) {
        return players.computeIfAbsent(uuid, key -> new HackedPlayer(uuid));
    }

    public static Collection<HackedPlayer> getPlayers() {
        return players.values();
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

    public static boolean isMessageDuplicate(HackedPlayer player, String channel, String message) {
        MessagePayload payload = new MessagePayload(channel, message);
        Set<MessagePayload> playerHistory = messageHistory.computeIfAbsent(player, k -> new HashSet<>());
        return !playerHistory.add(payload);
    }
}
