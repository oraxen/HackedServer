package org.hackedserver.spigot.listeners;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingPayloads {

    private static final long MAX_AGE_MS = 30_000L;
    private static final int MAX_PAYLOADS_PER_PLAYER = 256;
    private static final Map<Player, PendingQueue> PENDING = new ConcurrentHashMap<>();

    private PendingPayloads() {
    }

    public static void queue(Player temporaryPlayer, String channel, String message) {
        if (temporaryPlayer == null) {
            return;
        }
        pruneExpired();
        PendingQueue queue = PENDING.computeIfAbsent(temporaryPlayer, key -> new PendingQueue());
        queue.add(channel, message);
    }

    public static List<PendingPayload> drainFor(Player player) {
        if (player == null) {
            return List.of();
        }
        pruneExpired();
        List<PendingPayload> drained = new ArrayList<>();
        for (Map.Entry<Player, PendingQueue> entry : PENDING.entrySet()) {
            Player temporaryPlayer = entry.getKey();
            Player resolved = resolvePlayer(temporaryPlayer);
            if (resolved == null) {
                continue;
            }
            if (resolved.equals(player)) {
                PendingQueue removed = PENDING.remove(temporaryPlayer);
                if (removed != null) {
                    drained.addAll(removed.drain());
                }
            }
        }
        return drained;
    }

    public static void clearFor(Player player) {
        if (player == null) {
            return;
        }
        for (Map.Entry<Player, PendingQueue> entry : PENDING.entrySet()) {
            Player temporaryPlayer = entry.getKey();
            Player resolved = resolvePlayer(temporaryPlayer);
            if (resolved == null) {
                continue;
            }
            if (resolved.equals(player)) {
                PENDING.remove(temporaryPlayer, entry.getValue());
            }
        }
    }

    private static Player resolvePlayer(Player temporaryPlayer) {
        try {
            return temporaryPlayer.getPlayer();
        } catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    private static void pruneExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Player, PendingQueue> entry : PENDING.entrySet()) {
            if (entry.getValue().isExpired(now)) {
                PENDING.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    public static final class PendingPayload {
        private final String channel;
        private final String message;
        private final long receivedAt;

        public PendingPayload(String channel, String message, long receivedAt) {
            this.channel = channel;
            this.message = message;
            this.receivedAt = receivedAt;
        }

        public String getChannel() {
            return channel;
        }

        public String getMessage() {
            return message;
        }

        public long getReceivedAt() {
            return receivedAt;
        }
    }

    private static final class PendingQueue {
        private final Deque<PendingPayload> payloads = new ArrayDeque<>();
        private long lastUpdated = System.currentTimeMillis();

        private synchronized void add(String channel, String message) {
            long now = System.currentTimeMillis();
            if (payloads.size() >= MAX_PAYLOADS_PER_PLAYER) {
                payloads.pollFirst();
            }
            payloads.addLast(new PendingPayload(channel, message, now));
            lastUpdated = now;
        }

        private synchronized List<PendingPayload> drain() {
            if (payloads.isEmpty()) {
                return List.of();
            }
            List<PendingPayload> drained = new ArrayList<>(payloads);
            payloads.clear();
            return drained;
        }

        private synchronized boolean isExpired(long now) {
            // Only expire if time has passed AND queue is empty or stale
            // A newly created empty queue is not expired until MAX_AGE_MS has passed
            return now - lastUpdated > MAX_AGE_MS;
        }
    }
}
