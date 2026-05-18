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

import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;

/**
 * Marker interface for every event that carries a {@link TGUser}. Subscribing
 * to {@code TGUserEvent.class} fans across every concrete user-bearing event
 * ({@link TGUserJoinEvent}, {@link TGUserQuitEvent}, {@link TGCheckEvent},
 * {@link TGModDetectionResolvedEvent}, etc.).
 */
public interface TGUserEvent extends TGEvent {

    /**
     * The TotemGuard user involved in this event.
     */
    @NotNull TGUser getUser();
}
