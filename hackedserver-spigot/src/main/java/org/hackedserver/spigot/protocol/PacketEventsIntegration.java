package org.hackedserver.spigot.protocol;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.hackedserver.spigot.HackedServerPlugin;
import org.hackedserver.spigot.listeners.PacketEventsPayloadListener;
import org.hackedserver.spigot.probing.SignTranslationProber;
import org.jetbrains.annotations.Nullable;

/**
 * Isolates PacketEvents references to prevent class loading failures when PacketEvents is not installed.
 * This class should only be loaded when PacketEvents is confirmed to be available.
 */
public final class PacketEventsIntegration {

    private final HackedServerPlugin plugin;
    private final PacketEventsPayloadListener listener;
    @Nullable
    private PacketEventsSignListener signListener;
    private boolean loaded = false;
    private boolean initialized = false;
    private boolean ownedByUs = false;

    public PacketEventsIntegration(HackedServerPlugin plugin) {
        this.plugin = plugin;
        this.listener = new PacketEventsPayloadListener(plugin);
    }

    /**
     * Initialize PacketEvents. Must be called during onLoad() phase.
     * Only initializes if PacketEvents is not already loaded as a standalone plugin.
     */
    public void load() {
        // Check if PacketEvents is already loaded as a plugin (standalone or by another plugin)
        // In that case, we should NOT call setAPI/load - just register our listener in register()
        if (Bukkit.getPluginManager().getPlugin("packetevents") != null) {
            plugin.getLogger().info("PacketEvents is loaded as a standalone plugin, will register listener only");
            ownedByUs = false;
            return;
        }

        // Check if API is already set by another plugin
        try {
            if (PacketEvents.getAPI() != null) {
                plugin.getLogger().info("PacketEvents API already initialized by another plugin, will register listener only");
                ownedByUs = false;
                return;
            }
        } catch (Exception e) {
            // API not set yet, we'll initialize it
        }

        // We're the first to initialize PacketEvents
        plugin.getLogger().info("Initializing PacketEvents API...");
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false);
        PacketEvents.getAPI().load();
        ownedByUs = true;
        loaded = true;
    }

    /**
     * Register the packet listener. Must be called during onEnable() phase after load().
     */
    public void register() {
        // Only call init() if we own the PacketEvents instance
        if (ownedByUs) {
            PacketEvents.getAPI().init();
        }
        // Always register our listener (priority is set in listener constructor)
        PacketEvents.getAPI().getEventManager().registerListener(listener);
        initialized = true;
        plugin.getLogger().info("PacketEvents listener registered successfully");
    }

    /**
     * Whether this integration can be selected as the packet library: our own instance is loaded,
     * or the standalone plugin finished its own initialization.
     */
    public boolean isUsable() {
        try {
            if (PacketEvents.getAPI() == null) {
                return false;
            }
            return ownedByUs ? loaded : PacketEvents.getAPI().isInitialized();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Whether PacketEvents is actually injected and delivering packets: either we initialized it
     * ourselves, or the standalone plugin finished its own initialization.
     * A plugin that failed to enable (e.g. an outdated build on a new Minecraft version) is not ready.
     */
    public boolean isReadyForListeners() {
        try {
            if (PacketEvents.getAPI() == null || PacketEvents.getAPI().getEventManager() == null) {
                return false;
            }
            return ownedByUs ? initialized : PacketEvents.getAPI().isInitialized();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Consume sign probe replies at the packet level. Must be called after the API is ready.
     */
    public void registerSignListener(SignTranslationProber prober) {
        signListener = new PacketEventsSignListener(prober);
        PacketEvents.getAPI().getEventManager().registerListener(signListener);
    }

    /**
     * Unregister the packet listener and terminate PacketEvents if we own it.
     */
    public void unregister() {
        if (signListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(signListener);
            signListener = null;
        }
        if (initialized) {
            // Always unregister our listener to prevent duplicates on reload
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            // Only terminate if we own the PacketEvents instance AND we called init()
            // PacketEvents lifecycle requires: load() -> init() -> terminate()
            // Calling terminate() without init() causes errors
            if (ownedByUs && loaded) {
                PacketEvents.getAPI().terminate();
                loaded = false;
            }
            initialized = false;
        }
    }
}
