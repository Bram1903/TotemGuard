/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.common.discord;

import com.deathmotion.totemguard.api3.config.key.DiscordKeys;
import com.deathmotion.totemguard.api3.reload.Reloadable;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.config.schema.WebhookConfig;
import com.deathmotion.totemguard.common.config.schema.WebhookField;
import com.deathmotion.totemguard.common.config.view.DiscordView;
import com.deathmotion.totemguard.common.discord.webhook.*;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class DiscordWebhookService implements Reloadable {

    private static final Predicate<String> WEBHOOK_REGEX =
            Pattern.compile("https://(?:ptb\\.|canary\\.)?discord(?:app)?\\.com/api/webhooks/\\d+/[\\w-]+")
                    .asMatchPredicate();

    private static final int MAX_QUEUE_SIZE = 20;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final String UNSPECIFIED_DEBUG = "Not specified";

    private final TGPlatform platform;
    private final ConfigRepositoryImpl configRepository;
    private final PlaceholderRepositoryImpl placeholderRepository;

    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    private final WebhookChannel alertChannel;
    private final WebhookChannel punishmentChannel;

    public DiscordWebhookService() {
        this.platform = TGPlatform.getInstance();
        this.configRepository = platform.getConfigRepository();
        this.placeholderRepository = platform.getPlaceholderRepository();
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread t = new Thread(r, "TotemGuard-DiscordWebhook");
                    t.setDaemon(true);
                    return t;
                });
        this.alertChannel = new WebhookChannel(DiscordKeys.ALERTS_PREFIX);
        this.punishmentChannel = new WebhookChannel(DiscordKeys.PUNISHMENTS_PREFIX);

        reload();

        this.alertChannel.start();
        this.punishmentChannel.start();
    }

    @Override
    public void reload() {
        DiscordView view = configRepository.discord();
        alertChannel.updateConfig(loadChannelConfig(view.webhook(DiscordKeys.ALERTS_PREFIX), DiscordKeys.ALERTS_PREFIX));
        punishmentChannel.updateConfig(loadChannelConfig(view.webhook(DiscordKeys.PUNISHMENTS_PREFIX), DiscordKeys.PUNISHMENTS_PREFIX));
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public void sendAlert(@NotNull CheckImpl check, int violations, @Nullable String debug) {
        dispatch(alertChannel, check, violations, debug);
    }

    public void sendPunishment(@NotNull CheckImpl check, @Nullable String debug) {
        dispatch(punishmentChannel, check, check.getViolations(), debug);
    }

    private void dispatch(WebhookChannel channel, CheckImpl check, int violations, @Nullable String debug) {
        ChannelConfig cfg = channel.config;
        if (cfg == null || !cfg.valid()) return;

        TGPlayer player = check.player;
        Map<String, Object> extras = Map.of(
                "tg_check_violations", violations,
                "tg_check_debug", debug == null ? UNSPECIFIED_DEBUG : debug
        );

        Function<String, String> resolver = key -> resolvePlaceholder(key, player, check, extras);

        Embed embed = new Embed(render(cfg.description, resolver))
                .title(cfg.title)
                .color(cfg.color);

        if (!cfg.thumbnail.isBlank()) {
            String thumbnail = placeholderRepository.replace(cfg.thumbnail, player, check, extras);
            if (!thumbnail.isBlank()) embed.thumbnailURL(thumbnail);
        }

        if (cfg.fields.length > 0) {
            EmbedField[] fields = new EmbedField[cfg.fields.length];
            for (int i = 0; i < cfg.fields.length; i++) {
                CompiledField cf = cfg.fields[i];
                fields[i] = new EmbedField(
                        render(cf.name, resolver),
                        render(cf.value, resolver),
                        cf.inline
                );
            }
            embed.fields(fields);
        }

        if (cfg.timestamp) embed.timestamp(Instant.now());

        if (cfg.footer != null) {
            String footerText = render(cfg.footer, resolver);
            if (!footerText.isBlank()) {
                embed.footer(new EmbedFooter(footerText));
            }
        }

        WebhookMessage msg = new WebhookMessage()
                .username(cfg.username)
                .avatar(cfg.avatar.isBlank() ? null : cfg.avatar)
                .addEmbeds(embed);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(cfg.uri)
                .header("Content-Type", "application/json")
                .timeout(HTTP_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(msg.toJson().toString()))
                .build();

        channel.enqueue(request);
    }

    private String render(CompiledDiscordTemplate template, Function<String, String> resolver) {
        if (template == null) return "";
        return template.render(resolver);
    }

    private @Nullable String resolvePlaceholder(String key, TGPlayer player, CheckImpl check, Map<String, Object> extras) {
        String probe = "%" + key + "%";
        String out = placeholderRepository.replace(probe, player, check, extras);
        return probe.equals(out) ? null : out;
    }

    private ChannelConfig loadChannelConfig(WebhookConfig cfg, String prefix) {
        if (!cfg.enabled()) return ChannelConfig.disabled();

        if (!WEBHOOK_REGEX.test(cfg.url())) {
            platform.getLogger().warning("[Discord] Invalid webhook URL for '" + prefix + "': " + cfg.url());
            return ChannelConfig.disabled();
        }

        URI uri;
        try {
            uri = URI.create(cfg.url());
        } catch (IllegalArgumentException e) {
            platform.getLogger().warning("[Discord] Failed to parse webhook URL for '" + prefix + "': " + e.getMessage());
            return ChannelConfig.disabled();
        }

        Integer color = parseColor(cfg.color());
        if (color == null) {
            platform.getLogger().warning("[Discord] Invalid hex color for '" + prefix + "', falling back to default.");
            color = 0xd9b61a;
        }

        List<CompiledField> compiled = compileFields(cfg.fields());
        CompiledDiscordTemplate footer = cfg.footer().isBlank() ? null : CompiledDiscordTemplate.compile(cfg.footer());

        return new ChannelConfig(
                true,
                uri,
                cfg.username(),
                cfg.avatar(),
                cfg.title(),
                color,
                cfg.timestamp(),
                cfg.thumbnail(),
                CompiledDiscordTemplate.compile(""),
                footer,
                compiled.toArray(CompiledField[]::new)
        );
    }

    private List<CompiledField> compileFields(List<WebhookField> fields) {
        if (fields.isEmpty()) return List.of();
        int max = Math.min(fields.size(), Embed.MAX_FIELDS);
        List<CompiledField> out = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            WebhookField f = fields.get(i);
            out.add(new CompiledField(
                    CompiledDiscordTemplate.compile(f.name()),
                    CompiledDiscordTemplate.compile(f.value()),
                    f.inline()
            ));
        }
        return out;
    }

    private @Nullable Integer parseColor(String hex) {
        if (hex == null || hex.isBlank()) return null;
        String trimmed = hex.trim();
        if (trimmed.startsWith("#")) trimmed = trimmed.substring(1);
        try {
            return Integer.parseInt(trimmed, 16) & 0xffffff;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record CompiledField(CompiledDiscordTemplate name, CompiledDiscordTemplate value, boolean inline) {
    }

    private record ChannelConfig(
            boolean valid,
            URI uri,
            String username,
            String avatar,
            String title,
            int color,
            boolean timestamp,
            String thumbnail,
            CompiledDiscordTemplate description,
            @Nullable CompiledDiscordTemplate footer,
            CompiledField[] fields
    ) {
        static ChannelConfig disabled() {
            return new ChannelConfig(false, null, null, null, null, 0, false, "", null, null, new CompiledField[0]);
        }
    }

    private final class WebhookChannel {
        private final String prefix;
        private final LinkedBlockingDeque<HttpRequest> queue = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
        private final AtomicBoolean sending = new AtomicBoolean(false);
        private final AtomicLong rateLimitedUntil = new AtomicLong(0);
        private final AtomicBoolean started = new AtomicBoolean(false);

        private volatile ChannelConfig config = ChannelConfig.disabled();

        WebhookChannel(String prefix) {
            this.prefix = prefix;
        }

        void updateConfig(ChannelConfig newCfg) {
            this.config = newCfg;
            queue.clear();
        }

        void enqueue(HttpRequest req) {
            if (!queue.offerLast(req)) {
                queue.pollFirst();
                queue.offerLast(req);
                platform.getLogger().warning("[Discord] Queue full for '" + prefix + "'. Dropped oldest request.");
            }
        }

        void start() {
            if (started.compareAndSet(false, true)) {
                scheduler.scheduleAtFixedRate(this::processQueue, 1L, 1L, TimeUnit.SECONDS);
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
                        platform.getLogger().log(Level.WARNING, "[Discord] Send failed for '" + prefix + "': " + err.getMessage());
                    } else if (resp.statusCode() == 429) {
                        resp.headers().firstValue("X-RateLimit-Reset")
                                .ifPresent(r -> {
                                    try {
                                        rateLimitedUntil.set((long) (Double.parseDouble(r) * 1000L));
                                    } catch (NumberFormatException ignored) {
                                        // fall back to one-second backoff
                                        rateLimitedUntil.set(System.currentTimeMillis() + 1000L);
                                    }
                                });
                    } else if (resp.statusCode() >= 400) {
                        platform.getLogger().warning("[Discord] Webhook error " + resp.statusCode()
                                + " for '" + prefix + "': " + resp.body());
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
