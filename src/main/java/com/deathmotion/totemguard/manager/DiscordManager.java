package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Webhooks;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.TpsUtil;
import com.deathmotion.totemguard.util.webhook.Embed;
import com.deathmotion.totemguard.util.webhook.EmbedField;
import com.deathmotion.totemguard.util.webhook.EmbedFooter;
import com.deathmotion.totemguard.util.webhook.WebhookMessage;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Pattern;

// Big thanks to GrimAnticheat for the original DiscordManager implementation
public class DiscordManager {
    private static final Predicate<String> WEBHOOK_REGEX = Pattern.compile("https://discord\\.com/api/webhooks/\\d+/[\\w-]+").asMatchPredicate();

    private static final int MAX_QUEUE_SIZE = 20;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private static final LinkedBlockingDeque<HttpRequest> REQUEST_QUEUE = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);

    private static final AtomicBoolean SCHEDULER_STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean SENDING = new AtomicBoolean(false);
    private static final AtomicLong rateLimitedUntil = new AtomicLong(0L);

    private final TotemGuard plugin;
    private final WebhookConfig alertConfig = new WebhookConfig();
    private final WebhookConfig punishmentConfig = new WebhookConfig();

    public DiscordManager(TotemGuard plugin) {
        this.plugin = plugin;
        reload();
        startQueueProcessor();
    }

    public void reload() {
        REQUEST_QUEUE.clear();

        Webhooks webhooks = plugin.getConfigManager().getWebhooks();
        loadSettings(webhooks.getAlert(), alertConfig);
        loadSettings(webhooks.getPunishment(), punishmentConfig);
    }

    private void loadSettings(Webhooks.WebhookSettings src, WebhookConfig dst) {
        dst.clear();
        if (!src.isEnabled()) return;

        String url = src.getUrl();
        if (!WEBHOOK_REGEX.test(url)) {
            plugin.getLogger().warning("Invalid webhook URL: " + url);
            return;
        }

        try {
            dst.uri = new URI(url);
            dst.username = src.getName();
            dst.avatarUrl = src.getProfileImage();
            dst.title = src.getTitle();
            dst.color = Color.decode(src.getColor()).getRGB();
            dst.timestamp = src.isTimestamp();
            dst.footer = src.isFooter();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse webhook settings: " + e.getMessage());
        }
    }

    public void sendAlert(Check check, Component details) {
        sendWebhook(alertConfig, check, details);
    }

    public void sendPunishment(Check check, Component details) {
        sendWebhook(punishmentConfig, check, details);
    }

    private void sendWebhook(WebhookConfig cfg, Check check, Component details) {
        if (cfg.uri == null) return;

        TotemPlayer p = check.getPlayer();
        Embed embed = new Embed("")
                .title(cfg.title)
                .color(cfg.color)
                .thumbnailURL("http://cravatar.eu/avatar/" + p.getName() + "/64.png")
                .addFields(
                        new EmbedField("Player", "`" + p.getName() + "`", true),
                        new EmbedField("Check", check.getCheckName(), true)
                );

        // only for alerts (not punishments)
        if (cfg == alertConfig) {
            String viol = check.getCheckSettings().isPunishable()
                    ? "[" + check.getViolations() + "/" + check.getMaxViolations() + "]"
                    : String.valueOf(check.getViolations());
            embed.addFields(
                    new EmbedField("Violations", viol, true),
                    new EmbedField("Client Brand", p.getBrand(), true),
                    new EmbedField("Client Version",
                            p.user.getClientVersion().getReleaseName(), true),
                    new EmbedField("Ping", String.valueOf(p.getKeepAlivePing()), true),
                    new EmbedField("TPS", String.format("%.2f", TpsUtil
                            .getInstance().getTps(p.bukkitPlayer.getLocation())), true),
                    new EmbedField("Details", "```" +
                            PlainTextComponentSerializer.plainText().serialize(details) +
                            "```", false)
            );
        }

        if (cfg.timestamp) embed.timestamp(Instant.now());
        if (cfg.footer) embed.footer(new EmbedFooter("Server: " +
                plugin.getConfigManager().getSettings().getServer()));

        WebhookMessage msg = new WebhookMessage()
                .username(cfg.username)
                .avatar(cfg.avatarUrl)
                .addEmbeds(embed);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(cfg.uri)
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(msg.toJson().toString()))
                .build();

        if (!REQUEST_QUEUE.offerLast(req)) {
            REQUEST_QUEUE.pollFirst();
            plugin.getLogger().warning("Discord queue full. Dropped the oldest request");
            REQUEST_QUEUE.offerLast(req);
        }
    }

    private void startQueueProcessor() {
        if (SCHEDULER_STARTED.compareAndSet(false, true)) {
            FoliaScheduler.getAsyncScheduler()
                    .runAtFixedRate(plugin, o -> processQueue(), 0, 1, TimeUnit.SECONDS);
        }
    }

    private void processQueue() {
        long now = System.currentTimeMillis();
        if (now < rateLimitedUntil.get()) return;

        HttpRequest head = REQUEST_QUEUE.peekFirst();
        if (head == null || !SENDING.compareAndSet(false, true)) return;

        HTTP_CLIENT.sendAsync(head, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, err) -> {
                    try {
                        if (err != null) {
                            plugin.getLogger().warning("Webhook send failed: " + err.getMessage());
                        } else if (resp.statusCode() == 429) {
                            resp.headers().firstValue("X-RateLimit-Reset")
                                    .ifPresent(reset -> {
                                        long resetMs = Long.parseLong(reset) * 1000L;
                                        rateLimitedUntil.updateAndGet(r -> Math.max(r, resetMs));
                                    });
                        } else {
                            if (resp.statusCode() >= 400) {
                                plugin.getLogger().warning(
                                        "Discord webhook error " + resp.statusCode() + ": " + resp.body()
                                );
                            }
                        }
                    } finally {
                        if (err != null || (resp != null && resp.statusCode() != 429)) {
                            REQUEST_QUEUE.pollFirst();
                        }
                        SENDING.set(false);
                    }
                });
    }

    private static class WebhookConfig {
        URI uri;
        String username;
        String avatarUrl;
        String title;
        int color;
        boolean timestamp;
        boolean footer;

        void clear() {
            uri = null;
            username = avatarUrl = title = null;
            color = 0;
            timestamp = footer = false;
        }
    }
}
