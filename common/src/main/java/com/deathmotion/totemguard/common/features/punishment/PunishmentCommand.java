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

package com.deathmotion.totemguard.common.features.punishment;

import com.deathmotion.totemguard.api.punishment.PunishmentType;

import java.util.Locale;

public record PunishmentCommand(PunishmentType type, String raw, String banDuration) {

    public static PunishmentCommand parse(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (!trimmed.startsWith("[")) {
            return new PunishmentCommand(PunishmentType.GENERIC, trimmed, null);
        }
        int end = trimmed.indexOf(']');
        if (end <= 1) {
            return new PunishmentCommand(PunishmentType.GENERIC, trimmed, null);
        }
        String tag = trimmed.substring(1, end).trim().toUpperCase(Locale.ROOT);
        if (tag.startsWith("BAN:")) {
            String duration = trimmed.substring(5, end).trim();
            if (!duration.isEmpty() && duration.chars().noneMatch(Character::isWhitespace)) {
                return new PunishmentCommand(PunishmentType.BAN, trimmed.substring(end + 1).trim(), duration);
            }
        }
        try {
            PunishmentType type = PunishmentType.valueOf(tag);
            return new PunishmentCommand(type, trimmed.substring(end + 1).trim(), null);
        } catch (IllegalArgumentException ignored) {
            // Unknown tag, keep the line verbatim, treat as GENERIC.
            return new PunishmentCommand(PunishmentType.GENERIC, trimmed, null);
        }
    }
}
