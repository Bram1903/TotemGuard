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

package com.deathmotion.totemguard.common.replay.format;

public final class ReplayFormat {

    public static final int MAGIC = 0x54475243;
    public static final int VERSION = 1;
    public static final String EXTENSION = ".tgrec";
    public static final String GOLDEN_EXTENSION = ".golden";
    public static final String FLAGS_EXTENSION = ".flags";

    public static final int FILTER_VERSION = 2;
    public static final int STATE_TABLE_SIZE = 1 << 16;

    public static final byte FRAME_IN = 1;
    public static final byte FRAME_OUT = 2;
    public static final byte FRAME_TICK = 3;
    public static final byte FRAME_LOOP = 4;
    public static final byte FRAME_VERDICT = 5;
    public static final byte FRAME_MARK = 6;
    public static final byte FRAME_PROLOGUE_END = 7;
    public static final byte FRAME_END = 8;
    public static final byte FRAME_ATTACH = 9;
    public static final byte FRAME_FLAG = 10;
    public static final byte FRAME_IN_REPEAT = 11;
    public static final byte FRAME_OUT_REPEAT = 12;
    public static final byte FRAME_INTEGRATION = 13;

    public static final int REPEAT_MIN_PAYLOAD = 256;
    public static final int REPEAT_MAX_ENTRIES = 4096;
    public static final int REPEAT_MAX_BYTES = 32 << 20;

    private ReplayFormat() {
    }
}
