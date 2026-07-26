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

package com.deathmotion.totemguard.common.replay.retention;

public final class RetentionPolicy {

    public static final long SEGMENT_NANOS = 10_000_000_000L;

    public static final long POST_ROLL_NANOS = 10_000_000_000L;

    public static final long MAX_SESSION_NANOS = 300_000_000_000L;

    public static final int MAX_SEGMENT_FRAMES = 12_000;

    public static final int MAX_BLOCK_UNDO = 8_192;

    public static final int MAX_COLUMN_PRE_IMAGES = 512;

    public static final int MAX_STICKY_ENTRIES = 4_096;

    public static final int MAX_STICKY_BYTES = 512 * 1024;

    public static final int MAX_EFFECT_LOG = 256;

    public static final int MAX_PLAYER_INFO_LOG = 32;

    public static final double ENTITY_RADIUS = 40.0;

    public static final int COLUMN_RADIUS = 3;

    public static final int VISITED_DILATION = 2;

    public static final int MAX_PROLOGUE_COLUMNS = 1_024;

    public static final int DIGEST_RADIUS = 1;

    public static final int DIGEST_HEIGHT = 8;

    private RetentionPolicy() {
    }
}
