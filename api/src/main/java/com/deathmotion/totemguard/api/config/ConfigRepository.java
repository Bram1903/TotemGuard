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

package com.deathmotion.totemguard.api.config;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Central access point for TotemGuard's configuration. Reload is atomic, concurrent
 * readers see either the old or new snapshot but never a partial one.
 */
public interface ConfigRepository {

    /**
     * Filesystem directory where TotemGuard's YAML files live (typically
     * {@code plugins/TotemGuard/}), used to resolve add-on file paths and asset locations.
     */
    @NotNull Path configDirectory();

    /**
     * Current snapshot for the given file. Never {@code null}, missing or unparseable user
     * files fall back to the bundled defaults so reads always succeed.
     */
    @NotNull Config config(@NotNull ConfigFile file);

    /**
     * Re-reads one file from disk, atomically swapping the snapshot returned by
     * {@link #config(ConfigFile)}. Concurrent readers never see a half-loaded state.
     */
    void reload(@NotNull ConfigFile file);

    /**
     * Re-reads every managed file from disk in one pass. Each file is swapped
     * independently (no global lock across files).
     */
    void reloadAll();
}
