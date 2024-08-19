package net.strealex.totemguard.manager;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.config.Settings;
import net.strealex.totemguard.data.CheckDetails;
import net.strealex.totemguard.data.TotemPlayer;

import java.awt.*;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordManager {
    public static final Pattern WEBHOOK_PATTERN = Pattern.compile("(?:https?://)?(?:\\w+\\.)?\\w+\\.\\w+/api(?:/v\\d+)?/webhooks/(\\d+)/([\\w-]+)(?:/(?:\\w+)?)?");
    private final TotemGuard plugin;

    public DiscordManager(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public void sendAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        sendWebhook(totemPlayer, checkDetails, plugin.getConfigManager().getSettings().getWebhook(), false);
    }

    public void sendPunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        sendWebhook(totemPlayer, checkDetails, plugin.getConfigManager().getSettings().getWebhook(), true);
    }

    private void sendWebhook(TotemPlayer totemPlayer, CheckDetails checkDetails, Settings.Webhook settings, boolean isPunishment) {
        if (!settings.isEnabled()) return;

        Matcher matcher = WEBHOOK_PATTERN.matcher(settings.getUrl());
        if (!matcher.matches()) {
            plugin.getLogger().warning("Invalid webhook URL! Please check your configuration.");
            return;
        }

        WebhookClient client = WebhookClient.withId(Long.parseLong(matcher.group(1)), matcher.group(2));
        WebhookMessage embed = isPunishment ? createPunishmentWebhook(totemPlayer, checkDetails, settings) : createAlertWebhook(totemPlayer, checkDetails, settings);
        client.setTimeout(15000);

        try {
            client.send(embed);
        } catch (Exception ignored) {
            plugin.getLogger().warning("Failed to send webhook message!\n" + embed.getBody());
        }
    }

    private WebhookMessage createAlertWebhook(TotemPlayer totemPlayer, CheckDetails checkDetails, Settings.Webhook settings) {
        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setUsername(settings.getName())
                .setAvatarUrl(settings.getProfileImage());

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setThumbnailUrl("http://cravatar.eu/avatar/" + totemPlayer.getUsername() + "/64.png")
                .setColor(Color.decode(settings.getColor()).getRGB())
                .setTitle(new WebhookEmbed.EmbedTitle(settings.getTitle(), null))
                .addField(new WebhookEmbed.EmbedField(true, "**Player**", totemPlayer.getUsername()))
                .addField(new WebhookEmbed.EmbedField(true, "**Check**", checkDetails.getCheckName()))
                .addField(new WebhookEmbed.EmbedField(true, "**Violations**", String.valueOf(checkDetails.getViolations())))
                .addField(new WebhookEmbed.EmbedField(true, "**Client Brand**", totemPlayer.getClientBrandName()))
                .addField(new WebhookEmbed.EmbedField(true, "**Client Version**", totemPlayer.getClientVersion().getReleaseName()))
                .addField(new WebhookEmbed.EmbedField(true, "**Ping**", String.valueOf(checkDetails.getPing())))
                .addField(new WebhookEmbed.EmbedField(true, "**TPS**", String.valueOf(checkDetails.getTps())));

        String serializedDetails = PlainTextComponentSerializer.plainText().serialize(checkDetails.getDetails());
        embedBuilder.addField(new WebhookEmbed.EmbedField(false, "**Details**", "```" + serializedDetails + "```"));

        if (settings.isTimestamp()) {
            embedBuilder.setTimestamp(Instant.now());
        }

        messageBuilder.addEmbeds(embedBuilder.build());

        return messageBuilder.build();
    }

    private WebhookMessage createPunishmentWebhook(TotemPlayer totemPlayer, CheckDetails checkDetails, Settings.Webhook settings) {
        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setUsername(settings.getName())
                .setAvatarUrl(settings.getProfileImage());

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setThumbnailUrl("http://cravatar.eu/avatar/" + totemPlayer.getUsername() + "/64.png")
                .setColor(Color.decode(settings.getPunishmentWebhook().getPunishmentColor()).getRGB())
                .setTitle(new WebhookEmbed.EmbedTitle(settings.getPunishmentWebhook().getPunishmentTitle(), null))
                .addField(new WebhookEmbed.EmbedField(true, "**Player**", totemPlayer.getUsername()))
                .addField(new WebhookEmbed.EmbedField(true, "**Check**", checkDetails.getCheckName()));

        if (settings.isTimestamp()) {
            embedBuilder.setTimestamp(Instant.now());
        }

        messageBuilder.addEmbeds(embedBuilder.build());

        return messageBuilder.build();
    }
}
