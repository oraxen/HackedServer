package org.hackedserver.core.lunar;

import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.config.LunarConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LunarHandshakeProcessor {

    private LunarHandshakeProcessor() {
    }

    public static LunarHandshakeResult process(HackedPlayer player, List<LunarModInfo> mods) {
        if (player == null || mods == null || !LunarConfig.isEnabled()) {
            return new LunarHandshakeResult(List.of());
        }

        boolean hadLunarData = player.hasLunarModsData();
        List<LunarModInfo> previousMods = new ArrayList<>(player.getLunarMods());
        boolean hadFabric = containsFabric(previousMods);
        boolean hadForge = containsForge(previousMods);

        boolean hadLunarCheck = player.hasGenericCheck("lunar_client");
        boolean hadFabricCheck = player.hasGenericCheck("fabric");
        boolean hadForgeCheck = player.hasGenericCheck("forge");

        player.setLunarMods(mods);

        boolean hasFabric = containsFabric(mods);
        boolean hasForge = containsForge(mods);

        List<LunarActionTrigger> triggers = new ArrayList<>();

        if (LunarConfig.shouldMarkLunarClient()) {
            if (!hadLunarCheck) {
                player.addGenericCheck("lunar_client");
            }
            if (!hadLunarCheck && !hadLunarData) {
                triggers.add(new LunarActionTrigger("Lunar Client", LunarConfig.getLunarClientActions()));
            }
        }

        if (LunarConfig.shouldMarkFabric() && hasFabric) {
            if (!hadFabricCheck) {
                player.addGenericCheck("fabric");
            }
            if (!hadFabricCheck && !hadFabric) {
                triggers.add(new LunarActionTrigger("Fabric", LunarConfig.getFabricActions()));
            }
        }

        if (LunarConfig.shouldMarkForge() && hasForge) {
            if (!hadForgeCheck) {
                player.addGenericCheck("forge");
            }
            if (!hadForgeCheck && !hadForge) {
                triggers.add(new LunarActionTrigger("Forge", LunarConfig.getForgeActions()));
            }
        }

        if (!mods.isEmpty()) {
            Set<String> previousIds = new HashSet<>();
            for (LunarModInfo mod : previousMods) {
                previousIds.add(LunarConfig.normalizeModId(mod.getId()));
            }
            for (LunarModInfo mod : mods) {
                String modId = LunarConfig.normalizeModId(mod.getId());
                if (modId.isEmpty() || previousIds.contains(modId)) {
                    continue;
                }
                List<org.hackedserver.core.config.Action> actions = LunarConfig.getModActions(modId);
                if (!actions.isEmpty()) {
                    triggers.add(new LunarActionTrigger(LunarConfig.formatMod(mod), actions));
                }
            }
        }

        return new LunarHandshakeResult(triggers);
    }

    private static boolean containsFabric(List<LunarModInfo> mods) {
        for (LunarModInfo mod : mods) {
            if (mod.isFabric()) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsForge(List<LunarModInfo> mods) {
        for (LunarModInfo mod : mods) {
            if (mod.isForge()) {
                return true;
            }
        }
        return false;
    }
}
