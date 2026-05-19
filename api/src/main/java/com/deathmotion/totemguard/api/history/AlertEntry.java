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

package com.deathmotion.totemguard.api.history;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public projection of a single recorded alert.
 *
 * @param id            primary key in {@code tg_alerts}, stable identifier
 * @param checkName     check that flagged the player, matches {@link com.deathmotion.totemguard.api.check.Check#getName()}
 * @param serverName    server that wrote the alert, the local server name at write time
 * @param debug         rendered debug payload (template args substituted), or {@code null}
 *                      when the check produced none
 * @param clientBrand   client brand reported by the player (e.g. {@code vanilla},
 *                      {@code fabric}), captured at alert time
 * @param clientVersion Minecraft protocol version number captured at alert time
 * @param createdAt     epoch milliseconds when the alert was logged
 */
public record AlertEntry(
        long id,
        @NotNull String checkName,
        @NotNull String serverName,
        @Nullable String debug,
        @NotNull String clientBrand,
        int clientVersion,
        long createdAt
) {
}
