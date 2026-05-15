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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a specific inner-jar artifact the loader could fetch and stage. Returned
 * by {@link LoaderController#resolveTarget()}.
 * <p>
 * The {@link #sha256()} field is {@code null} until after the bytes have actually
 * been downloaded (some sources do not advertise a SHA-256 ahead of time). It is
 * populated by {@link LoaderController#download(UpdateTarget)}.
 *
 * @param source    the source label ({@code GITHUB}, {@code MODRINTH}, {@code LOCAL}).
 *                  Free-form so the api survives future source additions
 * @param version   the resolved version string (e.g. {@code 3.0.5}). Stable identifier
 *                  suitable for use as a fleet dedup key
 * @param sha256    the SHA-256 fingerprint of the inner jar bytes, hex-encoded.
 *                  {@code null} when not yet known
 * @param sizeBytes the jar size in bytes, or {@code -1} when not known
 * @param fileName  the suggested filename for the staged jar
 */
public record UpdateTarget(
        @NotNull String source,
        @NotNull String version,
        @Nullable String sha256,
        long sizeBytes,
        @NotNull String fileName
) {

    public @NotNull String resolutionKey() {
        return source + ":" + version;
    }

    public @NotNull UpdateTarget withSha256(@NotNull String sha256) {
        return new UpdateTarget(source, version, sha256, sizeBytes, fileName);
    }
}
