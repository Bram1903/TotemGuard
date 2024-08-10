package com.strealex.totemguard.discord;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.config.Settings;
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
    private static final Settings settings = TotemGuard.getInstance().getConfigManager().getSettings();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void sendWebhook(Map<String, String> placeholders) {
        if (!settings.getWebhook().isEnabled()) return;

        try {
            sendWebhook(
                    settings.getWebhook().getUrl(),
                    settings.getWebhook().getName(),
                    settings.getWebhook().getColor(),
                    settings.getWebhook().getTitle(),
                    List.of(settings.getWebhook().getDescription()),
                    settings.getWebhook().getImage(),
                    settings.getWebhook().getProfileImage(),
                    settings.getWebhook().isTimestamp(),
                    placeholders
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendWebhook(String url, String name, String color, String title, List<String> descriptionLines, String image, String profileImage, boolean timestamp, Map<String, String> placeholders) throws IOException {
        List<String> replacedDescriptionLines = Placeholders.replacePlaceholders(descriptionLines, placeholders);
        String replacedDescription = Placeholders.joinMessages(replacedDescriptionLines);
        String replacedImage = Placeholders.replacePlaceholders(Collections.singletonList(image), placeholders).get(0);

        JSONObject json = new JSONObject();
        json.put("username", name);
        json.put("avatar_url", profileImage);

        JSONObject embed = new JSONObject();
        embed.put("title", title);
        embed.put("description", replacedDescription);
        embed.put("color", Integer.parseInt(color.replace("#", ""), 16));
        embed.put("thumbnail", new JSONObject().put("url", replacedImage));

        if (timestamp) {
            embed.put("timestamp", Instant.now().toString());
        }

        JSONArray embeds = new JSONArray();
        embeds.add(embed);
        json.put("embeds", embeds);

        String jsonString = json.toJSONString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                .build();

        HttpResponse<String> response = null;

        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new IOException("Unexpected response code " + response.statusCode() + ": " + response.body());
        }
    }
}

