package de.outdev.totemguard.discord;

import de.outdev.totemguard.TotemGuard;
import okhttp3.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DiscordWebhook {

    private static final OkHttpClient httpClient = new OkHttpClient();

    public static void sendWebhook(Map<String, String> placeholders) {
        if (!(TotemGuard.getConfiguration().getBoolean("webhook.enabled"))) return;

        try {
            sendWebhook(
                    getWebhookUrl(),
                    getWebhookName(),
                    getWebhookColor(),
                    getWebhookTitle(),
                    getWebhookDescription(),
                    getWebhookImage(),
                    getWebhookProfileImage(),
                    isWebhookTimestampEnabled(),
                    placeholders
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendWebhook(String url, String name, String color, String title, List<String> descriptionLines, String image, String profileImage, boolean timestamp, Map<String, String> placeholders) throws IOException {
        List<String> replacedDescriptionLines = Placeholders.replacePlaceholders(descriptionLines, placeholders);
        String replacedDescription = Placeholders.joinMessages(replacedDescriptionLines);


        JSONObject json = new JSONObject();
        json.put("username", name);
        json.put("avatar_url", profileImage);

        JSONObject embed = new JSONObject();
        embed.put("title", title);
        embed.put("description", replacedDescription);
        embed.put("color", Integer.parseInt(color.replace("#", ""), 16));
        embed.put("thumbnail", new JSONObject().put("url", image));


        if (timestamp) {
            embed.put("timestamp", Instant.now().toString());
        }

        json.put("embeds", new JSONArray().add(embed));

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
        }
    }

    private static FileConfiguration getConfig() {
        return TotemGuard.getConfiguration();
    }

    private static String getWebhookUrl() {
        return getConfig().getString("webhook.url");
    }

    private static String getWebhookName() {
        return getConfig().getString("webhook.name");
    }

    private static String getWebhookColor() {
        return getConfig().getString("webhook.color");
    }

    private static String getWebhookTitle() {
        return getConfig().getString("webhook.title");
    }

    private static List<String> getWebhookDescription() {
        return getConfig().getStringList("webhook.description");
    }

    private static String getWebhookImage() {
        return getConfig().getString("webhook.image");
    }

    private static String getWebhookProfileImage() {
        return getConfig().getString("webhook.profile_image");
    }


    private static boolean isWebhookTimestampEnabled() {
        return getConfig().getBoolean("webhook.timestamp");
    }

}

