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

public record LoaderSemver(int major, int minor, int patch, boolean snapshot) implements Comparable<LoaderSemver> {

    public static LoaderSemver fromString(String version) {
        String trimmed = version.trim();
        boolean snapshot = trimmed.contains("-SNAPSHOT");
        String core = trimmed.replace("-SNAPSHOT", "");
        int plus = core.indexOf('+');
        if (plus >= 0) core = core.substring(0, plus);

        String[] parts = core.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("Version must be 'major.minor[.patch][+commit][-SNAPSHOT]', got '" + version + "'");
        }
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return new LoaderSemver(major, minor, patch, snapshot);
    }

    @Override
    public int compareTo(LoaderSemver other) {
        int c = Integer.compare(this.major, other.major);
        if (c != 0) return c;
        c = Integer.compare(this.minor, other.minor);
        if (c != 0) return c;
        c = Integer.compare(this.patch, other.patch);
        if (c != 0) return c;
        return Boolean.compare(other.snapshot, this.snapshot);
    }

    public boolean isOlderThan(LoaderSemver other) {
        return compareTo(other) < 0;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (snapshot ? "-SNAPSHOT" : "");
    }
}
