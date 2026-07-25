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

package com.deathmotion.totemguard.common.replay;

import com.deathmotion.totemguard.common.replay.format.*;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public record RecordingSummary(
        String path,
        long bytes,
        long modifiedMillis,
        boolean truncated,
        String pluginVersion,
        String gitHash,
        int serverProtocol,
        int clientProtocol,
        boolean supportsEndTick,
        String playerName,
        UUID playerUuid,
        long startEpochMillis,
        RecordingLabel label,
        String scenario,
        String note,
        List<String> tags,
        boolean observeOnly,
        String preset,
        boolean viaTable,
        boolean sealed,
        long elapsedNanos,
        long frames,
        long droppedFrames,
        long prunedFrames,
        boolean pruned,
        long judgedTicks,
        long coastedTicks,
        long declinedTicks,
        long flags,
        List<String> marks,
        Map<String, Integer> flagCounts
) {

    private static final int MARK_LIMIT = 12;

    public static RecordingSummary read(Path root, Path file) throws IOException {
        long bytes = Files.size(file);
        long modified = Files.getLastModifiedTime(file).toMillis();
        List<String> marks = new ArrayList<>();
        Map<String, Integer> flagCounts = new LinkedHashMap<>();
        long overflowMarks = 0L;

        RecordingHeader header;
        RecordingTrailer trailer = null;
        boolean truncated;

        try (RecordingReader reader = new RecordingReader(file)) {
            header = reader.getHeader();
            RecordingFrame frame;
            while ((frame = reader.next()) != null) {
                if (frame instanceof RecordingFrame.End end) {
                    trailer = end.trailer();
                } else if (frame instanceof RecordingFrame.Mark mark) {
                    if (marks.size() < MARK_LIMIT) marks.add(mark.label());
                    else overflowMarks++;
                } else if (frame instanceof RecordingFrame.Flag flag) {
                    flagCounts.merge(flag.check(), 1, Integer::sum);
                }
            }
            truncated = reader.isTruncated();
        }

        if (overflowMarks > 0) marks.add("+" + overflowMarks + " more");

        return new RecordingSummary(
                relative(root, file),
                bytes,
                modified,
                truncated,
                header.pluginVersion(),
                header.gitHash(),
                header.serverProtocol(),
                header.clientProtocol(),
                header.supportsEndTick(),
                header.playerName(),
                header.playerUuid(),
                header.startEpochMillis(),
                header.label(),
                header.scenario(),
                header.note(),
                header.tags(),
                header.observeOnly(),
                header.physicsConfig().preset(),
                header.blockStateTable() != null,
                trailer != null,
                trailer == null ? 0L : trailer.endNanos() - header.startNanos(),
                trailer == null ? 0L : trailer.frames(),
                trailer == null ? 0L : trailer.droppedFrames(),
                trailer == null ? 0L : trailer.prunedFrames(),
                trailer != null && trailer.pruned(),
                trailer == null ? 0L : trailer.judgedTicks(),
                trailer == null ? 0L : trailer.coastedTicks(),
                trailer == null ? 0L : trailer.declinedTicks(),
                trailer == null ? 0L : trailer.flags(),
                List.copyOf(marks),
                Map.copyOf(flagCounts));
    }

    private static String relative(Path root, Path file) {
        Path resolved = file.startsWith(root) ? root.relativize(file) : file.getFileName();
        return resolved.toString().replace('\\', '/');
    }

    public String display() {
        return RecordingLibrary.display(path);
    }

    public String clientName() {
        ClientVersion version = ClientVersion.getById(clientProtocol);
        return version == null || version == ClientVersion.UNKNOWN
                ? "protocol " + clientProtocol
                : version.getReleaseName();
    }

    public String length() {
        return sealed ? ReplayText.elapsed(elapsedNanos) : "unknown";
    }

    public boolean degraded() {
        return droppedFrames > 0;
    }

    public @Nullable String topFlag() {
        String top = null;
        int best = 0;
        for (Map.Entry<String, Integer> entry : flagCounts.entrySet()) {
            if (entry.getValue() <= best) continue;
            best = entry.getValue();
            top = entry.getKey();
        }
        return top;
    }
}
