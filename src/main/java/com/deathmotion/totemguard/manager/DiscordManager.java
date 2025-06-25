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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Pattern;

// Big thanks to GrimAnticheat for the original DiscordManager implementation
public class DiscordManager {
    public static final Predicate<String> WEBHOOK_REGEX = Pattern.compile("https://discord\\.com/api/webhooks/\\d+/[\\w-]+").asMatchPredicate();

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP_CLIENT = HttpClient
            .newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private static final ConcurrentLinkedDeque<HttpRequest> REQUEST_QUEUE = new ConcurrentLinkedDeque<>();
    private static final AtomicBoolean SCHEDULER_STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean SENDING = new AtomicBoolean(false);
    private static final AtomicLong rateLimitedUntil = new AtomicLong(0L);

    private final TotemGuard plugin;

    private URI alertUri, punishmentUri;
    private String alertUsername, alertAvatar;
    private String punishmentUsername, punishmentAvatar;
    private String alertTitle, punishmentTitle;
    private int alertColor, punishmentColor;
    private boolean alertTimestamp, alertFooter;
    private boolean punishmentTimestamp, punishmentFooter;

    public DiscordManager(TotemGuard plugin) {
        this.plugin = plugin;
        reload();
        startQueueProcessor();
    }

    public void reload() {
        REQUEST_QUEUE.clear();

        Webhooks webhooks = plugin.getConfigManager().getWebhooks();

        loadSettings(webhooks.getAlert(),
                (uri, name, avatar, title, color, ts, ft) -> {
                    this.alertUri = uri;
                    this.alertUsername = name;
                    this.alertAvatar = avatar;
                    this.alertTitle = title;
                    this.alertColor = color;
                    this.alertTimestamp = ts;
                    this.alertFooter = ft;
                }
        );

        loadSettings(webhooks.getPunishment(),
                (uri, name, avatar, title, color, ts, ft) -> {
                    this.punishmentUri = uri;
                    this.punishmentUsername = name;
                    this.punishmentAvatar = avatar;
                    this.punishmentTitle = title;
                    this.punishmentColor = color;
                    this.punishmentTimestamp = ts;
                    this.punishmentFooter = ft;
                }
        );
    }

    private void loadSettings(Webhooks.WebhookSettings settings, SettingConsumer consumer) {
        if (!settings.isEnabled() || !WEBHOOK_REGEX.test(settings.getUrl())) {
            if (settings.isEnabled()) {
                plugin.getLogger().warning("Invalid webhook URL: " + settings.getUrl());
            }
            consumer.accept(null, null, null, null, 0, false, false);
            return;
        }

        try {
            URI uri = new URI(settings.getUrl());
            int color = Color.decode(settings.getColor()).getRGB();
            consumer.accept(
                    uri,
                    settings.getName(),
                    settings.getProfileImage(),
                    settings.getTitle(),
                    color,
                    settings.isTimestamp(),
                    settings.isFooter()
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse webhook settings: " + e.getMessage());
            consumer.accept(null, null, null, null, 0, false, false);
        }
    }

    public void sendAlert(Check check, Component details) {
        sendWebhook(false, check, details);
    }

    public void sendPunishment(Check check, Component details) {
        sendWebhook(true, check, details);
    }

    private void sendWebhook(boolean isPunishment, Check check, Component details) {
        URI uri = isPunishment ? punishmentUri : alertUri;
        if (uri == null) return;

        String username = isPunishment ? punishmentUsername : alertUsername;
        String avatar = isPunishment ? punishmentAvatar : alertAvatar;
        String title = isPunishment ? punishmentTitle : alertTitle;
        int color = isPunishment ? punishmentColor : alertColor;
        boolean ts = isPunishment ? punishmentTimestamp : alertTimestamp;
        boolean ft = isPunishment ? punishmentFooter : alertFooter;

        TotemPlayer p = check.getPlayer();

        Embed embed = new Embed("")
                .title(title)
                .color(color)
                .thumbnailURL("http://cravatar.eu/avatar/" + p.getName() + "/64.png");

        embed.addFields(
                new EmbedField("Player", "`" + p.getName() + "`", true),
                new EmbedField("Check", check.getCheckName(), true)
        );

        if (!isPunishment) {
            String viol = check.getCheckSettings().isPunishable()
                    ? "[" + check.getViolations() + "/" + check.getMaxViolations() + "]"
                    : String.valueOf(check.getViolations());

            embed.addFields(
                    new EmbedField("Violations", viol, true),
                    new EmbedField("Client Brand", p.getBrand(), true),
                    new EmbedField("Client Version", p.user.getClientVersion().getReleaseName(), true),
                    new EmbedField("Ping", String.valueOf(p.getKeepAlivePing()), true),
                    new EmbedField("TPS", String.format("%.2f", TpsUtil.getInstance().getTps(p.bukkitPlayer.getLocation())), true),
                    new EmbedField("Details", "```" + PlainTextComponentSerializer.plainText().serialize(details) + "```", false)
            );
        }

        if (ts) {
            embed.timestamp(Instant.now());
        }
        if (ft) {
            embed.footer(new EmbedFooter("Server: " + plugin.getConfigManager().getSettings().getServer()));
        }

        WebhookMessage msg = new WebhookMessage()
                .username(username)
                .avatar(avatar)
                .addEmbeds(embed);

        String body = msg.toJson().toString();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        REQUEST_QUEUE.add(req);
    }

    private void startQueueProcessor() {
        if (!SCHEDULER_STARTED.getAndSet(true)) {
            FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> processQueue(), 0, 1, TimeUnit.SECONDS);
        }
    }

    private void processQueue() {
        if (System.currentTimeMillis() < rateLimitedUntil.get()) return;

        HttpRequest head = REQUEST_QUEUE.peek();
        if (head == null || !SENDING.compareAndSet(false, true)) return;

        HTTP_CLIENT.sendAsync(head, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        plugin.getLogger().warning("Webhook send failed: " + err.getMessage());
                        SENDING.set(false);
                        return;
                    }

                    if (resp.statusCode() == 429) {
                        resp.headers().firstValue("X-RateLimit-Reset").ifPresent(reset -> {
                            long resetMs = Long.parseLong(reset) * 1000L;
                            rateLimitedUntil.updateAndGet(prev -> Math.max(prev, resetMs));
                        });
                        SENDING.set(false);
                        return;
                    }

                    if (resp.statusCode() >= 400) {
                        plugin.getLogger().warning(
                                "Discord webhook error " + resp.statusCode() + ": " + resp.body()
                        );
                    }

                    REQUEST_QUEUE.poll();
                    SENDING.set(false);
                });
    }

    @FunctionalInterface
    private interface SettingConsumer {
        void accept(URI uri,
                    String name,
                    String avatar,
                    String title,
                    int color,
                    boolean timestamp,
                    boolean footer);
    }
}
