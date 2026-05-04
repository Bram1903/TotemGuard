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
 * Public projection of a single recorded alert. Equivalent to one row in the GUI history.
 *
 * @param id            primary key in {@code tg_alerts}; useful as a stable identifier
 *                      when threading user input back to a specific alert.
 * @param checkName     the check that flagged the player (e.g. {@code AutoTotemA}).
 * @param serverName    the name of the server that wrote the alert (multi-server installs).
 * @param debug         the rendered debug payload from the check, or {@code null} if the
 *                      check did not provide one. Template args are already substituted.
 * @param clientBrand   Minecraft client brand string (e.g. {@code vanilla}, {@code lunarclient}).
 * @param clientVersion protocol version number recorded on the matching profile.
 * @param createdAt     epoch ms when the alert was logged.
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
