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

package com.deathmotion.totemguard.replay;

import com.deathmotion.totemguard.common.replay.format.ReplayFormat;
import com.deathmotion.totemguard.common.replay.format.TickDigest;
import com.deathmotion.totemguard.common.replay.playback.ReplayFlag;
import com.deathmotion.totemguard.common.replay.playback.ReplayResult;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GoldenStore {

    private static final String HEADER = "# TotemGuard golden trace v1. One row per tick, full double precision.";

    private GoldenStore() {
    }

    public static Path goldenPath(Path recording) {
        return sibling(recording, ReplayFormat.GOLDEN_EXTENSION);
    }

    public static Path flagsPath(Path recording) {
        return sibling(recording, ReplayFormat.FLAGS_EXTENSION);
    }

    private static Path sibling(Path recording, String extension) {
        String name = recording.getFileName().toString();
        if (name.endsWith(ReplayFormat.EXTENSION)) {
            name = name.substring(0, name.length() - ReplayFormat.EXTENSION.length());
        }
        return recording.resolveSibling(name + extension);
    }

    public static void write(Path recording, ReplayResult result) throws IOException {
        List<String> golden = new ArrayList<>(result.digests().size() + 1);
        golden.add(HEADER);
        for (TickDigest digest : result.digests()) {
            golden.add(digest.toGoldenRow());
        }
        Files.write(goldenPath(recording), golden, StandardCharsets.UTF_8);

        List<String> flagRows = new ArrayList<>(result.flags().size());
        for (ReplayFlag flag : result.flags()) {
            flagRows.add(flag.toLogRow());
        }
        Files.write(flagsPath(recording), flagRows, StandardCharsets.UTF_8);
    }

    public static @Nullable Diff diff(Path recording, ReplayResult result) throws IOException {
        Path golden = goldenPath(recording);
        if (!Files.isRegularFile(golden)) return new Diff(-1, "no golden committed yet", null, null);

        List<String> committed = Files.readAllLines(golden, StandardCharsets.UTF_8);
        List<String> current = new ArrayList<>(result.digests().size() + 1);
        current.add(HEADER);
        for (TickDigest digest : result.digests()) {
            current.add(digest.toGoldenRow());
        }

        int shared = Math.min(committed.size(), current.size());
        for (int i = 1; i < shared; i++) {
            if (!committed.get(i).equals(current.get(i))) {
                return new Diff(i, "row differs", committed.get(i), current.get(i));
            }
        }
        if (committed.size() != current.size()) {
            return new Diff(shared, "tick count differs: golden " + (committed.size() - 1)
                    + ", replay " + (current.size() - 1), null, null);
        }
        return null;
    }

    public static @Nullable FlagDiff diffFlags(Path recording, ReplayResult result) throws IOException {
        Path flags = flagsPath(recording);
        List<String> committed = Files.isRegularFile(flags)
                ? Files.readAllLines(flags, StandardCharsets.UTF_8)
                : List.of();
        List<String> current = new ArrayList<>(result.flags().size());
        for (ReplayFlag flag : result.flags()) {
            current.add(flag.toLogRow());
        }
        if (committed.equals(current)) return null;
        return new FlagDiff(committed.size(), current.size());
    }

    public record Diff(int row, String reason, @Nullable String golden, @Nullable String replay) {
    }

    public record FlagDiff(int goldenCount, int replayCount) {
    }
}
