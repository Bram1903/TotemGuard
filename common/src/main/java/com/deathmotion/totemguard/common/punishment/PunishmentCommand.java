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

package com.deathmotion.totemguard.common.punishment;

import com.deathmotion.totemguard.api3.punishment.PunishmentType;

import java.util.Locale;

public record PunishmentCommand(PunishmentType type, String raw) {

    public static PunishmentCommand parse(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (!trimmed.startsWith("[")) {
            return new PunishmentCommand(PunishmentType.GENERIC, trimmed);
        }
        int end = trimmed.indexOf(']');
        if (end <= 1) {
            return new PunishmentCommand(PunishmentType.GENERIC, trimmed);
        }
        String tag = trimmed.substring(1, end).trim().toUpperCase(Locale.ROOT);
        try {
            PunishmentType type = PunishmentType.valueOf(tag);
            return new PunishmentCommand(type, trimmed.substring(end + 1).trim());
        } catch (IllegalArgumentException ignored) {
            // Unknown tag — keep the line verbatim, treat as GENERIC.
            return new PunishmentCommand(PunishmentType.GENERIC, trimmed);
        }
    }
}
