package org.hackedserver.core.forge;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses the minecraft:register payload to extract Forge/NeoForge mod IDs.
 *
 * The minecraft:register packet contains a null-separated list of channel identifiers
 * in the format "namespace:path". The namespace portion reveals the mod ID that
 * registered the channel.
 */
public final class ForgeChannelParser {

    public static final String REGISTER_CHANNEL = "minecraft:register";
    public static final String BRAND_CHANNEL = "minecraft:brand";

    /**
     * Built-in namespaces that should not be considered as mods.
     */
    private static final Set<String> BUILTIN_NAMESPACES = Set.of(
            "minecraft",
            "neoforge",
            "forge",
            "fml",
            "c"  // common namespace used by NeoForge
    );

    private ForgeChannelParser() {
    }

    /**
     * Parses the brand message to determine if the client is using Forge/NeoForge.
     *
     * @param message the brand message content
     * @return the client type, or null if not a Forge/NeoForge client
     */
    public static ForgeClientType parseClientType(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        String lower = message.toLowerCase(Locale.ROOT).trim();
        if (lower.contains("neoforge")) {
            return ForgeClientType.NEOFORGE;
        }
        if (lower.contains("forge") || lower.contains("fml")) {
            return ForgeClientType.FORGE;
        }
        return null;
    }

    /**
     * Parses the minecraft:register payload to extract mod IDs from channel namespaces.
     *
     * @param message the register payload (null-separated channel list or space-separated for debug)
     * @return list of detected forge mods
     */
    public static List<ForgeModInfo> parseRegisteredChannels(String message) {
        if (message == null || message.isEmpty()) {
            return List.of();
        }

        Set<String> namespaces = new HashSet<>();
        List<ForgeModInfo> mods = new ArrayList<>();

        // Parse channels - can be null-separated (raw bytes) or space-separated (debug output)
        String[] channels;
        if (message.contains("\0")) {
            channels = message.split("\0");
        } else {
            channels = message.split("\\s+");
        }

        for (String channel : channels) {
            channel = channel.trim();
            if (channel.isEmpty()) {
                continue;
            }

            // Extract namespace from namespace:path format
            int colonIndex = channel.indexOf(':');
            if (colonIndex <= 0) {
                continue;
            }

            String namespace = channel.substring(0, colonIndex).toLowerCase(Locale.ROOT);

            // Skip built-in namespaces
            if (BUILTIN_NAMESPACES.contains(namespace)) {
                continue;
            }

            // Only add each namespace once
            if (namespaces.add(namespace)) {
                mods.add(new ForgeModInfo(namespace));
            }
        }

        return mods;
    }

    /**
     * Parses raw bytes from the register payload.
     *
     * @param data raw bytes from the packet
     * @return list of detected forge mods
     */
    public static List<ForgeModInfo> parseRegisteredChannels(byte[] data) {
        if (data == null || data.length == 0) {
            return List.of();
        }
        return parseRegisteredChannels(new String(data, StandardCharsets.UTF_8));
    }

    /**
     * Checks if the given namespace is a built-in namespace.
     */
    public static boolean isBuiltinNamespace(String namespace) {
        return namespace != null && BUILTIN_NAMESPACES.contains(namespace.toLowerCase(Locale.ROOT));
    }
}
