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

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Webhooks;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.TpsUtil;
import com.deathmotion.totemguard.util.webhook.*;
import lombok.Getter;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class DiscordManager {
    private static final Predicate<String> WEBHOOK_REGEX = Pattern.compile("https://discord\\.com/api/webhooks/\\d+/[\\w-]+").asMatchPredicate();
    private static final int MAX_QUEUE_SIZE = 20;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private final TotemGuard plugin;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    private final WebhookClient alertClient;
    private final WebhookClient punishmentClient;

    public DiscordManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "TotemGuard-DiscordWebhookProcessor"));

        Webhooks hooks = plugin.getConfigManager().getWebhooks();
        this.alertClient = new WebhookClient(loadConfig(hooks.getAlert()));
        this.punishmentClient = new WebhookClient(loadConfig(hooks.getPunishment()));

        this.alertClient.start();
        this.punishmentClient.start();
    }

    public void reload() {
        Webhooks hooks = plugin.getConfigManager().getWebhooks();
        this.alertClient.updateConfig(loadConfig(hooks.getAlert()));
        this.punishmentClient.updateConfig(loadConfig(hooks.getPunishment()));
    }

    public void sendAlert(Check check, String detailsPlain) {
        send(alertClient, check, detailsPlain, true);
    }

    public void sendPunishment(Check check, String detailsPlain) {
        send(punishmentClient, check, detailsPlain, false);
    }

    private void send(WebhookClient client, Check check, String detailsPlain, boolean isAlert) {
        if (!client.config.isValid()) return;
        TotemPlayer p = check.getPlayer();

        String violations = check.getCheckSettings().isPunishable()
                ? String.format("[%d/%d]", check.getViolations(), check.getMaxViolations())
                : String.valueOf(check.getViolations());

        Embed embed = new Embed("")
                .title(client.config.getTitle())
                .color(client.config.getColor())
                .thumbnailURL("https://crafthead.net/helm/" + p.getUniqueId());

        embed.addFields(
                new EmbedField("**Player**", "`" + p.getName() + "`", true),
                new EmbedField("**Check**", check.getCheckName(), true)
        );

        if (isAlert) {
            embed.addFields(
                    new EmbedField("**Violations**", violations, true),
                    new EmbedField("**Brand**", p.getBrand(), true),
                    new EmbedField("**Version**", p.user.getClientVersion().getReleaseName(), true),
                    new EmbedField("**Ping**", "(K: " + p.getKeepAlivePing() + " | T: " + p.pingData.getTransactionPing() + ")", true),
                    new EmbedField("**TPS**", String.format("%.2f", TpsUtil.getInstance().getTps(p.bukkitPlayer.getLocation())), true)
            );

            if (!detailsPlain.isEmpty()) {
                embed.addFields(
                        new EmbedField("**Details**", "```" + detailsPlain + "```", false)
                );
            }
        }

        if (client.config.isTimestamp()) {
            embed.timestamp(Instant.now());
        }
        if (client.config.isFooter()) {
            embed.footer(new EmbedFooter(
                    "Server: " + plugin.getConfigManager().getSettings().getServer()
            ));
        }

        WebhookMessage msg = new WebhookMessage()
                .username(client.config.getUsername())
                .avatar(client.config.getAvatarUrl())
                .addEmbeds(embed);

        client.enqueue(buildRequest(msg, client.config.getUri()));
    }

    private HttpRequest buildRequest(WebhookMessage message, URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .timeout(HTTP_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(
                        message.toJson().toString()))
                .build();
    }

    private WebhookConfig loadConfig(Webhooks.WebhookSettings src) {
        WebhookConfig cfg = new WebhookConfig();
        if (!src.isEnabled()) return cfg;

        String url = src.getUrl();
        if (!WEBHOOK_REGEX.test(url)) {
            plugin.getLogger().warning("Invalid webhook URL: " + url);
            return cfg;
        }

        try {
            cfg.setUri(new URI(url));
            cfg.setUsername(src.getName());
            cfg.setAvatarUrl(src.getProfileImage());
            cfg.setTitle(src.getTitle());
            cfg.setColor(Color.decode(src.getColor()).getRGB());
            cfg.setTimestamp(src.isTimestamp());
            cfg.setFooter(src.isFooter());
            cfg.setValid(true);
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "Failed to parse webhook settings: " + e.getMessage()
            );
        }

        return cfg;
    }

    private class WebhookClient {
        private final LinkedBlockingDeque<HttpRequest> queue = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
        private final AtomicBoolean sending = new AtomicBoolean(false);
        private final AtomicLong rateLimitedUntil = new AtomicLong(0);
        private final AtomicBoolean started = new AtomicBoolean(false);

        @Getter
        private WebhookConfig config;

        WebhookClient(WebhookConfig initial) {
            this.config = initial;
        }

        void updateConfig(WebhookConfig newCfg) {
            this.config = newCfg;
            queue.clear();
        }

        void enqueue(HttpRequest req) {
            if (!queue.offerLast(req)) {
                queue.pollFirst();
                queue.offerLast(req);
                plugin.getLogger().warning(
                        "Discord queue full. Dropped oldest request."
                );
            }
        }

        void start() {
            if (started.compareAndSet(false, true)) {
                scheduler.scheduleAtFixedRate(this::processQueue, 0, 1, TimeUnit.SECONDS);
            }
        }

        private void processQueue() {
            long now = System.currentTimeMillis();
            if (now < rateLimitedUntil.get()) return;
            if (!sending.compareAndSet(false, true)) return;

            HttpRequest req = queue.peekFirst();
            if (req == null) {
                sending.set(false);
                return;
            }

            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                try {
                    if (err != null) {
                        plugin.getLogger().warning("Webhook send failed: " + err.getMessage());
                    } else if (resp.statusCode() == 429) {
                        resp.headers().firstValue("X-RateLimit-Reset").ifPresent(r -> rateLimitedUntil.set(Long.parseLong(r) * 1000L));
                    } else if (resp.statusCode() >= 400) {
                        plugin.getLogger().warning("Discord webhook error " + resp.statusCode() + ": " + resp.body());
                    }
                } finally {
                    if (err != null || resp == null || resp.statusCode() != 429) {
                        queue.pollFirst();
                    }
                    sending.set(false);
                }
            });
        }
    }
}
