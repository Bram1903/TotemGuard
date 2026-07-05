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

package com.deathmotion.totemguard.common.physics.trace;

import java.util.function.Consumer;

public final class TickRecorder {

    public static final int CAPACITY = 128;

    private final TraceFrame[] ring = new TraceFrame[CAPACITY];
    private int writeIndex;
    private int size;

    public TickRecorder() {
        for (int i = 0; i < CAPACITY; i++) ring[i] = new TraceFrame();
    }

    public void record(TraceFrame frame) {
        ring[writeIndex].copyFrom(frame);
        writeIndex = (writeIndex + 1) & (CAPACITY - 1);
        if (size < CAPACITY) size++;
    }

    public int size() {
        return size;
    }

    public void forEachRecent(int count, Consumer<TraceFrame> consumer) {
        int n = Math.min(count, size);
        int start = (writeIndex - n + CAPACITY) & (CAPACITY - 1);
        for (int i = 0; i < n; i++) {
            consumer.accept(ring[(start + i) & (CAPACITY - 1)]);
        }
    }

    public void clear() {
        writeIndex = 0;
        size = 0;
    }
}
