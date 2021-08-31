package org.hackedserver.core.config;

import java.util.List;

public abstract class AbstractAction {

    private List<String> consoleCommands;
    private List<String> playerCommands;
    private List<String> oppedPlayerCommands;
    private String alert;

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

    public List<String> getConsoleCommands() {
        return consoleCommands;
    }

    public List<String> getPlayerCommands() {
        return playerCommands;
    }

    public List<String> getOppedPlayerCommands() {
        return oppedPlayerCommands;
    }
}
