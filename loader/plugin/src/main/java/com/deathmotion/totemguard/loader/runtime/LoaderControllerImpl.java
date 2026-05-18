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

package com.deathmotion.totemguard.loader.runtime;

import com.deathmotion.totemguard.api.event.events.TGPluginShutdownEvent;
import com.deathmotion.totemguard.api.fleet.FleetCache;
import com.deathmotion.totemguard.api.host.LoaderInfo;
import com.deathmotion.totemguard.host.LoaderController;
import com.deathmotion.totemguard.host.LoaderResolveException;
import com.deathmotion.totemguard.host.UpdateTarget;
import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.deathmotion.totemguard.loader.config.LoaderConfig;
import com.deathmotion.totemguard.loader.core.*;
import com.deathmotion.totemguard.loader.source.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

final class LoaderControllerImpl implements LoaderController {

    private final LoaderCore core;
    private final PluginRuntime runtime;

    LoaderControllerImpl(LoaderCore core, PluginRuntime runtime) {
        this.core = core;
        this.runtime = runtime;
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    private static LoaderResolveException.Reason reasonOf(Throwable t) {
        return switch (LoaderCore.classifyResolveFailure(t)) {
            case UNREACHABLE -> LoaderResolveException.Reason.UNREACHABLE;
            case UPSTREAM_ERROR -> LoaderResolveException.Reason.UPSTREAM_ERROR;
            case NO_MATCH -> LoaderResolveException.Reason.NO_MATCH;
            case GATE_REJECTED -> LoaderResolveException.Reason.GATE_REJECTED;
            case RESOLVE_FAILED -> LoaderResolveException.Reason.RESOLVE_FAILED;
        };
    }

    @Override
    public @NotNull LoaderInfo info() {
        LoaderConfig config = core.lastConfig();
        LoaderResult result = core.lastResult();

        String configuredSource = config != null ? config.source().name() : "UNKNOWN";
        String configuredVersion = config != null ? config.version() : "UNKNOWN";
        String loadedVersion = runtime.loadedVersion();
        if (loadedVersion == null && result != null) {
            loadedVersion = result.pluginVersion();
        }
        if (loadedVersion == null) {
            loadedVersion = "UNKNOWN";
        }

        return new LoaderInfo(
                LoaderManifest.loaderVersion(),
                configuredSource,
                configuredVersion,
                loadedVersion,
                readStagedVersion()
        );
    }

    private String readStagedVersion() {
        LoaderPaths paths = runtime.paths();
        if (!Files.isRegularFile(paths.stagedMeta()) || !Files.isRegularFile(paths.stagedJar())) {
            return null;
        }
        try {
            Map<String, String> meta = new HashMap<>();
            for (String line : Files.readAllLines(paths.stagedMeta(), StandardCharsets.UTF_8)) {
                int equals = line.indexOf('=');
                if (equals > 0) {
                    meta.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
                }
            }
            return meta.get("version");
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    public @NotNull UpdateTarget resolveTarget() throws IOException {
        try {
            Artifact artifact = core.resolveCurrentTarget();
            String source = sourceNameOf(artifact);
            return new UpdateTarget(source, artifact.version(), null, -1L, artifact.fileName());
        } catch (LoaderResolveException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LoaderResolveException(reasonOf(ex),
                    "Failed to resolve loader target: " + ex.getMessage(), ex);
        }
    }

    private String sourceNameOf(Artifact artifact) {
        String label = artifact.sourceLabel();
        if (label == null) return "UNKNOWN";
        String upper = label.toUpperCase(Locale.ROOT);
        if (upper.startsWith("GITHUB")) return "GITHUB";
        if (upper.startsWith("MODRINTH")) return "MODRINTH";
        if (upper.startsWith("LOCAL")) return "LOCAL";
        return "UNKNOWN";
    }

    @Override
    public byte @NotNull [] download(@NotNull UpdateTarget target) throws IOException {
        try {
            Artifact artifact = core.resolveCurrentTarget();
            if (!artifact.version().equals(target.version())) {
                Logger logger = runtime.core().logger();
                logger.warning("Resolved artifact version " + artifact.version()
                        + " differs from requested " + target.version() + ". Downloading the resolved one.");
            }
            return core.downloadArtifact(artifact);
        } catch (LoaderResolveException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LoaderResolveException(reasonOf(ex),
                    "Failed to download " + target.version() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void stageJar(byte @NotNull [] bytes, @NotNull UpdateTarget target) throws IOException {
        if (target.sha256() == null) {
            throw new IOException("Cannot stage a target with no sha256");
        }
        PluginVersionGate.require(target.version(), target.source());
        String actual = sha256(bytes);
        if (!actual.equalsIgnoreCase(target.sha256())) {
            throw new IOException("Bytes hash " + actual + " did not match target sha256 " + target.sha256());
        }

        LoaderPaths paths = runtime.paths();
        StagedJar.write(paths, bytes, target.version(), target.source(), actual);

        // Defense in depth: verify the on-disk jar carries the embedded integrity stamp
        // before we leave it sitting for the next loader start to pick up.
        Logger logger = runtime.core().logger();
        JarIntegrityChecker checker = new JarIntegrityChecker(logger, "TotemGuard");
        if (!checker.verifyJar(paths.stagedJar())) {
            try {
                Files.deleteIfExists(paths.stagedJar());
                Files.deleteIfExists(paths.stagedMeta());
            } catch (IOException ignored) {
            }
            throw new IOException("Staged jar failed integrity verification. Refusing to stage.");
        }

        logger.info("Staged TotemGuard " + target.version() + " for the next loader restart.");
    }

    @Override
    public @NotNull CompletableFuture<Void> restart(@NotNull TGPluginShutdownEvent.Reason reason) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Thread worker = new Thread(() -> {
            try {
                runtime.restart(reason);
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, "TotemGuard-Loader-Restart");
        worker.setDaemon(true);
        worker.start();
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> stop(@NotNull TGPluginShutdownEvent.Reason reason) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Thread worker = new Thread(() -> {
            try {
                runtime.stop(reason);
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, "TotemGuard-Loader-Stop");
        worker.setDaemon(true);
        worker.start();
        return future;
    }

    @Override
    public void attachFleetCache(@Nullable FleetCache cache) {
        core.fleetCacheRef().set(cache);
    }

    @Override
    public @Nullable FleetCache fleetCache() {
        return core.fleetCacheRef().current();
    }
}
