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

package com.deathmotion.totemguard.api.event.events;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common supertype for events fired around a {@link Check}. Subscribe to react to every
 * check-driven event ({@link TGUserFlagEvent}, {@link TGUserPunishEvent}, future subtypes)
 * with one handler.
 */
public interface TGCheckEvent extends TGUserEvent, Cancellable {

    /**
     * The check that produced this event. Same instance the violation was recorded
     * against, so {@link Check#getViolations()} reflects the count after this flag.
     */
    @NotNull Check getCheck();

    /**
     * Pre-rendered debug payload from the check (already template-substituted), or
     * {@code null} when the check did not produce debug text for this dispatch. The same
     * string that ends up in {@code tg_alerts.debug} and the staff debug toggle.
     */
    @Nullable String getDebug();
}
