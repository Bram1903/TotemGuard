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

package com.deathmotion.totemguard.common.replay.viewer;

import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.replay.format.RecordingHeader;
import com.deathmotion.totemguard.common.replay.format.RecordingReader;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

final class ViewerStream implements AutoCloseable {

    private final Path file;

    @Getter
    private final RecordingHeader header;

    private RecordingReader reader;
    private @Nullable RecordingFrame pending;

    ViewerStream(Path file) throws IOException {
        this.file = file;
        this.reader = new RecordingReader(file);
        this.header = reader.getHeader();
        this.pending = reader.next();
    }

    @Nullable RecordingFrame poll(long untilNanos) throws IOException {
        RecordingFrame frame = pending;
        if (frame == null || frame.nanos() > untilNanos) return null;
        pending = reader.next();
        return frame;
    }

    boolean exhausted() {
        return pending == null;
    }

    long positionNanos() {
        RecordingFrame frame = pending;
        return frame == null ? Long.MAX_VALUE : frame.nanos();
    }

    void rewind() throws IOException {
        reader.close();
        reader = new RecordingReader(file);
        pending = reader.next();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
