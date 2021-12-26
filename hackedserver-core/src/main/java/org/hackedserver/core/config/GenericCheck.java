package org.hackedserver.core.config;

import java.util.List;

public class GenericCheck {

    private final String id;
    private final String name;
    private final List<String> channels;
    private final String messageHas;
    private final List<Action> actions;

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

    public boolean pass(String channel, String message) {
        return this.channels.contains(channel)
                && (messageHas == null || message.contains(messageHas));
    }

    public List<Action> getActions() {
        return actions;
    }

}
