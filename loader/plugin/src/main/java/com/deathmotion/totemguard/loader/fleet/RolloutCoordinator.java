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

package com.deathmotion.totemguard.loader.fleet;

import com.deathmotion.totemguard.api.fleet.FleetCache;
import com.deathmotion.totemguard.api.fleet.FleetLock;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RolloutCoordinator {

    public static final int SCHEMA_VERSION = 1;
    public static final String LOCK_KEY = "totemguard:loader:lock:rollout";
    public static final String HASH_PREFIX = "totemguard:loader:rollout:";
    public static final String TOPIC = "totemguard:loader:pub:rollout";
    public static final Duration LOCK_TTL = Duration.ofMinutes(10);
    public static final Duration HASH_TTL = Duration.ofHours(24);

    public static final String PHASE_BEGIN = "BEGIN";
    public static final String PHASE_APPLY = "APPLY";
    public static final String PHASE_CANCEL = "CANCEL";
    public static final String PHASE_COMPLETE = "COMPLETE";

    public static final Duration DEFAULT_APPLY_OFFSET = Duration.ofSeconds(3);

    private final Logger logger;
    private final FleetCacheRef cacheRef;
    private final Consumer<RolloutApply> applyHandler;
    private final BiPredicate<String, String> catalogStager;

    private volatile @Nullable AutoCloseable subscription;
    private volatile @Nullable LeaderState leaderState;

    public RolloutCoordinator(Logger logger, FleetCacheRef cacheRef,
                              Consumer<RolloutApply> applyHandler,
                              BiPredicate<String, String> catalogStager) {
        this.logger = logger;
        this.cacheRef = cacheRef;
        this.applyHandler = applyHandler;
        this.catalogStager = catalogStager;
    }

    public void connect() {
        cacheRef.onAttach(this::onAttach);
        cacheRef.onDetach(this::onDetach);
    }

    private void onAttach(FleetCache cache) {
        try {
            this.subscription = cache.subscribe(TOPIC, payload -> handlePubsub(cache, payload));
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to subscribe to rollout topic", t);
        }
    }

    private void onDetach(FleetCache cache) {
        if (subscription != null) {
            try {
                subscription.close();
            } catch (Throwable ignored) {
            }
            subscription = null;
        }
    }

    public RolloutBegin begin(String version, String sha256, String sourceLabel) throws RolloutBusyException {
        FleetCache cache = cacheRef.available()
                .orElseThrow(() -> new IllegalStateException("Fleet cache not attached. Rollouts require Redis."));

        Optional<FleetLock> lock = cache.tryLock(LOCK_KEY, LOCK_TTL);
        if (lock.isEmpty()) {
            throw new RolloutBusyException("Another rollout is in progress. Use /tgloader status to see who's holding it.");
        }
        FleetLock acquired = lock.get();

        UUID opId = UUID.randomUUID();
        UUID leader = cache.instanceId();
        Instant now = Instant.now();

        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("schemaVersion", String.valueOf(SCHEMA_VERSION));
        hash.put("opId", opId.toString());
        hash.put("leader", leader.toString());
        hash.put("startedAt", now.toString());
        hash.put("phase", PHASE_BEGIN);
        hash.put("targetVersion", version);
        hash.put("targetSha256", sha256);
        hash.put("targetSource", sourceLabel == null ? "" : sourceLabel);

        try {
            cache.putHash(HASH_PREFIX + opId, hash, HASH_TTL);
        } catch (Throwable t) {
            try {
                acquired.close();
            } catch (Throwable ignored) {
            }
            throw new RolloutBusyException("Failed to write rollout state: " + t.getMessage());
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("schemaVersion", SCHEMA_VERSION);
        payload.addProperty("opId", opId.toString());
        payload.addProperty("phase", PHASE_BEGIN);
        payload.addProperty("version", version);
        payload.addProperty("sha256", sha256);
        payload.addProperty("leader", leader.toString());
        try {
            cache.publish(TOPIC, payload.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }

        RolloutBegin begin = new RolloutBegin(opId, acquired, leader);
        this.leaderState = new LeaderState(opId, acquired, version, sha256);
        return begin;
    }

    public Optional<LeaderState> leaderState() {
        LeaderState s = leaderState;
        return Optional.ofNullable(s);
    }

    public void applyActive(Duration applyOffset) {
        LeaderState state = leaderState;
        if (state == null) {
            throw new IllegalStateException("No active rollout on this host. Run /tgloader rollout stage first.");
        }
        Instant applyAt = Instant.now().plus(applyOffset == null ? DEFAULT_APPLY_OFFSET : applyOffset);
        apply(state.opId(), state.lock(), applyAt);
        leaderState = null;
    }

    public void cancelActive() {
        LeaderState state = leaderState;
        if (state == null) {
            throw new IllegalStateException("No active rollout on this host to cancel.");
        }
        cancel(state.opId(), state.lock());
        leaderState = null;
    }

    public void apply(UUID opId, FleetLock lock, Instant applyAt) {
        FleetCache cache = cacheRef.available()
                .orElseThrow(() -> new IllegalStateException("Fleet cache not attached"));

        Map<String, String> existing = cache.getHash(HASH_PREFIX + opId);
        String targetVersion = existing.getOrDefault("targetVersion", "");
        String targetSha256 = existing.getOrDefault("targetSha256", "");

        JsonObject payload = new JsonObject();
        payload.addProperty("schemaVersion", SCHEMA_VERSION);
        payload.addProperty("opId", opId.toString());
        payload.addProperty("phase", PHASE_APPLY);
        payload.addProperty("applyAtMillis", applyAt.toEpochMilli());
        payload.addProperty("targetVersion", targetVersion);
        payload.addProperty("targetSha256", targetSha256);
        cache.publish(TOPIC, payload.toString().getBytes(StandardCharsets.UTF_8));

        Map<String, String> hash = new LinkedHashMap<>(existing);
        hash.put("phase", PHASE_APPLY);
        hash.put("applyAt", applyAt.toString());
        try {
            cache.putHash(HASH_PREFIX + opId, hash, HASH_TTL);
        } catch (Throwable ignored) {
        }
        try {
            lock.close();
        } catch (Throwable ignored) {
        }
    }

    public void cancel(UUID opId, FleetLock lock) {
        cacheRef.available().ifPresent(cache -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("schemaVersion", SCHEMA_VERSION);
            payload.addProperty("opId", opId.toString());
            payload.addProperty("phase", PHASE_CANCEL);
            try {
                cache.publish(TOPIC, payload.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Throwable ignored) {
            }
            Map<String, String> hash = new LinkedHashMap<>(cache.getHash(HASH_PREFIX + opId));
            hash.put("phase", PHASE_CANCEL);
            try {
                cache.putHash(HASH_PREFIX + opId, hash, HASH_TTL);
            } catch (Throwable ignored) {
            }
        });
        try {
            lock.close();
        } catch (Throwable ignored) {
        }
    }

    public Optional<RolloutSnapshot> active() {
        Optional<FleetCache> cacheOpt = cacheRef.available();
        if (cacheOpt.isEmpty()) return Optional.empty();
        FleetCache cache = cacheOpt.get();
        if (!cache.exists(LOCK_KEY)) return Optional.empty();

        // Cheap lookup: scan for one rollout hash key.
        List<String> keys = cache.scanKeys(HASH_PREFIX, 16);
        RolloutSnapshot best = null;
        for (String key : keys) {
            Map<String, String> hash = cache.getHash(key);
            if (hash.isEmpty()) continue;
            try {
                UUID opId = UUID.fromString(hash.get("opId"));
                String phase = hash.getOrDefault("phase", PHASE_BEGIN);
                if (PHASE_COMPLETE.equals(phase) || PHASE_CANCEL.equals(phase)) continue;
                Instant started = Instant.parse(hash.get("startedAt"));
                if (best == null || started.isAfter(best.startedAt())) {
                    best = new RolloutSnapshot(opId,
                            UUID.fromString(hash.get("leader")),
                            hash.getOrDefault("targetVersion", "?"),
                            hash.getOrDefault("targetSha256", ""),
                            phase, started, hash);
                }
            } catch (Throwable ignored) {
            }
        }
        return Optional.ofNullable(best);
    }

    private void handlePubsub(FleetCache cache, byte[] payload) {
        try {
            JsonObject json = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = json.has("schemaVersion") ? json.get("schemaVersion").getAsInt() : 0;
            if (schema > SCHEMA_VERSION) return;
            String phase = json.get("phase").getAsString();
            UUID opId = UUID.fromString(json.get("opId").getAsString());

            switch (phase) {
                case PHASE_APPLY -> {
                    long applyAtMillis = json.has("applyAtMillis")
                            ? json.get("applyAtMillis").getAsLong()
                            : System.currentTimeMillis();
                    String targetVersion = json.has("targetVersion") ? json.get("targetVersion").getAsString() : "";
                    String targetSha256 = json.has("targetSha256") ? json.get("targetSha256").getAsString() : "";
                    applyHandler.accept(new RolloutApply(opId, Instant.ofEpochMilli(applyAtMillis),
                            targetVersion, targetSha256));
                }
                case PHASE_BEGIN -> {
                    String beginVersion = json.has("version") ? json.get("version").getAsString() : "";
                    String beginSha = json.has("sha256") ? json.get("sha256").getAsString() : "";
                    if (!beginSha.isBlank() && catalogStager != null) {
                        catalogStager.test(beginVersion, beginSha);
                    }
                    logger.fine("Rollout " + opId + " BEGIN received.");
                }
                case PHASE_CANCEL -> logger.info("Rollout " + opId + " was cancelled by the leader.");
                case PHASE_COMPLETE -> logger.fine("Rollout " + opId + " marked COMPLETE.");
                default -> logger.fine("Unknown rollout phase: " + phase);
            }
        } catch (Throwable t) {
            logger.log(Level.FINE, "Failed to handle rollout payload", t);
        }
    }

    public record LeaderState(UUID opId, FleetLock lock, String targetVersion, String targetSha256) {
    }

    public record RolloutBegin(UUID opId, FleetLock lock, UUID leader) {
    }

    public record RolloutApply(UUID opId, Instant applyAt, String targetVersion, String targetSha256) {
    }

    public record RolloutSnapshot(UUID opId, UUID leader, String targetVersion, String targetSha256,
                                  String phase, Instant startedAt, Map<String, String> raw) {
    }

    public static final class RolloutBusyException extends Exception {
        public RolloutBusyException(String message) {
            super(message);
        }
    }
}
