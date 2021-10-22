package org.hackedserver.core.config;

import java.util.List;

public class GenericCheck {

    private final String id;
    private final String name;
    private final String channel;
    private final String messageHas;
    private final List<Action> actions;

    public GenericCheck(String id, String name, String channel, String messageHas, List<Action> actions) {
        this.id = id;
        this.name = name;
        this.channel = channel;
        this.messageHas = messageHas;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean pass(String channel, String message) {
        return channel.equals(this.channel)
                && (messageHas == null || message.contains(messageHas));
    }

    public List<Action> getActions() {
        return actions;
    }

}
