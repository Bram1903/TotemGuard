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

package com.deathmotion.totemguard.api.host;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of the loader's configuration and current state. Consumers obtain
 * one via {@link com.deathmotion.totemguard.api.TotemGuardAPI#getLoaderInfo()} when the
 * inner plugin is being driven by the TotemGuard loader.
 *
 * @param loaderVersion     the loader plugin's own version
 * @param configuredSource  the configured source for resolving inner jars
 *                          (e.g. {@code GITHUB}, {@code MODRINTH}, {@code LOCAL}).
 *                          A free-form label so the api can survive future source additions
 * @param configuredVersion the configured version pin
 *                          (e.g. {@code LATEST}, {@code GIT}, or a concrete version like {@code 3.0.5}).
 *                          A free-form label for the same reason
 * @param loadedVersion     the version of the inner plugin currently running
 * @param stagedVersion     a version that has been staged for the next loader restart,
 *                          or {@code null} when no jar is staged
 */
public record LoaderInfo(
        @NotNull String loaderVersion,
        @NotNull String configuredSource,
        @NotNull String configuredVersion,
        @NotNull String loadedVersion,
        @Nullable String stagedVersion
) {
}
