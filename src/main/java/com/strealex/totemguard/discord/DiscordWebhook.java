package com.strealex.totemguard.discord;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.config.Settings;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DiscordWebhook {

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Blocking
    public static void sendWebhook(Map<String, String> placeholders, @NotNull String details) {
        Settings settings = TotemGuard.getInstance().getConfigManager().getSettings();
        Settings.Webhook webhookConfig = settings.getWebhook();

        String replacedDescription = getReplacedDescription(webhookConfig, placeholders);
        String replacedImage = getReplacedImage(webhookConfig, placeholders);

        JSONObject json = createWebhookPayload(webhookConfig, replacedDescription, replacedImage, details);

        HttpRequest request = buildHttpRequest(webhookConfig, json);

        HttpResponse<String> response = sendHttpRequest(request);
        validateResponse(response);
    }

    private static String getReplacedDescription(Settings.Webhook webhookConfig, Map<String, String> placeholders) {
        List<String> descriptionLines = Placeholders.replacePlaceholders(List.of(webhookConfig.getDescription()), placeholders);
        return Placeholders.joinMessages(descriptionLines);
    }

    private static String getReplacedImage(Settings.Webhook webhookConfig, Map<String, String> placeholders) {
        return Placeholders.replacePlaceholders(Collections.singletonList(webhookConfig.getImage()), placeholders).get(0);
    }

    private static JSONObject createWebhookPayload(Settings.Webhook webhookConfig, String description, String imageUrl, String details) {
        JSONObject json = new JSONObject();
        json.put("username", webhookConfig.getName());
        json.put("avatar_url", webhookConfig.getProfileImage());

        JSONObject embed = new JSONObject();
        embed.put("title", webhookConfig.getTitle());
        embed.put("description", description);
        embed.put("color", parseColor(webhookConfig.getColor()));
        embed.put("thumbnail", new JSONObject().put("url", imageUrl));

        if (webhookConfig.isTimestamp()) {
            embed.put("timestamp", Instant.now().toString());
        }

        // Add a code block with the details string
        JSONArray fieldsArray = new JSONArray();
        JSONObject detailsField = new JSONObject();
        detailsField.put("name", "Details");
        detailsField.put("value", "```" + details + "```");
        detailsField.put("inline", false);  // Set to false to make sure it's not inline with other fields
        fieldsArray.add(detailsField);

        embed.put("fields", fieldsArray);

        // Create a JSONArray and use add method
        JSONArray embedsArray = new JSONArray();
        embedsArray.add(embed); // Correctly use add to add the embed object to the JSONArray

        json.put("embeds", embedsArray); // Add the array to the JSON payload

        return json;
    }


    private static int parseColor(String color) {
        return Integer.parseInt(color.replace("#", ""), 16);
    }

    private static HttpRequest buildHttpRequest(Settings.Webhook webhookConfig, JSONObject json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(webhookConfig.getUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toJSONString()))
                .build();
    }

    private static HttpResponse<String> sendHttpRequest(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            TotemGuard.getInstance().getLogger().warning("Failed to send Webhook request");
            return null;
        }
    }

    private static void validateResponse(HttpResponse<String> response) {
        if (response == null) {
            TotemGuard.getInstance().getLogger().warning("Failed to send webhook, response is null");
        }

        int statusCode = response.statusCode();
        if (statusCode != 200 && statusCode != 204) {
            TotemGuard.getInstance().getLogger().warning("Failed to send webhook, status code: " + statusCode);
        }
    }
}
