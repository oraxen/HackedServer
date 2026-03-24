package org.hackedserver.core.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class DiscordWebhook {

    // Simple rate limiter: max 25 requests per 60 seconds (Discord limit is ~30)
    private static final int MAX_REQUESTS_PER_WINDOW = 25;
    private static final long WINDOW_MS = 60_000L;
    private static final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
    private static final AtomicInteger requestCount = new AtomicInteger(0);

    public static void send(String webhookUrl, String content, String embedTitle, String embedDescription, int embedColor, String embedFooter) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        if (!tryAcquireRateLimit()) {
            System.err.println("[HackedServer] Discord webhook rate limit reached, skipping message");
            return;
        }

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean tryAcquireRateLimit() {
        long now = System.currentTimeMillis();
        while (true) {
            long start = windowStart.get();
            if (now - start >= WINDOW_MS) {
                if (windowStart.compareAndSet(start, now)) {
                    requestCount.set(1);
                    return true;
                }
                // Another thread reset the window; retry with updated values
                continue;
            }
            return requestCount.incrementAndGet() <= MAX_REQUESTS_PER_WINDOW;
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
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
