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

import com.deathmotion.totemguard.common.replay.format.ReplayFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class RetentionSweep {

    private RetentionSweep() {
    }

    public static Result run(Path directory, long maxBytes, Logger logger) {
        if (maxBytes <= 0L || !Files.isDirectory(directory)) return Result.EMPTY;

        List<Entry> entries = new ArrayList<>();
        try (Stream<Path> walk = Files.list(directory)) {
            for (Path path : walk.toList()) {
                if (!Files.isRegularFile(path)) continue;
                if (!path.getFileName().toString().endsWith(ReplayFormat.EXTENSION)) continue;
                entries.add(new Entry(path, Files.size(path), Files.getLastModifiedTime(path).toMillis()));
            }
        } catch (IOException failure) {
            logger.warning("[Replay] could not read " + directory + " to sweep it: " + failure.getMessage());
            return Result.EMPTY;
        }

        entries.sort(Comparator.comparingLong((Entry entry) -> entry.modified).reversed());

        long kept = 0L;
        long freed = 0L;
        int deleted = 0;
        for (Entry entry : entries) {
            if (kept + entry.size <= maxBytes) {
                kept += entry.size;
                continue;
            }
            try {
                Files.deleteIfExists(entry.path);
                Files.deleteIfExists(entry.path.resolveSibling(
                        entry.path.getFileName() + ReplayFormat.GOLDEN_EXTENSION));
                Files.deleteIfExists(entry.path.resolveSibling(
                        entry.path.getFileName() + ReplayFormat.FLAGS_EXTENSION));
                freed += entry.size;
                deleted++;
            } catch (IOException failure) {
                logger.warning("[Replay] could not remove " + entry.path + ": " + failure.getMessage());
                kept += entry.size;
            }
        }

        if (deleted > 0) {
            logger.info("[Replay] retention sweep removed " + deleted + " recording(s) from "
                    + directory.getFileName() + ", freeing " + (freed / 1024L) + " KiB");
        }
        return new Result(deleted, freed, kept);
    }

    public record Result(int deleted, long freedBytes, long keptBytes) {

        public static final Result EMPTY = new Result(0, 0L, 0L);
    }

    private record Entry(Path path, long size, long modified) {
    }
}
