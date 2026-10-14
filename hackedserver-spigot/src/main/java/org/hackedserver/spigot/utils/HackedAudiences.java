package org.hackedserver.spigot.utils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves Adventure audiences for Bukkit senders.
 * <p>
 * Paper (and forks such as Folia/Purpur) implement Adventure natively: every {@link CommandSender}
 * is an {@link Audience}, so messages are sent directly. The shaded adventure-platform-bukkit
 * bridge is only created on servers without native Adventure (Spigot, some hybrids); it relies on
 * CraftBukkit internals and silently drops messages on modern Paper.
 */
public final class HackedAudiences {

    private static final boolean NATIVE_ADVENTURE = hasNativeAdventure();

    @Nullable
    private final BukkitAudiences bridge;

    private HackedAudiences(@Nullable BukkitAudiences bridge) {
        this.bridge = bridge;
    }

    public static HackedAudiences create(Plugin plugin) {
        return new HackedAudiences(NATIVE_ADVENTURE ? null : BukkitAudiences.create(plugin));
    }

    public static boolean isNativeAdventure() {
        return NATIVE_ADVENTURE;
    }

    public @NotNull Audience sender(@NotNull CommandSender sender) {
        if (bridge == null) {
            return (Audience) (Object) sender;
        }
        return bridge.sender(sender);
    }

    public @NotNull Audience player(@NotNull Player player) {
        return sender(player);
    }

    public @NotNull Audience console() {
        return sender(Bukkit.getConsoleSender());
    }

    public void close() {
        if (bridge != null) {
            bridge.close();
        }
    }

    private static boolean hasNativeAdventure() {
        try {
            return Audience.class.isAssignableFrom(CommandSender.class);
        } catch (LinkageError e) {
            return false;
        }
    }
}
