package org.hackedserver.core.config;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;

public class GenericCheck {

    private final String id;
    private final String name;
    private final List<String> channels;
    private final String messageHas;
    private final List<Action> actions;
    private final Map<HackedPlayer, Set<Integer>> messageHistory = new HashMap<>();

    public GenericCheck(String id, String name, List<String> channels, String messageHas, List<Action> actions) {
        this.id = id;
        this.name = name;
        this.channels = channels;
        this.messageHas = messageHas;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean pass(HackedPlayer player, String channel, String message) {
        if (!this.channels.contains(channel) ||
                (messageHas != null && !message.toLowerCase().contains(messageHas.toLowerCase()))) {
            return false;
        }

        return !Config.SKIP_DUPLICATES.toBool() || !HackedServer.isMessageDuplicate(player, channel, message);
    }

    public List<Action> getActions() {
        return actions;
    }

}
