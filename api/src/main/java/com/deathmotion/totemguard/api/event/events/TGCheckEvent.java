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
 * Common supertype for events fired around a {@link Check}. Subscribe to
 * {@code TGCheckEvent} when you want to react to every check-driven event
 * ({@link TGUserFlagEvent}, {@link TGUserPunishEvent}, and any future
 * subtypes) with one handler.
 */
public interface TGCheckEvent extends TGUserEvent, Cancellable {

    /**
     * The check responsible for the event.
     */
    @NotNull Check getCheck();

    /**
     * Optional debug information formatted by the check. {@code null} when the
     * check did not produce debug text for this dispatch.
     */
    @Nullable String getDebug();
}
