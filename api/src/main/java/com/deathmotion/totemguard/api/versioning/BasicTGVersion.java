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

package com.deathmotion.totemguard.api.versioning;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default {@link TGVersion} implementation. Most callers should construct versions
 * via {@link TGVersion#of} or {@link TGVersion#fromString}; this record is exposed
 * only so third-party tooling can reach the concrete type when needed.
 */
public record BasicTGVersion(int major, int minor, int patch, boolean snapshot,
                             @Nullable String snapshotCommit) implements TGVersion {
    @Override
    public @NotNull String toString() {
        return major + "." + minor + "." + patch + (snapshot && snapshotCommit != null ? ("+" + snapshotCommit + "-SNAPSHOT") : "");
    }
}
