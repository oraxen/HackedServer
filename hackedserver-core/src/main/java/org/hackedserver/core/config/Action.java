package org.hackedserver.core.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;

public class Action {

    private final String id;
    private List<String> consoleCommands;
    private List<String> playerCommands;
    private List<String> oppedPlayerCommands;
    private String alert;

    public Action(String id) {
        this.id = id;
    }

    public void setConsoleCommands(List<String> consoleCommands) {
        this.consoleCommands = consoleCommands;
    }

    public void setPlayerCommands(List<String> playerCommands) {
        this.playerCommands = playerCommands;
    }

    public void setOppedPlayerCommands(List<String> oppedPlayerCommands) {
        this.oppedPlayerCommands = oppedPlayerCommands;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    public boolean hasAlert() {
        return alert != null;
    }

    public String getId() {
        return id;
    }

    public List<String> getConsoleCommands() {
        return consoleCommands;
    }

    public List<String> getPlayerCommands() {
        return playerCommands;
    }

    public List<String> getOppedPlayerCommands() {
        return oppedPlayerCommands;
    }

    public Component getAlert(TagResolver.Single... placeholders) {
        return Message.parse(alert, placeholders);
    }
}
