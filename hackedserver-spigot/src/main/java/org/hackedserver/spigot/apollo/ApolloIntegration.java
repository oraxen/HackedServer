package org.hackedserver.spigot.apollo;

import com.lunarclient.apollo.client.mod.LunarClientMod;
import com.lunarclient.apollo.event.ApolloListener;
import com.lunarclient.apollo.event.EventBus;
import com.lunarclient.apollo.event.Listen;
import com.lunarclient.apollo.event.player.ApolloPlayerHandshakeEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.LunarConfig;
import org.hackedserver.core.lunar.LunarModInfo;
import org.hackedserver.spigot.HackedServerPlugin;
import org.hackedserver.spigot.utils.logs.Logs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ApolloIntegration implements ApolloListener {

    public void register() {
        EventBus.getBus().register(this);
    }

    public void unregister() {
        EventBus.getBus().unregister(this);
    }

    @Listen
    public void onHandshake(ApolloPlayerHandshakeEvent event) {
        if (!LunarConfig.isEnabled()) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        HackedPlayer hackedPlayer = HackedServer.getPlayer(uuid);
        boolean hadLunarData = hackedPlayer.hasLunarModsData();
        List<LunarModInfo> previousMods = new ArrayList<>(hackedPlayer.getLunarMods());
        boolean hadFabric = containsFabric(previousMods);
        boolean hadForge = containsForge(previousMods);

        List<LunarModInfo> mods = toModInfo(event.getInstalledMods());
        hackedPlayer.setLunarMods(mods);

        boolean hasFabric = containsFabric(mods);
        boolean hasForge = containsForge(mods);
        Player player = Bukkit.getPlayer(uuid);
        String playerName = event.getPlayer().getName();

        boolean hadLunarCheck = hackedPlayer.hasGenericCheck("lunar_client");
        if (LunarConfig.shouldMarkLunarClient()) {
            if (!hadLunarCheck) {
                hackedPlayer.addGenericCheck("lunar_client");
            }
            if (!hadLunarCheck && !hadLunarData) {
                runActions(LunarConfig.getLunarClientActions(), uuid, player, playerName, "Lunar Client");
            }
        }

        boolean hadFabricCheck = hackedPlayer.hasGenericCheck("fabric");
        if (LunarConfig.shouldMarkFabric() && hasFabric) {
            if (!hadFabricCheck) {
                hackedPlayer.addGenericCheck("fabric");
            }
            if (!hadFabricCheck && !hadFabric) {
                runActions(LunarConfig.getFabricActions(), uuid, player, playerName, "Fabric");
            }
        }

        boolean hadForgeCheck = hackedPlayer.hasGenericCheck("forge");
        if (LunarConfig.shouldMarkForge() && hasForge) {
            if (!hadForgeCheck) {
                hackedPlayer.addGenericCheck("forge");
            }
            if (!hadForgeCheck && !hadForge) {
                runActions(LunarConfig.getForgeActions(), uuid, player, playerName, "Forge");
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
                runActions(LunarConfig.getModActions(modId), uuid, player, playerName,
                        LunarConfig.formatMod(mod));
            }
        }
    }

    private List<LunarModInfo> toModInfo(List<LunarClientMod> mods) {
        List<LunarModInfo> result = new ArrayList<>();
        if (mods == null) {
            return result;
        }
        for (LunarClientMod mod : mods) {
            if (mod == null || mod.getId() == null) {
                continue;
            }
            String type = mod.getType() != null ? mod.getType().name() : null;
            result.add(new LunarModInfo(mod.getId(), mod.getDisplayName(), mod.getVersion(), type));
        }
        return result;
    }

    private boolean containsFabric(List<LunarModInfo> mods) {
        for (LunarModInfo mod : mods) {
            if (mod.isFabric()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsForge(List<LunarModInfo> mods) {
        for (LunarModInfo mod : mods) {
            if (mod.isForge()) {
                return true;
            }
        }
        return false;
    }

    private void runActions(List<Action> actions, UUID uuid, Player player, String playerName, String checkName) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (Action action : actions) {
            performActions(action, uuid, player, playerName, checkName);
        }
    }

    private void performActions(Action action, UUID uuid, Player player, String playerName, String checkName) {
        TagResolver.Single[] templates = new TagResolver.Single[]{
                Placeholder.unparsed("player", playerName),
                Placeholder.parsed("name", checkName)
        };
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("hackedserver.alert")) {
                    HackedServerPlugin.get().getAudiences().player(admin)
                            .sendMessage(action.getAlert(templates));
                }
            }
        }

        if (player == null || !player.isOnline()) {
            HackedPlayer hackedPlayer = HackedServer.getPlayer(uuid);
            hackedPlayer.queuePendingAction(() ->
                    executeCommands(action, uuid, checkName));
            return;
        }

        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        executeCommands(action, uuid, checkName);
    }

    private void executeCommands(Action action, UUID uuid, String checkName) {
        long delayTicks = action.getDelayTicks();

        Runnable commandRunner = () -> {
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }
            if (onlinePlayer.hasPermission("hackedserver.bypass")) {
                return;
            }

            for (String command : action.getConsoleCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("<player>", onlinePlayer.getName())
                                .replace("<name>", checkName));
            }
            for (String command : action.getPlayerCommands()) {
                Bukkit.dispatchCommand(onlinePlayer,
                        command.replace("<player>", onlinePlayer.getName())
                                .replace("<name>", checkName));
            }
            for (String command : action.getOppedPlayerCommands()) {
                boolean op = onlinePlayer.isOp();
                onlinePlayer.setOp(true);
                Bukkit.dispatchCommand(onlinePlayer,
                        command.replace("<player>", onlinePlayer.getName())
                                .replace("<name>", checkName));
                onlinePlayer.setOp(op);
            }
        };

        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), commandRunner, delayTicks);
        } else {
            Bukkit.getScheduler().runTask(HackedServerPlugin.get(), commandRunner);
        }
    }

}
