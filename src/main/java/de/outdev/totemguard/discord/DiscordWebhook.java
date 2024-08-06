package de.outdev.totemguard.discord;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.config.Settings;
import okhttp3.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DiscordWebhook {
    private static final Settings settings = TotemGuard.getInstance().configManager.getSettings();
    private static final OkHttpClient httpClient = new OkHttpClient();

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

        String jsonString = json.toString();

        RequestBody body = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
        }
    }
}

