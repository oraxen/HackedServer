package org.hackedserver.spigot.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Action;
import org.hackedserver.core.config.Config;
import org.hackedserver.core.config.GenericCheck;
import org.hackedserver.core.config.Message;
import org.hackedserver.core.forge.ForgeActionTrigger;
import org.hackedserver.core.forge.ForgeChannelParser;
import org.hackedserver.core.forge.ForgeClientType;
import org.hackedserver.core.forge.ForgeConfig;
import org.hackedserver.core.forge.ForgeHandshakeProcessor;
import org.hackedserver.core.forge.ForgeHandshakeResult;
import org.hackedserver.core.forge.ForgeModInfo;
import org.hackedserver.spigot.HackedServerPlugin;
import org.hackedserver.spigot.utils.logs.Logs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PayloadProcessor {

    private PayloadProcessor() {
    }

    public static void process(Player player, String channel, String message) {
        if (player == null) {
            return;
        }
        if (message == null) {
            message = "unknown";
        }

        if (Config.DEBUG.toBool()) {
            Logs.logComponent(Message.DEBUG_MESSAGE.toComponent(
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("channel", channel),
                    Placeholder.unparsed("message", message)));
        }

        // Generic checks
        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
        for (GenericCheck check : HackedServer.getChecks()) {
            if (check.pass(hackedPlayer, channel, message)) {
                hackedPlayer.addGenericCheck(check);
                for (Action action : check.getActions()) {
                    Map<String, String> commandPlaceholders = new HashMap<>();
                    commandPlaceholders.put("name", check.getName());
                    performActions(action, player, commandPlaceholders,
                            Placeholder.unparsed("player", player.getName()),
                            Placeholder.parsed("name", check.getName()));
                }
            }
        }

        // Forge/NeoForge detection
        if (ForgeConfig.isEnabled()) {
            processForgePacket(player, hackedPlayer, channel, message);
        }
    }

    public static void runActions(Player player, String checkName, List<Action> actions,
                                  TagResolver.Single... extraPlaceholders) {
        runActions(player, checkName, actions, new HashMap<>(), extraPlaceholders);
    }

    public static void runActions(Player player, String checkName, List<Action> actions,
                                  Map<String, String> commandPlaceholders, TagResolver.Single... extraPlaceholders) {
        if (player == null || actions == null || actions.isEmpty()) {
            return;
        }
        TagResolver.Single[] placeholders = new TagResolver.Single[extraPlaceholders.length + 2];
        placeholders[0] = Placeholder.unparsed("player", player.getName());
        placeholders[1] = Placeholder.parsed("name", checkName);
        System.arraycopy(extraPlaceholders, 0, placeholders, 2, extraPlaceholders.length);

        // Build full command placeholders map
        Map<String, String> fullPlaceholders = new HashMap<>(commandPlaceholders);
        fullPlaceholders.put("name", checkName);

        for (Action action : actions) {
            performActions(action, player, fullPlaceholders, placeholders);
        }
    }

    private static void processForgePacket(Player player, HackedPlayer hackedPlayer, String channel, String message) {
        // Detect client type from minecraft:brand
        if (ForgeChannelParser.BRAND_CHANNEL.equalsIgnoreCase(channel)) {
            ForgeClientType clientType = ForgeChannelParser.parseClientType(message);
            if (clientType != null && hackedPlayer.getForgeClientType() == null) {
                hackedPlayer.setForgeClientType(clientType);
                ForgeHandshakeResult result = ForgeHandshakeProcessor.processClientType(hackedPlayer, clientType);
                if (result.hasTriggers()) {
                    runForgeActions(result.getTriggers(), player);
                }
            }
        }

        // Detect mods from minecraft:register
        if (ForgeChannelParser.REGISTER_CHANNEL.equalsIgnoreCase(channel)) {
            List<ForgeModInfo> mods = ForgeChannelParser.parseRegisteredChannels(message);
            if (!mods.isEmpty()) {
                ForgeHandshakeResult result = ForgeHandshakeProcessor.processMods(hackedPlayer, mods);
                if (result.hasTriggers()) {
                    runForgeActions(result.getTriggers(), player);
                }
            }
        }
    }

    private static void runForgeActions(List<ForgeActionTrigger> triggers, Player player) {
        for (ForgeActionTrigger trigger : triggers) {
            for (Action action : trigger.getActions()) {
                Map<String, String> commandPlaceholders = new HashMap<>();
                commandPlaceholders.put("name", trigger.getName());
                performActions(action, player, commandPlaceholders,
                        Placeholder.unparsed("player", player.getName()),
                        Placeholder.parsed("name", trigger.getName()));
            }
        }
    }

    private static void performActions(Action action, Player player, Map<String, String> commandPlaceholders,
                                       TagResolver.Single... templates) {
        if (action.hasAlert()) {
            Logs.logComponent(action.getAlert(templates));
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("hackedserver.alert")) {
                    HackedServerPlugin.get().getAudiences().player(admin)
                            .sendMessage(action.getAlert(templates));
                }
            }
        }
        if (player.hasPermission("hackedserver.bypass")) {
            return;
        }

        // Check if player is fully online - if not, defer the actions
        HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
        if (!isPlayerFullyOnline(player)) {
            hackedPlayer.queuePendingAction(() -> executeCommands(action, player, commandPlaceholders));
            return;
        }

        executeCommands(action, player, commandPlaceholders);
    }

    private static boolean isPlayerFullyOnline(Player player) {
        // During login/config phase, the player object exists but isn't fully joined
        // Check if the player can be found in the online players list
        Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
        return onlinePlayer != null && onlinePlayer.isOnline();
    }

    private static void executeCommands(Action action, Player player, Map<String, String> placeholders) {
        long delayTicks = action.getDelayTicks();

        // Schedule the commands with the configured delay
        // This ensures the player is fully connected before executing actions like kick
        Runnable commandRunner = () -> {
            // Re-fetch the player to ensure we have the current online instance
            Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return; // Player logged out before we could execute
            }

            for (String command : action.getConsoleCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        replacePlaceholders(command, onlinePlayer, placeholders));
            }
            for (String command : action.getPlayerCommands()) {
                Bukkit.dispatchCommand(onlinePlayer,
                        replacePlaceholders(command, onlinePlayer, placeholders));
            }
            for (String command : action.getOppedPlayerCommands()) {
                boolean op = onlinePlayer.isOp();
                onlinePlayer.setOp(true);
                Bukkit.dispatchCommand(onlinePlayer,
                        replacePlaceholders(command, onlinePlayer, placeholders));
                onlinePlayer.setOp(op);
            }
        };

        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(HackedServerPlugin.get(), commandRunner, delayTicks);
        } else {
            Bukkit.getScheduler().runTask(HackedServerPlugin.get(), commandRunner);
        }
    }

    private static String replacePlaceholders(String command, Player player, Map<String, String> placeholders) {
        String result = command.replace("<player>", player.getName());
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }
}
