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

package com.deathmotion.totemguard.common.replay.prune;

import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.replay.format.RecordingReader;
import com.deathmotion.totemguard.common.replay.format.RecordingTrailer;
import com.deathmotion.totemguard.common.replay.format.RecordingWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

public final class RecordingPruner {

    private RecordingPruner() {
    }

    public static long prune(Path file, Set<Long> droppableFrames) throws IOException {
        if (droppableFrames.isEmpty()) return 0L;

        Path temporary = file.resolveSibling(file.getFileName() + ".pruning");
        long dropped = 0;
        try (RecordingReader reader = new RecordingReader(file);
             RecordingWriter writer = new RecordingWriter(temporary, reader.getHeader())) {
            long index = 0;
            RecordingFrame frame;
            while ((frame = reader.next()) != null) {
                long current = index++;
                if (frame instanceof RecordingFrame.Packet && droppableFrames.contains(current)) {
                    dropped++;
                    continue;
                }
                if (frame instanceof RecordingFrame.End end) {
                    RecordingTrailer old = end.trailer();
                    writer.write(new RecordingFrame.End(end.nanos(), new RecordingTrailer(
                            old.endNanos(), old.frames() - dropped, old.droppedFrames(),
                            old.judgedTicks(), old.coastedTicks(), old.declinedTicks(), old.flags(),
                            true, dropped)));
                    continue;
                }
                writer.write(frame);
            }
        } catch (IOException | RuntimeException failure) {
            Files.deleteIfExists(temporary);
            throw failure;
        }

        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        return dropped;
    }

}
