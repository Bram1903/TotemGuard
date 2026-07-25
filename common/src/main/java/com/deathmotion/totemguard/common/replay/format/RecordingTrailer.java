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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public record RecordingTrailer(
        long endNanos,
        long frames,
        long droppedFrames,
        long judgedTicks,
        long coastedTicks,
        long declinedTicks,
        long flags,
        boolean pruned,
        long prunedFrames
) {
    public static RecordingTrailer read(DataInputStream in) throws IOException {
        return new RecordingTrailer(in.readLong(), in.readLong(), in.readLong(), in.readLong(),
                in.readLong(), in.readLong(), in.readLong(), in.readBoolean(), in.readLong());
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeLong(endNanos);
        out.writeLong(frames);
        out.writeLong(droppedFrames);
        out.writeLong(judgedTicks);
        out.writeLong(coastedTicks);
        out.writeLong(declinedTicks);
        out.writeLong(flags);
        out.writeBoolean(pruned);
        out.writeLong(prunedFrames);
    }

    public boolean degraded() {
        return droppedFrames > 0;
    }

    public long totalTicks() {
        return judgedTicks + coastedTicks + declinedTicks;
    }
}
