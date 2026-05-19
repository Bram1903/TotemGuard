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

package com.deathmotion.totemguard.api.punishment;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Read-only view of TotemGuard's punishment dispatch state. Does not expose a write API,
 * punishments are triggered by checks reaching thresholds, this repository just lets
 * callers ask whether the cross-server BAN lock is held for a player.
 */
public interface PunishmentRepository {

    /**
     * Whether a {@link PunishmentType#BAN} is in-flight or recently dispatched for the
     * player. Only BAN takes this lock, KICK and GENERIC always re-run. Reflects the
     * cross-server lock when Redis is enabled, otherwise local state only.
     */
    boolean isPunishmentQueued(@NotNull UUID uuid);
}
