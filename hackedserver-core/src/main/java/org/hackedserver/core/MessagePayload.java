package org.hackedserver.core;

import java.util.Objects;

final class MessagePayload {
    private final String channel;
    private final String message;

    public MessagePayload(String channel, String message) {
        this.channel = channel;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MessagePayload that = (MessagePayload) o;
        return Objects.equals(channel, that.channel) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, message);
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }
}