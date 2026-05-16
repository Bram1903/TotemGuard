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
import com.deathmotion.totemguard.loader.catalog.CatalogIndex;
import com.deathmotion.totemguard.loader.catalog.CatalogSidecar;
import com.deathmotion.totemguard.loader.core.HostPlatform;
import com.deathmotion.totemguard.loader.core.LoaderPaths;
import com.deathmotion.totemguard.loader.core.PluginVersionGate;
import com.deathmotion.totemguard.loader.download.Checksums;
import com.deathmotion.totemguard.loader.source.Artifact;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wires the loader into the fleet pub/sub. On attach: subscribes to jar-available and
 * catalog-changed topics, schedules a 5-minute heartbeat that refreshes catalog hashes.
 * On detach: tears everything down so the loader transparently goes back to file-only
 * behavior. Used as a fire-and-forget singleton inside {@link com.deathmotion.totemguard.loader.core.LoaderCore}.
 */
public final class FleetBroker {

    public static final String TOPIC_JAR_AVAILABLE = "totemguard:loader:pub:jar-available";
    public static final String TOPIC_CATALOG_CHANGED = "totemguard:loader:pub:catalog-changed";
    public static final String JAR_KEY_PREFIX = "totemguard:loader:jar:";
    public static final String CATALOG_KEY_PREFIX = "totemguard:loader:catalog:";

    public static final int SCHEMA_VERSION = 1;
    public static final Duration JAR_BROADCAST_TTL = Duration.ofMinutes(10);
    public static final Duration CATALOG_HEARTBEAT_TTL = Duration.ofMinutes(15);
    public static final Duration CATALOG_HEARTBEAT_PERIOD = Duration.ofMinutes(5);

    private static final Gson GSON = new Gson();

    private final Logger logger;
    private final LoaderPaths paths;
    private final HostPlatform platform;
    private final FleetCacheRef cacheRef;
    private final ScheduledExecutorService scheduler;

    private final Object subscriptionLock = new Object();
    private volatile @Nullable AutoCloseable jarSubscription;
    private volatile @Nullable AutoCloseable catalogSubscription;
    private volatile @Nullable ScheduledFuture<?> heartbeatFuture;

    public FleetBroker(Logger logger, LoaderPaths paths, HostPlatform platform, FleetCacheRef cacheRef) {
        this.logger = logger;
        this.paths = paths;
        this.platform = platform;
        this.cacheRef = cacheRef;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TotemGuard-Loader-Fleet");
            t.setDaemon(true);
            return t;
        });
        // No api references here. Method-reference creation for onAttach/onDetach is
        // deferred to connect() so the JVM does not try to resolve FleetCache before
        // the inner plugin jar has injected the api classes.
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._+-]", "_");
    }

    /**
     * Wire up subscriptions on the fleet-cache reference. Must be called after the api
     * classes are injected onto the parent classloader, otherwise the method references
     * below will fail to resolve.
     */
    public void connect() {
        cacheRef.onAttach(this::onAttach);
        cacheRef.onDetach(this::onDetach);
    }

    private void onAttach(FleetCache cache) {
        synchronized (subscriptionLock) {
            tearDown();
            try {
                jarSubscription = cache.subscribe(TOPIC_JAR_AVAILABLE, payload -> handleJarAvailable(cache, payload));
                catalogSubscription = cache.subscribe(TOPIC_CATALOG_CHANGED, this::handleCatalogChanged);
                heartbeatFuture = scheduler.scheduleAtFixedRate(this::pushHeartbeat,
                        0L, CATALOG_HEARTBEAT_PERIOD.toMillis(), TimeUnit.MILLISECONDS);
                logger.fine("Fleet broker connected (instance " + cache.instanceId() + ").");
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to attach fleet subscriptions. Continuing in file-only mode.", t);
                tearDown();
            }
        }
    }

    private void onDetach(FleetCache cache) {
        synchronized (subscriptionLock) {
            tearDown();
            logger.fine("Fleet broker disconnected. Falling back to local-only mode.");
        }
    }

    private void tearDown() {
        if (jarSubscription != null) {
            try {
                jarSubscription.close();
            } catch (Throwable ignored) {
            }
            jarSubscription = null;
        }
        if (catalogSubscription != null) {
            try {
                catalogSubscription.close();
            } catch (Throwable ignored) {
            }
            catalogSubscription = null;
        }
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
    }

    /**
     * Producer side: publish a freshly-acquired jar to the fleet. Stores the bytes in
     * Redis with a short TTL (peers pull within ~10m), then announces on the topic.
     * Safe to call when no fleet cache is attached (no-op).
     */
    public void announceJar(Path jar, String version, String sha256, String sourceLabel) {
        Optional<FleetCache> cacheOpt = cacheRef.available();
        if (cacheOpt.isEmpty()) return;
        FleetCache cache = cacheOpt.get();
        try {
            byte[] bytes = Files.readAllBytes(jar);
            String key = JAR_KEY_PREFIX + sha256.toLowerCase(Locale.ROOT);
            cache.put(key, bytes, JAR_BROADCAST_TTL);

            JsonObject payload = new JsonObject();
            payload.addProperty("schemaVersion", SCHEMA_VERSION);
            payload.addProperty("sha256", sha256.toLowerCase(Locale.ROOT));
            payload.addProperty("version", version);
            payload.addProperty("sourceLabel", sourceLabel == null ? "" : sourceLabel);
            payload.addProperty("senderInstance", cache.instanceId().toString());
            cache.publish(TOPIC_JAR_AVAILABLE, GSON.toJson(payload).getBytes(StandardCharsets.UTF_8));

            announceCatalogEntry(cache, version, sha256, sourceLabel);
            logger.fine("Announced TotemGuard " + version + " (" + sha256.substring(0, 10) + "…) to fleet.");
        } catch (Throwable t) {
            logger.log(Level.FINE, "Failed to announce jar to fleet", t);
        }
    }

    private void announceCatalogEntry(FleetCache cache, String version, String sha256, String sourceLabel) {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("sha256", sha256);
        hash.put("version", version);
        hash.put("source", sourceLabel == null ? "" : sourceLabel);
        hash.put("addedBy", cache.instanceId().toString());
        hash.put("addedAt", Instant.now().toString());

        String key = catalogKey(version, sha256);
        try {
            cache.putHash(key, hash, CATALOG_HEARTBEAT_TTL);
        } catch (Throwable ignored) {
        }

        JsonObject pub = new JsonObject();
        pub.addProperty("schemaVersion", SCHEMA_VERSION);
        pub.addProperty("sha256", sha256);
        pub.addProperty("version", version);
        pub.addProperty("action", "added");
        try {
            cache.publish(TOPIC_CATALOG_CHANGED, GSON.toJson(pub).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    /**
     * Refresh the catalog heartbeat for every locally-cached jar. Called on attach and
     * every {@link #CATALOG_HEARTBEAT_PERIOD}. Best-effort, swallows individual failures.
     */
    private void pushHeartbeat() {
        Optional<FleetCache> cacheOpt = cacheRef.available();
        if (cacheOpt.isEmpty()) return;
        FleetCache cache = cacheOpt.get();
        CatalogIndex index = new CatalogIndex(paths.versionsDir(), platform, logger);
        for (CatalogIndex.Entry entry : index.readAll()) {
            CatalogSidecar sidecar = entry.sidecar();
            if (sidecar.sha256() == null || sidecar.sha256().isBlank()) continue;
            try {
                announceCatalogEntry(cache, sidecar.version(), sidecar.sha256(),
                        sidecar.sources().isEmpty() ? "" : sidecar.sources().get(0));
            } catch (Throwable ignored) {
            }
        }
    }

    private void handleJarAvailable(FleetCache cache, byte[] payload) {
        try {
            JsonObject json = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = json.has("schemaVersion") ? json.get("schemaVersion").getAsInt() : 0;
            if (schema > SCHEMA_VERSION) {
                logger.fine("Ignoring jar-available from newer schema " + schema);
                return;
            }
            String sender = json.has("senderInstance") ? json.get("senderInstance").getAsString() : "";
            if (sender.equals(cache.instanceId().toString())) return; // our own broadcast

            String sha256 = json.get("sha256").getAsString();
            String version = json.get("version").getAsString();
            String sourceLabel = json.has("sourceLabel") ? json.get("sourceLabel").getAsString() : "";

            if (!PluginVersionGate.isSupportedConcrete(version)) {
                logger.fine("Ignoring fleet jar " + version + " (below " + PluginVersionGate.MINIMUM + ")");
                return;
            }

            // Already on disk?
            CatalogIndex index = new CatalogIndex(paths.versionsDir(), platform, logger);
            if (index.findBySha(sha256).isPresent()) return;

            // Pull bytes and import.
            scheduler.execute(() -> downloadAndImport(cache, sha256, version, sourceLabel));
        } catch (Throwable t) {
            logger.log(Level.FINE, "Failed to handle jar-available payload", t);
        }
    }

    private void handleCatalogChanged(byte[] payload) {
        // Informational: log at fine level so /tgloader peers diagnostics have something to grep.
        try {
            JsonObject json = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
            String version = json.has("version") ? json.get("version").getAsString() : "?";
            String sha = json.has("sha256") ? json.get("sha256").getAsString() : "";
            String action = json.has("action") ? json.get("action").getAsString() : "?";
            logger.fine("Fleet catalog " + action + ": " + version
                    + (sha.length() >= 10 ? " (" + sha.substring(0, 10) + "…)" : ""));
        } catch (Throwable ignored) {
        }
    }

    private void downloadAndImport(FleetCache cache, String sha256, String version, String sourceLabel) {
        String key = JAR_KEY_PREFIX + sha256.toLowerCase(Locale.ROOT);
        Optional<byte[]> bytes;
        try {
            bytes = cache.get(key);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Failed to pull fleet jar " + sha256, t);
            return;
        }
        if (bytes.isEmpty()) {
            logger.fine("Fleet jar " + sha256 + " blob already expired. Skipping pull.");
            return;
        }

        try {
            String actual = Checksums.hashBytes(bytes.get(), Artifact.HashAlgorithm.SHA_256);
            if (!actual.equalsIgnoreCase(sha256)) {
                logger.warning("Fleet jar payload hash " + actual + " did not match announcement " + sha256
                        + ". Refusing to import.");
                return;
            }

            String hashPrefix = sha256.substring(0, Math.min(10, sha256.length()));
            Path destination = paths.versionsDir().resolve("TotemGuard-"
                    + platform.name().toLowerCase(Locale.ROOT)
                    + "-" + sanitize(version)
                    + "-" + hashPrefix + ".jar");
            Files.createDirectories(destination.getParent());

            Path partial = destination.resolveSibling(destination.getFileName() + ".partial");
            Files.write(partial, bytes.get(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(partial, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            CatalogIndex index = new CatalogIndex(paths.versionsDir(), platform, logger);
            index.upsertSource(destination, version, sha256, CatalogSidecar.SOURCE_FLEET, null);

            logger.info("Imported fleet TotemGuard " + version + " (" + hashPrefix + "…)"
                    + (sourceLabel == null || sourceLabel.isBlank() ? "" : " from " + sourceLabel) + ".");
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to import fleet jar " + sha256, t);
        }
    }

    private String catalogKey(String version, String sha256) {
        String hashPrefix = sha256.substring(0, Math.min(10, sha256.length()));
        return CATALOG_KEY_PREFIX + platform.name().toLowerCase(Locale.ROOT)
                + ":" + sanitize(version)
                + ":" + hashPrefix;
    }

    public void shutdown() {
        synchronized (subscriptionLock) {
            tearDown();
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
