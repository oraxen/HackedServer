package org.hackedserver.spigot.protocol;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.hackedserver.spigot.HackedServerPlugin;
import org.hackedserver.spigot.listeners.PacketEventsPayloadListener;

/**
 * Isolates PacketEvents references to prevent class loading failures when PacketEvents is not installed.
 * This class should only be loaded when PacketEvents is confirmed to be available.
 */
public final class PacketEventsIntegration {

    private final HackedServerPlugin plugin;
    private final PacketEventsPayloadListener listener;
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
    }

    /**
     * Register the packet listener. Must be called during onEnable() phase after load().
     */
    public void register() {
        // Only call init() if we own the PacketEvents instance
        if (ownedByUs) {
            PacketEvents.getAPI().init();
        }
        // Always register our listener
        PacketEvents.getAPI().getEventManager().registerListener(listener, PacketListenerPriority.NORMAL);
        initialized = true;
        plugin.getLogger().info("PacketEvents listener registered successfully");
    }

    /**
     * Unregister the packet listener and terminate PacketEvents if we own it.
     */
    public void unregister() {
        if (initialized) {
            // Only terminate if we own the PacketEvents instance
            // Otherwise the standalone plugin will handle termination
            if (ownedByUs) {
                PacketEvents.getAPI().terminate();
            }
            initialized = false;
        }
    }
}
