/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.manager;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.models.checks.CheckDetails;
import com.deathmotion.totemguard.models.TotemPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.awt.*;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordManager {
    private static final Pattern WEBHOOK_PATTERN = Pattern.compile("(?:https?://)?(?:\\w+\\.)?\\w+\\.\\w+/api(?:/v\\d+)?/webhooks/(\\d+)/([\\w-]+)(?:/(?:\\w+)?)?");
    private final TotemGuard plugin;

    public DiscordManager(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public void sendAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        final Settings.Webhook.WebhookSettings settings = plugin.getConfigManager().getSettings().getWebhook().getAlert();
        sendWebhook(totemPlayer, checkDetails, settings, false);
    }

    public void sendPunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        final Settings.Webhook.WebhookSettings settings = plugin.getConfigManager().getSettings().getWebhook().getPunishment();
        sendWebhook(totemPlayer, checkDetails, settings, true);
    }

    private void sendWebhook(TotemPlayer totemPlayer, CheckDetails checkDetails, Settings.Webhook.WebhookSettings settings, boolean isPunishment) {
        if (!settings.isEnabled()) {
            return;
        }

        Matcher matcher = WEBHOOK_PATTERN.matcher(settings.getUrl());
        if (!matcher.matches()) {
            plugin.getLogger().warning("Invalid webhook URL! Please check your configuration.");
            return;
        }

        WebhookClient client = WebhookClient.withId(Long.parseLong(matcher.group(1)), matcher.group(2));
        client.setTimeout(15000);

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setUsername(settings.getName())
                .setAvatarUrl(settings.getProfileImage());

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setThumbnailUrl("http://cravatar.eu/avatar/" + totemPlayer.username() + "/64.png")
                .setColor(Color.decode(settings.getColor()).getRGB())
                .setTitle(new WebhookEmbed.EmbedTitle(settings.getTitle(), null))
                .addField(new WebhookEmbed.EmbedField(true, "**Player**", "`" + totemPlayer.username() + "`"))
                .addField(new WebhookEmbed.EmbedField(true, "**Check**", checkDetails.getCheckName()));

        if (!isPunishment) {
            if (checkDetails.isPunishable()) {
                embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Violations**", "[" + checkDetails.getViolations() + "/" + checkDetails.getMaxViolations() + "]"));
            } else {
                embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Violations**", String.valueOf(checkDetails.getViolations())));
            }

            embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Client Brand**", totemPlayer.clientBrand()));
            embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Client Version**", totemPlayer.clientVersion().getReleaseName()));
            embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Ping**", String.valueOf(checkDetails.getPing())));
            embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**TPS**", String.valueOf(checkDetails.getTps())));
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "**Details**", "```" + PlainTextComponentSerializer.plainText().serialize(checkDetails.getDetails()) + "```"));
        }

        if (settings.isTimestamp()) {
            embedBuilder.setTimestamp(Instant.now());
        }

        messageBuilder.addEmbeds(embedBuilder.build());
        WebhookMessage message = messageBuilder.build();

        try {
            client.send(message);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send webhook message!\n" + message.getBody());
        }
    }
}