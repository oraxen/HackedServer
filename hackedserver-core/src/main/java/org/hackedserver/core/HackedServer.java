package org.hackedserver.core;

import org.hackedserver.core.config.GenericCheck;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HackedServer {

    private final static Map<UUID, HackedPlayer> players = new HashMap<>();
    private final static Map<String, GenericCheck> genericChecks = new HashMap<>();

    public void registerPlayer(UUID uuid, HackedPlayer player) {
        players.put(uuid, player);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public void removePlayer(HackedPlayer player) {
        removePlayer(player.getUuid());
    }

    public HackedPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }


    public void registerCheck(String id, GenericCheck check) {
        genericChecks.put(id, check);
    }

    public void removeCheck(String id) {
        genericChecks.remove(id);
    }

    public void removeCheck(GenericCheck check) {
        removeCheck(check.getId());
    }

    public GenericCheck getCheck(String id) {
        return genericChecks.get(id);
    }
}
