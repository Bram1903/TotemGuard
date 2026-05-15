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

package com.deathmotion.totemguard.host;

import com.deathmotion.totemguard.api.event.impl.TGPluginShutdownEvent;
import com.deathmotion.totemguard.api.host.LoaderInfo;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Loader-side control surface exposed to the inner plugin when running under the
 * TotemGuard loader. Obtained via {@link TGPluginHost#loaderController()} which is
 * {@link java.util.Optional#empty()} in standalone mode.
 * <p>
 * Methods on this interface are safe to call from any thread.
 */
public interface LoaderController {

    /**
     * A point-in-time snapshot of loader configuration and state.
     */
    @NotNull LoaderInfo info();

    /**
     * Resolves the inner-jar artifact that this instance's local loader config
     * currently points at. Performs whatever network calls the source requires
     * (GitHub or Modrinth API).
     * <p>
     * Does not download the jar bytes. Use {@link #download(UpdateTarget)} for that.
     */
    @NotNull UpdateTarget resolveTarget() throws IOException;

    /**
     * Downloads the jar bytes for the given target. Validates source-native checksums
     * (e.g. Modrinth SHA-512) internally and returns the raw bytes. The returned bytes
     * are guaranteed to match the source's published checksum.
     * <p>
     * Callers must verify the SHA-256 of the returned bytes themselves before passing
     * them to {@link #stageJar(byte[], UpdateTarget)} if the bytes were sourced from
     * outside this controller (for example, fetched from a Redis blob).
     */
    byte @NotNull [] download(@NotNull UpdateTarget target) throws IOException;

    /**
     * Stages the given bytes as the next inner jar. Validates that the SHA-256 of the
     * bytes matches {@link UpdateTarget#sha256()} and that the bytes carry an embedded
     * integrity stamp, then atomically writes them to the loader cache. The next
     * loader-driven restart will pick up this staged jar instead of re-resolving.
     *
     * @throws IOException if validation fails or the write cannot be completed
     */
    void stageJar(byte @NotNull [] bytes, @NotNull UpdateTarget target) throws IOException;

    /**
     * Asynchronously restarts the inner plugin via the same path used by
     * {@code /tgloader restart}. If a jar has been staged via
     * {@link #stageJar(byte[], UpdateTarget)} it is picked up by the restart.
     * <p>
     * The returned future completes once the new inner plugin has fully enabled, or
     * exceptionally if startup failed.
     *
     * @param reason fired as the shutdown {@code Reason} so consumers can distinguish
     *               a manual restart from an update-triggered one
     */
    @NotNull CompletableFuture<Void> restart(@NotNull TGPluginShutdownEvent.Reason reason);

    /**
     * Convenience that restarts with {@link TGPluginShutdownEvent.Reason#LOADER_RESTART}.
     */
    default @NotNull CompletableFuture<Void> restart() {
        return restart(TGPluginShutdownEvent.Reason.LOADER_RESTART);
    }

    /**
     * Asynchronously stops the inner plugin without restarting it. The loader stays
     * online and accepts {@code /tgloader start} to bring TotemGuard back up.
     * <p>
     * The returned future completes once the inner plugin has fully shut down, or
     * exceptionally if the teardown threw.
     *
     * @param reason fired as the shutdown {@code Reason} so consumers can distinguish
     *               an operator-driven stop from a server stop
     */
    @NotNull CompletableFuture<Void> stop(@NotNull TGPluginShutdownEvent.Reason reason);

    /**
     * Convenience that stops with {@link TGPluginShutdownEvent.Reason#LOADER_STOP}.
     */
    default @NotNull CompletableFuture<Void> stop() {
        return stop(TGPluginShutdownEvent.Reason.LOADER_STOP);
    }
}
