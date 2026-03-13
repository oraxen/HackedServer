package org.hackedserver.core.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    public static void send(String webhookUrl, String content, String embedTitle, String embedDescription, int embedColor, String embedFooter) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
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
