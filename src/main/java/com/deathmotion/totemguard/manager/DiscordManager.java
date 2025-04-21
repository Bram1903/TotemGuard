/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Webhooks;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.TpsUtil;
import net.kyori.adventure.text.Component;
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

    public void sendAlert(Check check, Component details) {
        sendWebhook(check, details, false);
    }

    public void sendPunishment(Check check, Component details) {
        sendWebhook(check, details, true);
    }

    private void sendWebhook(Check check, Component details, boolean isPunishment) {
        Webhooks.WebhookSettings settings = isPunishment
                ? plugin.getConfigManager().getWebhooks().getPunishment()
                : plugin.getConfigManager().getWebhooks().getAlert();

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

        TotemPlayer totemPlayer = check.getPlayer();

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setThumbnailUrl("http://cravatar.eu/avatar/" + totemPlayer.getName() + "/64.png")
                .setColor(Color.decode(settings.getColor()).getRGB())
                .setTitle(new WebhookEmbed.EmbedTitle(settings.getTitle(), null))
                .addField(new WebhookEmbed.EmbedField(true, "**Player**", "`" + totemPlayer.getName() + "`"))
                .addField(new WebhookEmbed.EmbedField(true, "**Check**", check.getCheckName()));

        if (!isPunishment) {
            if (check.getCheckSettings().isPunishable()) {
                embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Violations**", "[" + check.getViolations() + "/" + check.getMaxViolations() + "]"));
            } else {
                embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Violations**", String.valueOf(check.getViolations())));
            }

            embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Client Brand**", totemPlayer.getBrand()));
            embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Client Version**", totemPlayer.user.getClientVersion().getReleaseName()));
            embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**Ping**", String.valueOf(totemPlayer.getKeepAlivePing())));
            embedBuilder.addField(new WebhookEmbed.EmbedField(true, "**TPS**", String.format("%.2f", TpsUtil.getInstance().getTps(totemPlayer.bukkitPlayer.getLocation()))));
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "**Details**", "```" + PlainTextComponentSerializer.plainText().serialize(details) + "```"));
        }

        if (settings.isTimestamp()) {
            embedBuilder.setTimestamp(Instant.now());
        }

        if (settings.isFooter()) {
            embedBuilder.setFooter(new WebhookEmbed.EmbedFooter("Server: " + plugin.getConfigManager().getSettings().getServer(), null));
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
