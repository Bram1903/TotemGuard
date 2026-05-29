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

package com.deathmotion.totemguard.api.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of a {@link LoaderControl#update()} call.
 *
 * @param outcome       what the loader did, never {@code null}
 * @param targetVersion the newest version the loader resolved, or {@code null} when
 *                      resolution itself failed
 * @param detail        a short human-readable explanation suitable for surfacing to an
 *                      operator (e.g. in a Discord reply), or {@code null}
 */
public record UpdateResult(
        @NotNull Outcome outcome,
        @Nullable String targetVersion,
        @Nullable String detail
) {

    /**
     * Discrete results of an update attempt.
     */
    public enum Outcome {
        /**
         * The resolved version matches the running version; nothing was changed.
         */
        UP_TO_DATE,
        /**
         * A newer jar was downloaded and staged, and a restart onto it was initiated.
         */
        UPDATING,
        /**
         * Resolution, download, or staging failed; see {@link UpdateResult#detail()}.
         */
        FAILED
    }
}
