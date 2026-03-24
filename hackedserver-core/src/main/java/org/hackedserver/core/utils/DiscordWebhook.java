package org.hackedserver.core.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class DiscordWebhook {

    // Simple rate limiter: max 25 requests per 60 seconds (Discord limit is ~30)
    private static final int MAX_REQUESTS_PER_WINDOW = 25;
    private static final long WINDOW_MS = 60_000L;
    // Encodes both window start and request count in a single AtomicLong to ensure
    // fully atomic window resets. High 48 bits = timestamp ms (good until year 2861),
    // low 16 bits = request count (max 65535, well above our limit of 25).
    private static final int COUNT_BITS = 16;
    private static final long COUNT_MASK = (1L << COUNT_BITS) - 1;
    private static final AtomicLong windowState = new AtomicLong(pack(System.currentTimeMillis(), 0));

    public static void send(String webhookUrl, String content, String embedTitle, String embedDescription, int embedColor, String embedFooter) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        if (!tryAcquireRateLimit()) {
            System.err.println("[HackedServer] Discord webhook rate limit reached, skipping message");
            return;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(webhookUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "HackedServer-Webhook");
            connection.setDoOutput(true);

            String jsonPayload = buildJsonPayload(content, embedTitle, embedDescription, embedColor, embedFooter);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                System.err.println("[HackedServer] Webhook request failed with status: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static long pack(long timestampMs, int count) {
        return (timestampMs << COUNT_BITS) | (count & COUNT_MASK);
    }

    private static long unpackTimestamp(long packed) {
        return packed >>> COUNT_BITS;
    }

    private static int unpackCount(long packed) {
        return (int) (packed & COUNT_MASK);
    }

    private static boolean tryAcquireRateLimit() {
        long now = System.currentTimeMillis();
        while (true) {
            long current = windowState.get();
            long start = unpackTimestamp(current);
            int count = unpackCount(current);

            long next;
            if (now - start >= WINDOW_MS) {
                // Window expired - reset with count=1 (this request)
                next = pack(now, 1);
            } else if (count < MAX_REQUESTS_PER_WINDOW) {
                // Window active and under limit - increment count
                next = pack(start, count + 1);
            } else {
                // Window active but limit reached
                return false;
            }

            if (windowState.compareAndSet(current, next)) {
                return true;
            }
            // CAS failed, another thread modified state - retry
        }
    }

    private static String buildJsonPayload(String content, String title, String description, int color, String footer) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        if (content != null && !content.isEmpty()) {
            json.append("\"content\": \"").append(escapeJson(content)).append("\",");
        }

        json.append("\"embeds\": [{");
        
        boolean hasField = false;
        if (title != null && !title.isEmpty()) {
            json.append("\"title\": \"").append(escapeJson(title)).append("\"");
            hasField = true;
        }
        
        if (description != null && !description.isEmpty()) {
            if (hasField) json.append(",");
            json.append("\"description\": \"").append(escapeJson(description)).append("\"");
            hasField = true;
        }
        
        if (hasField) json.append(",");
        json.append("\"color\": ").append(color);
        hasField = true;
        
        if (footer != null && !footer.isEmpty()) {
            if (hasField) json.append(",");
            json.append("\"footer\": {\"text\": \"").append(escapeJson(footer)).append("\"}");
        }
        
        json.append("}]");
        json.append("}");
        
        return json.toString();
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    // RFC 8259: all control characters U+0000-U+001F must be escaped
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
