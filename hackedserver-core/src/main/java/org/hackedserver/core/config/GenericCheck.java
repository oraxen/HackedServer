package org.hackedserver.core.config;

import java.util.List;
import java.util.Locale;

import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;

public class GenericCheck {

    private final String id;
    private final String name;
    private final List<String> channels;
    private final String messageHas;
    private final String messageNotHas;
    private final String category;
    private final List<Action> actions;

    public GenericCheck(String id, String name, List<String> channels, String messageHas, String messageNotHas, String category, List<Action> actions) {
        this.id = id;
        this.name = name;
        this.channels = channels;
        this.messageHas = messageHas;
        this.messageNotHas = messageNotHas;
        this.category = category != null ? category : "other";
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public boolean pass(HackedPlayer player, String channel, String message) {
        if (channel == null || !this.channels.contains(channel)) {
            return false;
        }
        String lowerMessage = message != null ? message.toLowerCase(Locale.ROOT) : "";
        if (messageHas != null && !lowerMessage.contains(messageHas.toLowerCase(Locale.ROOT))) {
            return false;
        }

        if (messageNotHas != null && lowerMessage.contains(messageNotHas.toLowerCase(Locale.ROOT))) {
            return false;
        }

        // Scope duplicates per check: one payload can legitimately match several checks
        // (e.g. a minecraft:register listing both wdl and meteor-client channels)
        return !Config.SKIP_DUPLICATES.toBool()
                || !HackedServer.isMessageDuplicate(player, id + '|' + channel, message);
    }

    public List<Action> getActions() {
        return actions;
    }

}
