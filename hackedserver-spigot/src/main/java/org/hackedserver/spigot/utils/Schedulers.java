package org.hackedserver.spigot.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.hackedserver.spigot.HackedServerPlugin;

import java.util.concurrent.TimeUnit;

/**
 * Schedules tasks on the right thread for the running server.
 * <p>
 * On Folia there is no main thread: global work (console commands) runs on the global region,
 * entity work on the entity's scheduler and block work on the owning region. On Paper/Spigot every
 * variant falls back to the classic Bukkit scheduler. The Folia scheduler API is part of the Paper
 * API since 1.20, so it is only ever invoked when {@link VersionUtil#isFoliaServer()} is true.
 */
public final class Schedulers {

    private Schedulers() {
    }

    /**
     * Runs a task on the global region (Folia) or the main thread.
     */
    public static void runGlobal(Runnable task, long delayTicks) {
        Plugin plugin = HackedServerPlugin.get();
        if (VersionUtil.isFoliaServer()) {
            if (delayTicks > 0) {
                Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduled -> task.run(), delayTicks);
            } else {
                Bukkit.getGlobalRegionScheduler().execute(plugin, task);
            }
            return;
        }
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task immediately when already on the main thread, otherwise on the next tick
     * (always the case on Folia, where global work is handed to the global region).
     */
    public static void runGlobalNow(Runnable task) {
        if (!VersionUtil.isFoliaServer() && Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        runGlobal(task, 0L);
    }

    /**
     * Runs a task on the thread owning the entity (Folia) or the main thread.
     * On Folia the task is silently dropped if the entity is removed before it runs.
     */
    public static void runForEntity(Entity entity, Runnable task, long delayTicks) {
        Plugin plugin = HackedServerPlugin.get();
        if (VersionUtil.isFoliaServer()) {
            entity.getScheduler().execute(plugin, task, null, Math.max(1L, delayTicks));
            return;
        }
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task on the region owning the location (Folia) or the main thread.
     */
    public static void runAtLocation(Location location, Runnable task, long delayTicks) {
        Plugin plugin = HackedServerPlugin.get();
        if (VersionUtil.isFoliaServer()) {
            if (delayTicks > 0) {
                Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduled -> task.run(), delayTicks);
            } else {
                Bukkit.getRegionScheduler().execute(plugin, location, task);
            }
            return;
        }
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task immediately if the current thread owns the location, otherwise on the next tick.
     */
    public static void runAtLocationNow(Location location, Runnable task) {
        boolean ownedThread = VersionUtil.isFoliaServer()
                ? Bukkit.isOwnedByCurrentRegion(location)
                : Bukkit.isPrimaryThread();
        if (ownedThread) {
            task.run();
            return;
        }
        runAtLocation(location, task, 0L);
    }

    /**
     * Runs a task off the server threads.
     */
    public static void runAsync(Runnable task, long delayTicks) {
        Plugin plugin = HackedServerPlugin.get();
        if (VersionUtil.isFoliaServer()) {
            if (delayTicks > 0) {
                Bukkit.getAsyncScheduler().runDelayed(plugin, scheduled -> task.run(),
                        delayTicks * 50L, TimeUnit.MILLISECONDS);
            } else {
                Bukkit.getAsyncScheduler().runNow(plugin, scheduled -> task.run());
            }
            return;
        }
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
}
