package org.hackedserver.core.utils;

import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.Config;
import org.hackedserver.core.forge.ForgeClientType;
import org.hackedserver.core.forge.ForgeModInfo;
import org.hackedserver.core.lunar.LunarModInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JoinWebhook {

    public static void send(String playerName, UUID playerUuid) {
        if (!Config.JOIN_WEBHOOK_ENABLED.toBool()) {
            return;
        }

        String url = Config.JOIN_WEBHOOK_URL.getStringOrDefault("");
        String content = replacePlaceholders(Config.JOIN_WEBHOOK_CONTENT.getStringOrDefault(""), playerName, playerUuid);
        String title = replacePlaceholders(Config.JOIN_WEBHOOK_EMBED_TITLE.getStringOrDefault("Player Joined"), playerName, playerUuid);
        String description = replacePlaceholders(Config.JOIN_WEBHOOK_EMBED_DESCRIPTION.getStringOrDefault("Player {player} has joined."), playerName, playerUuid);
        int color = (int) Config.JOIN_WEBHOOK_EMBED_COLOR.toLong(65280);
        String footer = replacePlaceholders(Config.JOIN_WEBHOOK_EMBED_FOOTER.getStringOrDefault("HackedServer"), playerName, playerUuid);

        DiscordWebhook.send(url, content, title, description, color, footer);
    }

    private static String replacePlaceholders(String input, String playerName, UUID playerUuid) {
        if (input == null) {
            return "";
        }
        String safeName = playerName != null ? playerName : "Unknown";
        String safeUuid = playerUuid != null ? playerUuid.toString() : "Unknown";
        // Use getPlayerIfPresent to avoid recreating a removed player record.
        // The webhook runs on a delay, so the player may have disconnected by now.
        HackedPlayer hackedPlayer = playerUuid != null ? HackedServer.getPlayerIfPresent(playerUuid) : null;
        String client = getClientName(hackedPlayer);
        String modlist = getModList(hackedPlayer);
        return input.replace("{player}", safeName)
                .replace("{uuid}", safeUuid)
                .replace("{client}", client)
                .replace("{modlist}", modlist);
    }

    private static String getClientName(HackedPlayer hackedPlayer) {
        if (hackedPlayer == null) {
            return "Unknown";
        }
        if (hackedPlayer.isBedrockDetected()) {
            return "Bedrock";
        }
        if (hackedPlayer.hasLunarModsData()) {
            return "Lunar Client";
        }

        ForgeClientType forgeClientType = hackedPlayer.getForgeClientType();
        if (forgeClientType != null) {
            return forgeClientType.getDisplayName();
        }

        return "Unknown";
    }

    private static String getModList(HackedPlayer hackedPlayer) {
        if (hackedPlayer == null) {
            return "Unknown";
        }

        List<String> mods = new ArrayList<>();
        for (LunarModInfo mod : hackedPlayer.getLunarMods()) {
            String displayName = mod.getDisplayName() != null && !mod.getDisplayName().isBlank()
                    ? mod.getDisplayName()
                    : mod.getId();
            if (displayName == null || displayName.isBlank()) {
                continue;
            }
            if (mod.getVersion() != null && !mod.getVersion().isBlank()) {
                mods.add(displayName + " (" + mod.getVersion() + ")");
            } else {
                mods.add(displayName);
            }
        }
        for (ForgeModInfo mod : hackedPlayer.getForgeMods()) {
            mods.add(mod.toString());
        }

        if (!mods.isEmpty()) {
            return String.join(", ", mods);
        }

        if (hackedPlayer.hasLunarModsData() || hackedPlayer.hasForgeModsData()) {
            return "None";
        }

        return "Unknown";
    }
}
