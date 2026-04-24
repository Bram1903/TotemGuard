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

package com.deathmotion.totemguard.common.database.model;

/**
 * One row per distinct check a player has ever flagged, with a cached count
 * so the filter list can show "TickA (42)" without a second query per row.
 *
 * <p>Because the underlying join is keyed by {@code check_id}, renaming or
 * replacing a check in code simply surfaces a new entry here — historical
 * rows keep pointing at the old catalog row and still show up under their
 * old name.</p>
 */
public record AlertCheckSummary(String checkName, int alertCount) {
}
