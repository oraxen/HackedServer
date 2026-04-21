package org.hackedserver.core.probing;

import org.hackedserver.core.config.Action;

import java.util.List;

/**
 * A single translation-based check that detects a mod by probing
 * whether the client resolves a specific translation key.
 */
public final class TranslationCheck {

    private final String id;
    private final String name;
    private final String translationKey;
    private final boolean bypassProtection;
    private final List<Action> actions;

    public TranslationCheck(String id, String name, String translationKey,
                            boolean bypassProtection, List<Action> actions) {
        this.id = id;
        this.name = name;
        this.translationKey = translationKey;
        this.bypassProtection = bypassProtection;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Whether to wrap the key in a %s substitution to bypass
     * client-side sign translation protection mixins.
     */
    public boolean isBypassProtection() {
        return bypassProtection;
    }

    public List<Action> getActions() {
        return actions;
    }

    /**
     * Returns the expected vanilla client response for this check.
     * A vanilla client cannot resolve mod translation keys, so it returns
     * the raw key string.
     */
    public String getExpectedVanillaResponse() {
        return translationKey;
    }
}
