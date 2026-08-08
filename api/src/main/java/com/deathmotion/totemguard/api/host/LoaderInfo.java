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
 * Immutable snapshot of the loader's configuration and state.
 *
 * @param loaderVersion     version string of the TotemGuard loader plugin itself,
 *                          independent of the loaded plugin's version
 * @param configuredSource  configured source for resolving jars ({@code GITHUB},
 *                          {@code MODRINTH}, {@code LOCAL}), free-form to survive future
 *                          source kinds
 * @param configuredVersion configured version pin ({@code LATEST}, {@code GIT}, or a
 *                          concrete value like {@code 3.0.5}), free-form for the same reason
 * @param loadedVersion     version string of the TotemGuard plugin currently running
 *                          inside the loader
 * @param stagedVersion     version staged for the next restart, or {@code null} when no
 *                          update is staged
 */
public record LoaderInfo(
        @NotNull String loaderVersion,
        @NotNull String configuredSource,
        @NotNull String configuredVersion,
        @NotNull String loadedVersion,
        @Nullable String stagedVersion
) {
}
