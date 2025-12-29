package org.hackedserver.core.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Collections;
import java.util.List;

public class Action {

    private final String id;
    private List<String> consoleCommands = Collections.emptyList();
    private List<String> playerCommands = Collections.emptyList();
    private List<String> oppedPlayerCommands = Collections.emptyList();
    private String alert;
    private long delayTicks = -1; // -1 means use global setting

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

    public void setDelayTicks(long delayTicks) {
        this.delayTicks = delayTicks;
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

    /**
     * Gets the delay in ticks before executing this action.
     * Returns the action-specific delay if set, otherwise the global setting.
     * Default is 20 ticks (1 second) if not configured.
     */
    public long getDelayTicks() {
        if (delayTicks >= 0) {
            return delayTicks;
        }
        // Default to 20 ticks (1 second) if not configured in config.toml
        return Config.ACTION_DELAY_TICKS.toLong(20L);
    }

    /**
     * Returns true if this action has a custom delay set (not using global).
     */
    public boolean hasCustomDelay() {
        return delayTicks >= 0;
    }

    public Component getAlert(TagResolver.Single... placeholders) {
        return Message.parse(alert, placeholders);
    }
}
