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
     * Returns whether a {@link PunishmentType#BAN} for the given player is currently
     * in-flight or recently dispatched.
     *
     * <p>Only BAN-type punishments take this lock; KICK and GENERIC commands are
     * always allowed to run again so re-flagging a player triggers another kick or
     * announcement immediately. When Redis-backed caching is enabled, this reflects
     * the shared cross-server ban lock; otherwise it reflects only the local node.</p>
     *
     * @param uuid the player UUID
     * @return {@code true} if a ban lock currently exists, otherwise {@code false}
     */
    boolean isPunishmentQueued(@NotNull UUID uuid);
}
