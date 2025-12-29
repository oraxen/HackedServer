package org.hackedserver.bungee.apollo;

import com.lunarclient.apollo.client.mod.LunarClientMod;
import com.lunarclient.apollo.event.ApolloListener;
import com.lunarclient.apollo.event.EventBus;
import com.lunarclient.apollo.event.Listen;
import com.lunarclient.apollo.event.player.ApolloPlayerHandshakeEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.hackedserver.bungee.HackedServerPlugin;
import org.hackedserver.bungee.logs.Logs;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.LunarConfig;
import org.hackedserver.core.lunar.LunarModInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ApolloIntegration implements ApolloListener {

    private final ProxyServer server;

    public ApolloIntegration(ProxyServer server) {
        this.server = server;
    }

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
        String playerName = event.getPlayer().getName();

        boolean hadLunarCheck = hackedPlayer.hasGenericCheck("lunar_client");
        if (LunarConfig.shouldMarkLunarClient()) {
            if (!hadLunarCheck) {
                hackedPlayer.addGenericCheck("lunar_client");
            }
            if (!hadLunarCheck && !hadLunarData) {
                runActions(LunarConfig.getLunarClientActions(), uuid, playerName, "Lunar Client");
            }
        }

        boolean hadFabricCheck = hackedPlayer.hasGenericCheck("fabric");
        if (LunarConfig.shouldMarkFabric() && hasFabric) {
            if (!hadFabricCheck) {
                hackedPlayer.addGenericCheck("fabric");
            }
            if (!hadFabricCheck && !hadFabric) {
                runActions(LunarConfig.getFabricActions(), uuid, playerName, "Fabric");
            }
        }

        boolean hadForgeCheck = hackedPlayer.hasGenericCheck("forge");
        if (LunarConfig.shouldMarkForge() && hasForge) {
            if (!hadForgeCheck) {
                hackedPlayer.addGenericCheck("forge");
            }
            if (!hadForgeCheck && !hadForge) {
                runActions(LunarConfig.getForgeActions(), uuid, playerName, "Forge");
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
                runActions(LunarConfig.getModActions(modId), uuid, playerName,
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

    private void runActions(List<Action> actions, UUID uuid, String playerName, String checkName) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (Action action : actions) {
            performActions(action, uuid, playerName, checkName);
        }
    }

    private void performActions(Action action, UUID uuid, String playerName, String checkName) {
        TagResolver.Single[] templates = new TagResolver.Single[]{
                Placeholder.unparsed("player", playerName),
                Placeholder.parsed("name", checkName)
        };
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (ProxiedPlayer admin : ProxyServer.getInstance().getPlayers()) {
                if (admin.hasPermission("hackedserver.alert")) {
                    HackedServerPlugin.get().getAudiences().player(admin)
                            .sendMessage(action.getAlert(templates));
                }
            }
        }

        ProxiedPlayer player = server.getPlayer(uuid);
        if (player == null) {
            HackedServer.getPlayer(uuid).queuePendingAction(() -> executeCommands(action, uuid, checkName));
            return;
        }

        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        executeCommands(action, uuid, checkName);
    }

    private void executeCommands(Action action, UUID uuid, String checkName) {
        ProxiedPlayer player = server.getPlayer(uuid);
        if (player == null) {
            return;
        }

        for (String command : action.getConsoleCommands()) {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    ProxyServer.getInstance().getConsole(),
                    command.replace("<player>", player.getName())
                            .replace("<name>", checkName));
        }
        for (String command : action.getPlayerCommands()) {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    player,
                    command.replace("<player>", player.getName())
                            .replace("<name>", checkName));
        }
    }
}
