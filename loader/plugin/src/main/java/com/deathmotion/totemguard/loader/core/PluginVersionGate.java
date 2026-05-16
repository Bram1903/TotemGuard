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

import java.io.IOException;
import java.util.Locale;

/**
 * Refuses TotemGuard versions below {@link #MINIMUM}. Older releases lack the jar
 * integrity stamp the loader's defense-in-depth relies on.
 */
public final class PluginVersionGate {

    public static final LoaderSemver MINIMUM = new LoaderSemver(3, 0, 0, true);

    private PluginVersionGate() {
    }

    /**
     * Channels (LATEST/EXPERIMENTAL/GIT) are accepted since they only resolve to a
     * concrete version inside {@link #isSupportedConcrete}; unparseable strings are
     * rejected so the loader never launches a jar it cannot prove is recent enough.
     */
    public static boolean isSupportedConcrete(String version) {
        if (version == null || version.isBlank()) return false;
        try {
            return !LoaderSemver.fromString(version.trim()).isOlderThan(MINIMUM);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static void require(String version, String sourceContext) throws IOException {
        if (!isSupportedConcrete(version)) {
            throw new IOException("Refusing to load TotemGuard " + version
                    + " from " + sourceContext + ": loader requires >= " + MINIMUM + ".");
        }
    }

    /**
     * Fails early when a user-supplied pin is parseable as semver and falls below
     * {@link #MINIMUM}. Channel tokens (LATEST/EXPERIMENTAL/GIT) and unparseable tokens
     * (tags, branch names) are deferred to the post-download {@link #require} check.
     */
    public static void rejectIfPinnedTooOld(String version, String context) throws IOException {
        if (version == null || version.isBlank()) return;
        String trimmed = version.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (upper.equals("LATEST") || upper.equals("EXPERIMENTAL") || upper.equals("GIT")) return;
        try {
            LoaderSemver parsed = LoaderSemver.fromString(trimmed);
            if (parsed.isOlderThan(MINIMUM)) {
                throw new IOException("Refusing to pin TotemGuard " + trimmed
                        + " in " + context + ": loader requires >= " + MINIMUM + ".");
            }
        } catch (IllegalArgumentException ignored) {
        }
    }
}
