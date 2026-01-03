package org.hackedserver.core.forge;

import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.config.Action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processes Forge/NeoForge handshake data and triggers actions.
 */
public final class ForgeHandshakeProcessor {

    private ForgeHandshakeProcessor() {
    }

    /**
     * Processes the client type (from minecraft:brand).
     *
     * @param player the player
     * @param clientType the detected client type
     * @return the result with any triggered actions
     */
    public static ForgeHandshakeResult processClientType(HackedPlayer player, ForgeClientType clientType) {
        if (player == null || clientType == null || !ForgeConfig.isEnabled()) {
            return new ForgeHandshakeResult(List.of());
        }

        List<ForgeActionTrigger> triggers = new ArrayList<>();

        boolean hadForgeCheck = player.hasGenericCheck("forge");
        boolean hadNeoForgeCheck = player.hasGenericCheck("neoforge");

        switch (clientType) {
            case FORGE -> {
                if (ForgeConfig.shouldMarkForge()) {
                    if (!hadForgeCheck) {
                        player.addGenericCheck("forge");
                        List<Action> actions = ForgeConfig.getForgeActions();
                        if (!actions.isEmpty()) {
                            triggers.add(new ForgeActionTrigger("Forge", actions));
                        }
                    }
                }
            }
            case NEOFORGE -> {
                if (ForgeConfig.shouldMarkNeoForge()) {
                    if (!hadNeoForgeCheck) {
                        player.addGenericCheck("neoforge");
                        List<Action> actions = ForgeConfig.getNeoForgeActions();
                        if (!actions.isEmpty()) {
                            triggers.add(new ForgeActionTrigger("NeoForge", actions));
                        }
                    }
                }
            }
        }

        return new ForgeHandshakeResult(triggers);
    }

    /**
     * Processes detected mods (from minecraft:register).
     *
     * @param player the player
     * @param mods the detected mods
     * @return the result with any triggered actions
     */
    public static ForgeHandshakeResult processMods(HackedPlayer player, List<ForgeModInfo> mods) {
        if (player == null || mods == null || mods.isEmpty() || !ForgeConfig.isEnabled()) {
            return new ForgeHandshakeResult(List.of());
        }

        List<ForgeModInfo> previousMods = new ArrayList<>(player.getForgeMods());
        Set<String> previousIds = new HashSet<>();
        for (ForgeModInfo mod : previousMods) {
            previousIds.add(ForgeConfig.normalizeModId(mod.getModId()));
        }

        player.addForgeMods(mods);

        List<ForgeActionTrigger> triggers = new ArrayList<>();

        for (ForgeModInfo mod : mods) {
            String modId = ForgeConfig.normalizeModId(mod.getModId());
            if (modId.isEmpty() || previousIds.contains(modId)) {
                continue;
            }

            List<Action> actions = ForgeConfig.getModActions(modId);
            if (!actions.isEmpty()) {
                triggers.add(new ForgeActionTrigger(ForgeConfig.formatMod(mod), actions));
            }
        }

        return new ForgeHandshakeResult(triggers);
    }
}
