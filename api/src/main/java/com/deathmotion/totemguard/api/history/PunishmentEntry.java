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

import com.deathmotion.totemguard.api.punishment.PunishmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public projection of a single recorded punishment.
 *
 * @param id         primary key in {@code tg_punishments}, stable identifier
 * @param checkName  check whose threshold was reached, matches {@link com.deathmotion.totemguard.api.check.Check#getName()}
 * @param serverName server that dispatched the command, the local server name at write time
 * @param type       punishment category ({@link PunishmentType#GENERIC},
 *                   {@link PunishmentType#KICK}, {@link PunishmentType#BAN})
 * @param command    fully-expanded command string after placeholder substitution
 * @param debug      pre-rendered debug payload, {@code null} when the check produced none
 * @param createdAt  epoch milliseconds when the punishment was dispatched
 */
public record PunishmentEntry(
        long id,
        @NotNull String checkName,
        @NotNull String serverName,
        @NotNull PunishmentType type,
        @NotNull String command,
        @Nullable String debug,
        long createdAt
) {
}
