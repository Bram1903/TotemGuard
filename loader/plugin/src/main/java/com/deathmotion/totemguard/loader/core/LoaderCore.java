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
import com.deathmotion.totemguard.loader.classloader.ApiClassInjector;
import com.deathmotion.totemguard.loader.config.LoaderConfig;
import com.deathmotion.totemguard.loader.download.CachedJarStore;
import com.deathmotion.totemguard.loader.source.Artifact;
import com.deathmotion.totemguard.loader.source.VersionResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Orchestrates the api-free part of the loader: read config, resolve the inner jar
 * from the configured source, download or use the cached copy, then inject the api
 * classes into the host plugin classloader. The reflective api-aware runtime is
 * spun up afterward by the platform entrypoint.
 * <p>
 * Nothing in this class touches {@code com.deathmotion.totemguard.api.*} directly.
 */
public final class LoaderCore {

    private final Logger logger;
    private final LoaderPaths paths;
    private final HostPlatform platform;

    private volatile LoaderConfig lastConfig;
    private volatile LoaderResult lastResult;

    public LoaderCore(Logger logger, LoaderPaths paths, HostPlatform platform) {
        this.logger = logger;
        this.paths = paths;
        this.platform = platform;
    }

    public LoaderResult run(ClassLoader injectTarget) throws Exception {
        LoaderConfig config = LoaderConfig.loadOrWriteDefault(paths.loaderDir(), logger);
        this.lastConfig = config;

        StagedJar staged = StagedJar.consumeIfPresent(paths, logger);
        Path jar;
        String resolvedVersion;
        String sourceLabel;
        if (staged != null) {
            logger.info("Loading staged TotemGuard " + staged.version() + " (source: " + staged.source() + ")");
            jar = staged.jar();
            resolvedVersion = staged.version();
            sourceLabel = staged.source();
        } else {
            VersionResolver resolver = VersionResolver.forConfig(config);
            logger.info("Resolving TotemGuard (version: " + config.version() + ", source: " + resolver.sourceName() + ")");

            Artifact artifact = resolver.resolve(config, platform, paths);
            CachedJarStore store = new CachedJarStore(paths.versionsDir(), logger);
            jar = store.getOrFetch(artifact, platform);
            resolvedVersion = artifact.version();
            sourceLabel = artifact.sourceLabel();
        }

        if (!new JarIntegrityChecker(logger, "TotemGuard").verifyJar(jar)) {
            throw new IOException("TotemGuard jar at " + jar + " failed integrity verification.");
        }

        new ApiClassInjector(logger).inject(jar, injectTarget);

        LoaderResult result = new LoaderResult(jar, resolvedVersion, sourceLabel);
        this.lastResult = result;
        return result;
    }

    public LoaderConfig lastConfig() {
        return lastConfig;
    }

    public LoaderResult lastResult() {
        return lastResult;
    }

    /**
     * Re-reads the loader config from disk, resolves the configured target via the
     * appropriate source, and returns the resulting {@link Artifact}. Performs the
     * network calls each source requires (GitHub releases listing, Modrinth versions
     * endpoint, etc.) but does not download the jar bytes.
     */
    public Artifact resolveCurrentTarget() throws Exception {
        LoaderConfig config = LoaderConfig.loadOrWriteDefault(paths.loaderDir(), logger);
        this.lastConfig = config;
        return VersionResolver.forConfig(config).resolve(config, platform, paths);
    }

    /**
     * Downloads the bytes for an {@link Artifact}, verifying the source-native checksum.
     */
    public byte[] downloadArtifact(Artifact artifact) throws IOException {
        return new ArtifactDownloader(logger).download(artifact);
    }

    public HostPlatform platform() {
        return platform;
    }

    public LoaderPaths paths() {
        return paths;
    }

    public Logger logger() {
        return logger;
    }
}
