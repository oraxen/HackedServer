package org.hackedserver.core.config;

public class GenericCheck {

    private final String id;
    private final String name;
    private final String channel;
    private final AbstractAction action;

    public GenericCheck(String id, String name, String channel, AbstractAction action) {
        this.id = id;
        this.name = name;
        this.channel = channel;
        this.action = action;
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

    public AbstractAction getAction() {
        return action;
    }

}
