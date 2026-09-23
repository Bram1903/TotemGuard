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

package com.deathmotion.totemguard.common.player.inventory.screen;

final class ScreenChange {

    private final Kind kind;
    private final int value;
    private final long opensAfter;

    ScreenChange(Kind kind, int value, long opensAfter) {
        this.kind = kind;
        this.value = value;
        this.opensAfter = opensAfter;
    }

    Kind kind() {
        return kind;
    }

    int value() {
        return value;
    }

    boolean possiblyApplied(long confirmedOrdinal) {
        return confirmedOrdinal >= opensAfter;
    }

    boolean closes() {
        return kind == Kind.CLOSE || kind == Kind.DISPLACE || kind == Kind.RESPAWN;
    }

    enum Kind {
        OPEN,
        CLOSE,
        DISPLACE,
        RESPAWN,
        SELECT,
        SPRINT
    }
}
