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
 * Represents a TotemGuard version using Semantic Versioning, optionally with a
 * {@code -SNAPSHOT} suffix and an attached commit hash. Snapshot versions are
 * considered <em>older</em> than the matching release version.
 *
 * <p>Use {@link #of(int, int, int)} / {@link #fromString(String)} to construct instances.</p>
 */
public interface TGVersion extends Comparable<TGVersion> {

    /**
     * Constructs a release version (not a snapshot) with no commit hash.
     */
    static @NotNull TGVersion of(int major, int minor, int patch) {
        return new BasicTGVersion(major, minor, patch, false, null);
    }

    /**
     * Constructs a version with an explicit snapshot flag and no commit hash.
     */
    static @NotNull TGVersion of(int major, int minor, int patch, boolean snapshot) {
        return new BasicTGVersion(major, minor, patch, snapshot, null);
    }

    /**
     * Constructs a version with an explicit snapshot flag and optional commit hash.
     * {@code snapshotCommit} is the short git hash baked into the build, {@code null}
     * when unknown.
     */
    static @NotNull TGVersion of(int major, int minor, int patch, boolean snapshot, @Nullable String snapshotCommit) {
        return new BasicTGVersion(major, minor, patch, snapshot, snapshotCommit);
    }

    /**
     * Parses a version string in the form {@code major.minor[.patch][+commit][-SNAPSHOT]}.
     *
     * @throws IllegalArgumentException if the input does not match the expected format.
     */
    static @NotNull TGVersion fromString(@NotNull String version) {
        String versionWithoutSnapshot = version.replace("-SNAPSHOT", "");
        String[] largeParts = versionWithoutSnapshot.split("\\+");
        String[] parts = largeParts.length > 0 ? largeParts[0].split("\\.") : null;

        if (largeParts.length < 1 || largeParts.length > 2
                || parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("Version string must be in the format 'major.minor[.patch][+commit][-SNAPSHOT]', found '" + version + "' instead");
        }

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        boolean snapshot = version.contains("-SNAPSHOT");
        String snapshotCommit = largeParts.length > 1 ? largeParts[1] : null;

        return new BasicTGVersion(major, minor, patch, snapshot, snapshotCommit);
    }

    /**
     * Major component, incremented on incompatible changes.
     */
    int major();

    /**
     * Minor component, incremented on backwards-compatible feature additions.
     */
    int minor();

    /**
     * Patch component, incremented on backwards-compatible fixes. Defaults to {@code 0}.
     */
    int patch();

    /**
     * Whether this is a {@code -SNAPSHOT} build. Snapshots compare older than the matching release.
     */
    boolean snapshot();

    /**
     * Short commit hash baked into the build, or {@code null} when unknown or unbuilt.
     */
    @Nullable String snapshotCommit();

    /**
     * Lexicographic compare over (major, minor, patch, snapshot), where a snapshot of the
     * same numeric version sorts strictly before the matching release. Consistent with
     * {@link #isNewerThan} and {@link #isOlderThan}.
     */
    @Override
    default int compareTo(@NotNull TGVersion other) {
        int c = Integer.compare(this.major(), other.major());
        if (c != 0) return c;
        c = Integer.compare(this.minor(), other.minor());
        if (c != 0) return c;
        c = Integer.compare(this.patch(), other.patch());
        if (c != 0) return c;
        return Boolean.compare(other.snapshot(), this.snapshot());
    }

    /**
     * Whether this version sorts strictly after {@code other}.
     */
    default boolean isNewerThan(@NotNull TGVersion other) {
        return compareTo(other) > 0;
    }

    /**
     * Whether this version sorts strictly before {@code other}.
     */
    default boolean isOlderThan(@NotNull TGVersion other) {
        return compareTo(other) < 0;
    }

    /**
     * String form without commit or snapshot suffix, e.g. {@code "3.0.0"}.
     */
    default String toStringWithoutSnapshot() {
        return major() + "." + minor() + "." + patch();
    }

    /**
     * User-facing form that preserves {@code -SNAPSHOT} but drops the commit hash.
     */
    default String toDisplayString() {
        return snapshot() ? major() + "." + minor() + "." + patch() + "-SNAPSHOT" : toStringWithoutSnapshot();
    }
}
