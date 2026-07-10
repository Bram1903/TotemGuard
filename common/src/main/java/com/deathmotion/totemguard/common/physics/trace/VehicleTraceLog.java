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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;

public final class VehicleTraceLog {

    private static final int CAPACITY = 128;
    private static final int DUMP_ROWS = 40;
    private static final long RATE_LIMIT_NANOS = 5_000_000_000L;

    private final String[] ring = new String[CAPACITY];
    private int writeIndex;
    private int size;
    private long lastDumpNanos;

    public void record(String line) {
        ring[writeIndex] = line;
        writeIndex = (writeIndex + 1) & (CAPACITY - 1);
        if (size < CAPACITY) size++;
    }

    public boolean dump(TGPlayer player, String cause) {
        long now = System.nanoTime();
        if (now - lastDumpNanos < RATE_LIMIT_NANOS) return false;
        lastDumpNanos = now;

        TGPlatform platform = TGPlatform.getInstance();
        String name = player.getUser().getName();
        platform.getLogger().info("[PhysicsTrace] " + name
                + " cause=" + cause
                + " client=" + player.getClientVersion().getReleaseName()
                + " ticks=" + size);

        int n = Math.min(DUMP_ROWS, size);
        int start = (writeIndex - n + CAPACITY) & (CAPACITY - 1);
        for (int i = 0; i < n; i++) {
            platform.getLogger().info("[PhysicsTrace] " + name + " " + ring[(start + i) & (CAPACITY - 1)]);
        }
        return true;
    }

    public void clear() {
        writeIndex = 0;
        size = 0;
    }
}
