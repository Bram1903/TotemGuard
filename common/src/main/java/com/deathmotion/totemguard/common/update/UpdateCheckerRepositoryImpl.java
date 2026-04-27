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

package com.deathmotion.totemguard.common.update;

import com.deathmotion.totemguard.api3.config.key.MessagesKeys;
import com.deathmotion.totemguard.api3.reload.Reloadable;
import com.deathmotion.totemguard.api3.update.UpdateCheckerRepository;
import com.deathmotion.totemguard.api3.versioning.TGVersion;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.UpdateCheckerOptions;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Polls GitHub for new TotemGuard releases.
 * <p>
 * The latest release tag is cached in Redis (when configured) so a fleet of
 * servers booting together share a single HTTP fetch. Once a server discovers
 * a new release it also publishes a {@link Packets#SYNC_UPDATE_AVAILABLE}
 * broadcast so siblings flip their local "update available" flag immediately
 * instead of waiting for their own polling cycle.
 */
public final class UpdateCheckerRepositoryImpl implements UpdateCheckerRepository, Reloadable {

    private static final String GITHUB_API_URL =
            "https://api.github.com/repos/Bram1903/TotemGuard/releases/latest";
    private static final String RELEASE_PAGE_URL =
            "https://github.com/Bram1903/TotemGuard/releases/latest";

    private static final String CACHE_KEY = "totemguard:update-checker:latest-version";

    // Reads do NOT refresh the TTL, so a server booting after this many idle
    // hours gets a real fresh fetch (and broadcasts to anyone still running).
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Duration CHECK_INTERVAL = Duration.ofHours(6);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    // Brief grace period after startup so other repositories finish wiring before we run.
    // Staggered boots (rolling restart) still get cache reuse via the Redis lookup; on a
    // truly synchronized fleet boot every server will cache-miss and fetch its own copy.
    private static final long INITIAL_DELAY_SECONDS = 5L;

    private final TGPlatform platform;
    private final HttpClient httpClient;
    private final RedisVersionCache redisCache;
    private final AtomicBoolean checkInFlight = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<TGVersion>> ongoingForcedFetch = new AtomicReference<>();

    private volatile UpdateCheckerOptions options;
    private volatile @Nullable TGVersion latestVersion;

    public UpdateCheckerRepositoryImpl() {
        this.platform = TGPlatform.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
        this.redisCache = new RedisVersionCache(platform.getRedisRepository(), CACHE_KEY, CACHE_TTL);
        this.options = currentOptions();

        if (this.options.enabled()) {
            scheduleCheck(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static @Nullable TGVersion parseVersion(String tag) {
        try {
            // GitHub release tags are typically prefixed with "v" (e.g. v3.0.0).
            return TGVersion.fromString(tag.replaceFirst("^[vV]", ""));
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void reload() {
        UpdateCheckerOptions previous = this.options;
        UpdateCheckerOptions next = currentOptions();
        this.options = next;

        if (!previous.enabled() && next.enabled()) {
            scheduleCheck(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public @Nullable TGVersion latestKnownVersion() {
        return this.latestVersion;
    }

    @Override
    public boolean isUpdateAvailable() {
        TGVersion latest = this.latestVersion;
        return latest != null && TGVersions.CURRENT.isOlderThan(latest);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable TGVersion> checkNow() {
        CompletableFuture<TGVersion> existing = ongoingForcedFetch.get();
        if (existing != null) return existing;

        CompletableFuture<TGVersion> future = new CompletableFuture<>();
        if (!ongoingForcedFetch.compareAndSet(null, future)) {
            // Another caller raced us; return their stage instead.
            return ongoingForcedFetch.get();
        }

        platform.getScheduler().runAsyncTask(() -> {
            try {
                FetchResult result = forceFetch();
                if (result != null) {
                    applyResult(result);
                }
                future.complete(result == null ? null : result.version());
            } catch (Throwable t) {
                future.complete(null);
                platform.getLogger().log(Level.FINE,
                        "Forced update check failed: " + t.getMessage(), t);
            } finally {
                ongoingForcedFetch.compareAndSet(future, null);
            }
        });

        return future;
    }

    /**
     * Send the "update available" message to the given user when an update has
     * been confirmed and the user is allowed to receive these notifications.
     */
    public void notifyIfOutdated(PlatformUser user) {
        UpdateCheckerOptions opts = this.options;
        if (!opts.enabled() || !opts.notifyOnJoin()) return;

        TGVersion latest = this.latestVersion;
        if (latest == null) return;
        if (!TGVersions.CURRENT.isOlderThan(latest)) return;

        if (!user.hasPermission("TotemGuardV3.UpdateNotify")) return;

        user.sendMessage(buildMessage(latest));
    }

    /**
     * Apply a version tag broadcast by a sibling server. New (or different)
     * versions update the local state and announce once; repeats are a no-op.
     */
    public void acceptSyncedVersion(String tag) {
        TGVersion parsed = parseVersion(tag);
        if (parsed == null) return;

        TGVersion previous = this.latestVersion;
        if (previous != null && previous.equals(parsed)) return;

        this.latestVersion = parsed;
        announce(parsed);
    }

    private UpdateCheckerOptions currentOptions() {
        return platform.getConfigRepository().configView().updateChecker();
    }

    private void scheduleCheck(long delay, TimeUnit unit) {
        platform.getScheduler().runAsyncTaskDelayed(this::performCheck, delay, unit);
    }

    private void performCheck() {
        if (!options.enabled()) return;
        if (!checkInFlight.compareAndSet(false, true)) return;

        try {
            FetchResult result = lookupLatest();
            if (result != null) {
                applyResult(result);
            }
        } catch (Exception ex) {
            // Swallow: version checks must never break startup or tick the logger every retry.
            platform.getLogger().log(Level.FINE, "Update check failed: " + ex.getMessage(), ex);
        } finally {
            checkInFlight.set(false);
            if (options.enabled()) {
                scheduleCheck(CHECK_INTERVAL.toSeconds(), TimeUnit.SECONDS);
            }
        }
    }

    private void applyResult(FetchResult result) {
        TGVersion previous = this.latestVersion;
        boolean changed = previous == null || !previous.equals(result.version());
        this.latestVersion = result.version();

        if (changed) {
            announce(result.version());
        }

        // Only the server that actually fetched broadcasts. Cached reads were
        // already populated by whoever fetched first, so re-broadcasting is noise.
        if (result.fromHttpFetch() && changed) {
            publishToFleet(result.tag());
        }
    }

    private @Nullable FetchResult lookupLatest() {
        String cached = redisCache.read();
        if (cached != null) {
            TGVersion parsed = parseVersion(cached);
            if (parsed != null) return new FetchResult(parsed, cached, false);
            // Bad payload sat in cache; drop it and fall through to a fresh fetch.
            redisCache.invalidate();
        }
        return forceFetch();
    }

    private @Nullable FetchResult forceFetch() {
        String fetched = fetchTagFromGitHub();
        if (fetched == null) return null;

        TGVersion parsed = parseVersion(fetched);
        if (parsed == null) return null;

        redisCache.write(fetched);
        return new FetchResult(parsed, fetched, true);
    }

    private @Nullable String fetchTagFromGitHub() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "TotemGuard/" + TGVersions.CURRENT.toStringWithoutSnapshot())
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                platform.getLogger().fine("GitHub returned status " + response.statusCode() + " for update check");
                return null;
            }

            JsonObject body = new JsonParser().parse(response.body()).getAsJsonObject();
            if (!body.has("tag_name")) return null;

            return body.get("tag_name").getAsString();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            platform.getLogger().log(Level.FINE, "Update check HTTP fetch failed: " + ex.getMessage(), ex);
            return null;
        }
    }

    private void publishToFleet(String tag) {
        RedisRepositoryImpl redis = platform.getRedisRepository();
        if (!redis.isConnected()) return;
        redis.publish(Packets.SYNC_UPDATE_AVAILABLE.<String>packet(), tag);
    }

    private void announce(TGVersion latest) {
        TGVersion current = TGVersions.CURRENT;

        if (current.isOlderThan(latest)) {
            platform.getLogger().info(
                    "A new TotemGuard release is available: " + latest
                            + " (running " + current + "). Download at " + RELEASE_PAGE_URL);
        } else if (current.isNewerThan(latest)) {
            platform.getLogger().info(
                    "Running a development build " + current + " ahead of latest release " + latest);
        }
    }

    private Component buildMessage(TGVersion latest) {
        Map<String, Object> extras = Map.of(
                "tg_update_current", TGVersions.CURRENT.toString(),
                "tg_update_latest", latest.toString(),
                "tg_update_url", RELEASE_PAGE_URL
        );
        return platform.getMessageService().getComponent(MessagesKeys.UPDATE_AVAILABLE, null, null, extras);
    }

    private record FetchResult(TGVersion version, String tag, boolean fromHttpFetch) {
    }
}
