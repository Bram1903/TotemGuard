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

package com.deathmotion.totemguard.common.replay.capture;

import com.deathmotion.totemguard.common.replay.format.RecordingLabel;

import java.util.List;
import java.util.UUID;

public record ArmedRecording(
        UUID uuid,
        String name,
        RecordingLabel label,
        String scenario,
        String note,
        List<String> tags,
        long expiresAtMillis,
        boolean shadow
) {
    public boolean expired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }

    public long secondsLeft(long nowMillis) {
        return Math.max(0L, (expiresAtMillis - nowMillis + 999L) / 1000L);
    }
}
