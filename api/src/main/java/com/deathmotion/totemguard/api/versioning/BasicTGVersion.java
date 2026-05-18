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
 * Default {@link TGVersion} implementation. Prefer {@link TGVersion#of} or
 * {@link TGVersion#fromString}, this record is exposed only for tooling that needs the
 * concrete type.
 *
 * @param major          semver major, incremented on incompatible changes
 * @param minor          semver minor, incremented on backwards-compatible feature additions
 * @param patch          semver patch, incremented on backwards-compatible fixes
 * @param snapshot       whether this is a {@code -SNAPSHOT} build, snapshots compare older
 *                       than the matching release
 * @param snapshotCommit short commit hash baked into the build, {@code null} when unknown
 *                       or when this is not a snapshot build
 */
public record BasicTGVersion(int major, int minor, int patch, boolean snapshot,
                             @Nullable String snapshotCommit) implements TGVersion {
    /**
     * Canonical display string. Releases render as {@code major.minor.patch}, snapshots
     * with a commit hash render as {@code major.minor.patch+commit-SNAPSHOT}, snapshots
     * without a commit hash collapse to the release form.
     */
    @Override
    public @NotNull String toString() {
        return major + "." + minor + "." + patch + (snapshot && snapshotCommit != null ? ("+" + snapshotCommit + "-SNAPSHOT") : "");
    }
}
