package org.hackedserver.core.config;

import java.util.List;

public class GenericCheck {

    private final String id;
    private final String name;
    private final String channel;
    private final List<Action> actions;

    public GenericCheck(String id, String name, String channel, List<Action> actions) {
        this.id = id;
        this.name = name;
        this.channel = channel;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getChannel() {
        return channel;
    }

    public List<Action> getActions() {
        return actions;
    }

}
