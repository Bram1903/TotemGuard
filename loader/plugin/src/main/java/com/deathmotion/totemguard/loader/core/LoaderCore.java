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

package com.deathmotion.totemguard.loader.core;

import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.deathmotion.totemguard.loader.catalog.CatalogIndex;
import com.deathmotion.totemguard.loader.catalog.CatalogRetention;
import com.deathmotion.totemguard.loader.catalog.CatalogSidecar;
import com.deathmotion.totemguard.loader.classloader.ApiClassInjector;
import com.deathmotion.totemguard.loader.config.LoaderConfig;
import com.deathmotion.totemguard.loader.download.CachedJarStore;
import com.deathmotion.totemguard.loader.download.Checksums;
import com.deathmotion.totemguard.loader.download.LocalImporter;
import com.deathmotion.totemguard.loader.fleet.FleetBroker;
import com.deathmotion.totemguard.loader.fleet.FleetCacheRef;
import com.deathmotion.totemguard.loader.fleet.RolloutCoordinator;
import com.deathmotion.totemguard.loader.source.Artifact;
import com.deathmotion.totemguard.loader.source.ResolveCache;
import com.deathmotion.totemguard.loader.source.ResolverContext;
import com.deathmotion.totemguard.loader.source.VersionResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates the api-free part of the loader: read config, resolve the TotemGuard
 * plugin jar from the configured source, download or use the cached copy, then inject
 * the api classes into the host plugin classloader. The reflective api-aware runtime
 * is spun up afterward by the platform entrypoint.
 * <p>
 * Nothing in this class touches {@code com.deathmotion.totemguard.api.*} directly.
 */
public final class LoaderCore {

    private final Logger logger;
    private final LoaderPaths paths;
    private final HostPlatform platform;
    private final FleetCacheRef fleetCacheRef;
    private final FleetBroker fleetBroker;
    private final AtomicBoolean fleetWired = new AtomicBoolean();
    private volatile RolloutCoordinator rolloutCoordinator;
    private volatile LoaderConfig lastConfig;
    private volatile LoaderResult lastResult;

    public LoaderCore(Logger logger, LoaderPaths paths, HostPlatform platform) {
        this.logger = logger;
        this.paths = paths;
        this.platform = platform;
        this.fleetCacheRef = new FleetCacheRef(logger);
        this.fleetBroker = new FleetBroker(logger, paths, platform, fleetCacheRef);
    }

    private static String parseVersionFromCacheName(String fileName) {
        int firstDash = fileName.indexOf('-');
        if (firstDash < 0) return fileName;
        int secondDash = fileName.indexOf('-', firstDash + 1);
        if (secondDash < 0) return fileName;
        int lastDash = fileName.lastIndexOf('-');
        if (lastDash <= secondDash) return fileName;
        return fileName.substring(secondDash + 1, lastDash);
    }

    private static String sourceTagFor(LoaderConfig config) {
        String channel = config.channel();
        return channel == null
                ? CatalogSidecar.SOURCE_PINNED
                : CatalogSidecar.SOURCE_CHANNEL_PREFIX + channel;
    }

    private static String stagedSourceTag(String stagedSource) {
        if (stagedSource == null) return CatalogSidecar.SOURCE_UNKNOWN;
        if ("FLEET".equalsIgnoreCase(stagedSource)) return CatalogSidecar.SOURCE_FLEET;
        return CatalogSidecar.SOURCE_PINNED;
    }

    /**
     * Sorts a resolve failure into a small set of buckets so the fallback log line can say
     * something true. The "unreachable" message used to fire for every failure including
     * "upstream returned 200 but the version it offered is too old" or "the channel was
     * filtered out by the asset suffix", which read like a network problem.
     */
    private static ResolveFailureKind classifyResolveFailure(Throwable t) {
        Throwable root = t;
        for (int hops = 0; hops < 32; hops++) {
            Throwable next = root.getCause();
            if (next == null || next == root) break;
            root = next;
        }
        if (root instanceof java.net.UnknownHostException
                || root instanceof java.net.ConnectException
                || root instanceof java.net.http.HttpTimeoutException
                || root instanceof java.net.SocketTimeoutException
                || root instanceof javax.net.ssl.SSLException) {
            return ResolveFailureKind.UNREACHABLE;
        }
        String msg = root.getMessage();
        if (msg != null) {
            if (msg.startsWith("Refusing to load TotemGuard")) return ResolveFailureKind.GATE_REJECTED;
            if (msg.contains("has no compatible")) return ResolveFailureKind.NO_MATCH;
            if (msg.startsWith("No ") && msg.contains(" matched ")) return ResolveFailureKind.NO_MATCH;
            if (msg.contains(" returned ")) return ResolveFailureKind.UPSTREAM_ERROR;
        }
        return ResolveFailureKind.RESOLVE_FAILED;
    }

    public void initRolloutCoordinator(Consumer<RolloutCoordinator.RolloutApply> applyHandler) {
        this.rolloutCoordinator = new RolloutCoordinator(logger, fleetCacheRef, applyHandler, this::stageFromCatalogBySha);
    }

    public RolloutCoordinator rolloutCoordinator() {
        return rolloutCoordinator;
    }

    public FleetCacheRef fleetCacheRef() {
        return fleetCacheRef;
    }

    private Path pickFallbackJar(CachedJarStore store, LoaderConfig config, HostPlatform platform, Exception originalEx) {
        Optional<Path> targeted = store.locateFallback(config, platform);
        Path cached = targeted.orElseGet(() -> store.findNewestCachedJar(platform).orElse(null));
        if (cached == null) return null;
        logger.log(Level.FINE, "Falling back to cached jar " + cached.getFileName() + " after resolve failure.", originalEx);
        if (!new JarIntegrityChecker(logger, "TotemGuard").verifyJar(cached)) {
            logger.warning("Cached fallback " + cached.getFileName() + " failed integrity verification. Refusing to use it.");
            return null;
        }
        String cachedVersion = parseVersionFromCacheName(cached.getFileName().toString());
        if (!PluginVersionGate.isSupportedConcrete(cachedVersion)) {
            logger.warning("Cached fallback " + cached.getFileName() + " is older than "
                    + PluginVersionGate.MINIMUM + ". Refusing to use it.");
            return null;
        }
        return cached;
    }

    public LoaderResult run(ClassLoader injectTarget) throws Exception {
        return run(injectTarget, null);
    }

    public LoaderResult run(ClassLoader injectTarget, String versionOverride) throws Exception {
        LoaderConfig diskConfig = LoaderConfig.loadOrWriteDefault(paths.loaderDir(), logger);
        this.lastConfig = diskConfig;
        LoaderConfig config = versionOverride == null ? diskConfig : diskConfig.withVersion(versionOverride);

        int imported = new LocalImporter(logger).importAll(paths, platform, record ->
                fleetBroker.announceJar(record.jar(), record.version(), record.sha256(), "local-import"));
        if (imported > 0) {
            logger.info("Imported " + imported + " jar" + (imported == 1 ? "" : "s") + " from local/ into the version catalog.");
        }

        CatalogIndex catalogIndex = new CatalogIndex(paths.versionsDir(), platform, logger);

        StagedJar staged = StagedJar.consumeIfPresent(paths, logger);
        Path jar;
        String resolvedVersion;
        String sourceLabel;
        String catalogSource;
        boolean alreadyVerified = false;
        if (staged != null) {
            PluginVersionGate.require(staged.version(), "staged jar (source " + staged.source() + ")");
            logger.info("Loading staged TotemGuard " + staged.version() + " (source: " + staged.source() + ")");
            jar = staged.jar();
            resolvedVersion = staged.version();
            sourceLabel = staged.source();
            catalogSource = stagedSourceTag(staged.source());
        } else {
            CachedJarStore store = new CachedJarStore(paths.versionsDir(), logger);
            VersionResolver resolver = VersionResolver.forConfig(config);
            catalogSource = sourceTagFor(config);

            Optional<Path> pinnedCache = config.channel() == null
                    ? store.findPinnedJar(config.version(), platform)
                    : Optional.empty();
            if (pinnedCache.isPresent()
                    && PluginVersionGate.isSupportedConcrete(config.version())
                    && new JarIntegrityChecker(logger, "TotemGuard").verifyJar(pinnedCache.get())) {
                Path cached = pinnedCache.get();
                logger.info("Using cached TotemGuard " + config.version() + " (" + cached.getFileName() + ").");
                String sha = Checksums.hashFile(cached, Artifact.HashAlgorithm.SHA_256);
                catalogIndex.upsertSource(cached, config.version(), sha, catalogSource, null);
                new ApiClassInjector(logger).inject(cached, injectTarget);
                wireFleetHooks();
                LoaderResult result = new LoaderResult(cached, config.version(), "Local cache", sha);
                this.lastResult = result;
                runRetention(catalogIndex, sha);
                return result;
            }

            logger.info("Resolving TotemGuard (version: " + config.version() + ", source: " + resolver.sourceName() + ")");

            try {
                ResolveCache resolveCache = new ResolveCache(logger, fleetCacheRef);
                Optional<String> negative = resolveCache.getNegative(resolver.sourceName(), config.version());
                if (negative.isPresent()) {
                    throw new IOException(negative.get());
                }
                Artifact artifact = resolveCache.get(resolver.sourceName(), config.version());
                if (artifact == null) {
                    try {
                        artifact = resolver.resolve(ResolverContext.of(config, platform, paths, fleetCacheRef));
                    } catch (Exception resolverEx) {
                        resolveCache.putNegative(resolver.sourceName(), config.version(),
                                resolverEx.getMessage() == null ? "resolve failed" : resolverEx.getMessage());
                        throw resolverEx;
                    }
                    resolveCache.put(resolver.sourceName(), config.version(), artifact);
                } else {
                    logger.fine("Resolve cache hit for " + resolver.sourceName() + ":" + config.version()
                            + " (" + artifact.version() + ")");
                }
                PluginVersionGate.require(artifact.version(), resolver.sourceName());
                jar = store.getOrFetch(artifact, platform);
                resolvedVersion = artifact.version();
                sourceLabel = artifact.sourceLabel();
                if (config.channel() != null) {
                    try {
                        store.recordChannel(config.channel(), jar);
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "Failed to record channel pointer for " + config.channel(), ex);
                    }
                }
            } catch (Exception ex) {
                Path cached = pickFallbackJar(store, config, platform, ex);
                if (cached == null) throw ex;
                ResolveFailureKind kind = classifyResolveFailure(ex);
                String rootMsg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                String summary = switch (kind) {
                    case UNREACHABLE -> resolver.sourceName() + " unreachable: " + rootMsg;
                    case RESOLVE_FAILED -> resolver.sourceName() + " resolve failed: " + rootMsg;
                    // GATE_REJECTED, NO_MATCH, UPSTREAM_ERROR are already self-describing.
                    default -> rootMsg;
                };
                logger.warning(summary + " Falling back to cached " + cached.getFileName() + ".");
                jar = cached;
                resolvedVersion = parseVersionFromCacheName(cached.getFileName().toString());
                sourceLabel = "Cached fallback (" + kind.headline + ")";
                alreadyVerified = true;
            }
        }

        if (!alreadyVerified && !new JarIntegrityChecker(logger, "TotemGuard").verifyJar(jar)) {
            throw new IOException("TotemGuard jar at " + jar + " failed integrity verification.");
        }

        new ApiClassInjector(logger).inject(jar, injectTarget);
        wireFleetHooks();

        String sha = Checksums.hashFile(jar, Artifact.HashAlgorithm.SHA_256);
        boolean isNewToCatalog = !alreadyVerified && catalogIndex.findBySha(sha).isEmpty();
        catalogIndex.upsertSource(jar, resolvedVersion, sha, catalogSource, null);
        if (isNewToCatalog) {
            fleetBroker.announceJar(jar, resolvedVersion, sha, sourceLabel);
        }
        LoaderResult result = new LoaderResult(jar, resolvedVersion, sourceLabel, sha);
        this.lastResult = result;
        runRetention(catalogIndex, sha);
        return result;
    }

    private void wireFleetHooks() {
        if (!fleetWired.compareAndSet(false, true)) return;
        fleetCacheRef.markApiReady();
        try {
            fleetBroker.connect();
            RolloutCoordinator coord = this.rolloutCoordinator;
            if (coord != null) coord.connect();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to wire fleet hooks. Falling back to local-only mode.", t);
        }
    }

    private void runRetention(CatalogIndex catalogIndex, String runningSha) {
        Set<String> protectedShas = new HashSet<>();
        if (runningSha != null) protectedShas.add(runningSha);
        readStagedShaSafely().ifPresent(protectedShas::add);
        try {
            int deleted = new CatalogRetention(catalogIndex, logger).sweep(protectedShas);
            if (deleted > 0) {
                logger.info("Retention removed " + deleted + " stale catalog entr"
                        + (deleted == 1 ? "y" : "ies") + ".");
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Catalog retention sweep failed", t);
        }
    }

    private Optional<String> readStagedShaSafely() {
        try {
            if (!Files.isRegularFile(paths.stagedMeta())) return Optional.empty();
            for (String line : Files.readAllLines(paths.stagedMeta())) {
                int eq = line.indexOf('=');
                if (eq > 0 && line.substring(0, eq).trim().equals("sha256")) {
                    return Optional.of(line.substring(eq + 1).trim());
                }
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    public LoaderConfig lastConfig() {
        return lastConfig;
    }

    public LoaderResult lastResult() {
        return lastResult;
    }

    public Artifact resolveCurrentTarget() throws Exception {
        LoaderConfig config = LoaderConfig.loadOrWriteDefault(paths.loaderDir(), logger);
        this.lastConfig = config;
        VersionResolver resolver = VersionResolver.forConfig(config);
        ResolveCache cache = new ResolveCache(logger, fleetCacheRef);
        Artifact cached = cache.get(resolver.sourceName(), config.version());
        if (cached != null) return cached;
        Artifact resolved = resolver.resolve(ResolverContext.of(config, platform, paths, fleetCacheRef));
        cache.put(resolver.sourceName(), config.version(), resolved);
        return resolved;
    }

    public byte[] downloadArtifact(Artifact artifact) throws IOException {
        return new ArtifactDownloader(logger).download(artifact);
    }

    public List<String> listCachedVersions() {
        try {
            return new CachedJarStore(paths.versionsDir(), logger).listCachedVersions(platform);
        } catch (IOException ex) {
            logger.warning("Failed to list cached versions: " + ex.getMessage());
            return List.of();
        }
    }

    public int importLocal() {
        return new LocalImporter(logger).importAll(paths, platform);
    }

    public StageResult stageVersion(String versionOverride) throws Exception {
        LoaderConfig diskConfig = LoaderConfig.loadOrWriteDefault(paths.loaderDir(), logger);
        LoaderConfig config = versionOverride == null ? diskConfig : diskConfig.withVersion(versionOverride);
        PluginVersionGate.rejectIfPinnedTooOld(config.version(), "/tgloader stage");

        if (config.channel() == null) {
            StageResult cached = stageFromLocalCatalog(config.version());
            if (cached != null) return cached;
        }

        VersionResolver resolver = VersionResolver.forConfig(config);
        ResolveCache resolveCache = new ResolveCache(logger, fleetCacheRef);
        Artifact artifact = resolveCache.get(resolver.sourceName(), config.version());
        if (artifact == null) {
            try {
                artifact = resolver.resolve(ResolverContext.of(config, platform, paths, fleetCacheRef));
            } catch (Exception resolverEx) {
                resolveCache.putNegative(resolver.sourceName(), config.version(),
                        resolverEx.getMessage() == null ? "resolve failed" : resolverEx.getMessage());
                throw resolverEx;
            }
            resolveCache.put(resolver.sourceName(), config.version(), artifact);
        }
        PluginVersionGate.require(artifact.version(), resolver.sourceName());

        byte[] bytes = new ArtifactDownloader(logger).download(artifact);
        String sha = Checksums.hashBytes(bytes, Artifact.HashAlgorithm.SHA_256);

        StagedJar.write(paths, bytes, artifact.version(), resolver.sourceName().toUpperCase(Locale.ROOT), sha);
        logger.info("Staged TotemGuard " + artifact.version() + " (sha " + sha.substring(0, 10)
                + "…). Run /tgloader apply to restart.");

        try {
            String hashPrefix = sha.substring(0, Math.min(10, sha.length()));
            Path destination = paths.versionsDir().resolve("TotemGuard-"
                    + platform.name().toLowerCase(Locale.ROOT)
                    + "-" + artifact.version().replaceAll("[^A-Za-z0-9._+-]", "_")
                    + "-" + hashPrefix + ".jar");
            Files.createDirectories(destination.getParent());
            Path partial = destination.resolveSibling(destination.getFileName() + ".partial");
            Files.write(partial, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(partial, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            new CatalogIndex(paths.versionsDir(), platform, logger)
                    .upsertSource(destination, artifact.version(), sha, CatalogSidecar.SOURCE_PINNED, null);
            fleetBroker.announceJar(destination, artifact.version(), sha, resolver.sourceName());
        } catch (Throwable t) {
            logger.log(Level.FINE, "Stage post-write to catalog failed (staged.jar still valid)", t);
        }

        return new StageResult(artifact.version(), sha, resolver.sourceName());
    }

    public Optional<StagedSnapshot> readStaged() {
        try {
            if (!Files.isRegularFile(paths.stagedMeta()) || !Files.isRegularFile(paths.stagedJar())) {
                return Optional.empty();
            }
            String version = null, source = null, sha = null;
            for (String line : Files.readAllLines(paths.stagedMeta())) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                if ("version".equals(k)) version = v;
                else if ("source".equals(k)) source = v;
                else if ("sha256".equals(k)) sha = v;
            }
            if (version == null || sha == null) return Optional.empty();
            return Optional.of(new StagedSnapshot(version, sha, source));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private StageResult stageFromLocalCatalog(String version) throws IOException {
        CachedJarStore store = new CachedJarStore(paths.versionsDir(), logger);
        Optional<Path> located = store.findPinnedJar(version, platform);
        if (located.isEmpty()) return null;
        Path jar = located.get();
        if (!new JarIntegrityChecker(logger, "TotemGuard").verifyJar(jar)) {
            logger.warning("Local catalog jar " + jar.getFileName() + " failed integrity. Skipping stage.");
            return null;
        }
        byte[] bytes = Files.readAllBytes(jar);
        String sha = Checksums.hashBytes(bytes, Artifact.HashAlgorithm.SHA_256);
        StagedJar.write(paths, bytes, version, "LOCAL-CACHE", sha);
        logger.info("Staged TotemGuard " + version + " from local catalog (" + jar.getFileName() + ").");
        new CatalogIndex(paths.versionsDir(), platform, logger)
                .upsertSource(jar, version, sha, CatalogSidecar.SOURCE_PINNED, null);
        fleetBroker.announceJar(jar, version, sha, "local-cache");
        return new StageResult(version, sha, "LOCAL-CACHE");
    }

    public boolean stageFromCatalogBySha(String version, String sha256) {
        if (sha256 == null || sha256.isBlank()) return false;
        try {
            CatalogIndex index = new CatalogIndex(paths.versionsDir(), platform, logger);
            Optional<CatalogIndex.Entry> match = index.findBySha(sha256);
            if (match.isEmpty()) {
                logger.fine("Rollout target " + sha256 + " is not in the local catalog yet. "
                        + "Skipping catalog-stage. The leader's jar-available broadcast may not have arrived.");
                return false;
            }
            Path jar = match.get().jar();
            if (!new JarIntegrityChecker(logger, "TotemGuard").verifyJar(jar)) {
                logger.warning("Catalog jar " + jar.getFileName()
                        + " failed integrity verification. Refusing to stage rollout target.");
                return false;
            }
            byte[] bytes = Files.readAllBytes(jar);
            String resolvedVersion = (version == null || version.isBlank())
                    ? match.get().sidecar().version() : version;
            StagedJar.write(paths, bytes, resolvedVersion, "FLEET", sha256);
            logger.info("Staged rollout target TotemGuard " + resolvedVersion + " ("
                    + sha256.substring(0, Math.min(10, sha256.length())) + ") from local catalog.");
            return true;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to stage rollout target " + sha256 + " from local catalog", t);
            return false;
        }
    }

    public HostPlatform platform() {
        return platform;
    }

    public Logger logger() {
        return logger;
    }

    public void shutdown() {
        fleetBroker.shutdown();
    }

    private enum ResolveFailureKind {
        UNREACHABLE("unreachable"),
        UPSTREAM_ERROR("returned an error"),
        NO_MATCH("had no matching build"),
        GATE_REJECTED("rejected by version gate"),
        RESOLVE_FAILED("resolve failed");

        private final String headline;

        ResolveFailureKind(String headline) {
            this.headline = headline;
        }
    }

    public record StageResult(String version, String sha256, String source) {
    }

    public record StagedSnapshot(String version, String sha256, String source) {
    }
}
