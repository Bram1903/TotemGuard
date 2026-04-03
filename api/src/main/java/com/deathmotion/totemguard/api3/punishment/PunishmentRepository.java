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

package com.deathmotion.totemguard.api3.punishment;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PunishmentRepository {

    /**
     * Returns whether a punishment for the given player is currently queued or in-flight.
     *
     * <p>When Redis-backed caching is enabled, this reflects the shared cross-server lock
     * used to prevent duplicate punishments. Otherwise, this reflects only the
     * local in-flight punishment attempt on the current server.</p>
     *
     * @param uuid the player UUID
     * @return {@code true} if a punishment lock currently exists, otherwise {@code false}
     */
    boolean isPunishmentQueued(@NotNull UUID uuid);
}
