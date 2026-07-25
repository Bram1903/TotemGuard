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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.List;

public final class RecordingEditor {

    private static final String STAGING_SUFFIX = ".rewrite";

    private RecordingEditor() {
    }

    public static Path apply(RecordingLibrary library, Path file, RecordingLabel label,
                             String scenario, List<String> tags, String note) throws IOException {
        String cleanScenario = RecordingLibrary.scenarioFrom(scenario);
        List<String> cleanTags = RecordingLibrary.normalizeTags(tags);
        if (!RecordingLibrary.validScenario(cleanScenario)) {
            throw new IOException(scenario + " cannot be a file name");
        }

        RecordingHeader original;
        try (RecordingReader reader = new RecordingReader(file)) {
            original = reader.getHeader();
        }

        if (original.label() == label
                && original.scenario().equals(cleanScenario)
                && original.tags().equals(cleanTags)
                && original.note().equals(note)) {
            return file;
        }

        RecordingHeader rewritten = withMetadata(original, label, cleanScenario, cleanTags, note);
        boolean relocate = original.label() != label || !original.scenario().equals(cleanScenario);
        Path target = relocate ? library.allocate(label, cleanScenario, original.clientTag()) : file;

        FileTime modified = Files.getLastModifiedTime(file);
        Path staging = file.resolveSibling(file.getFileName() + STAGING_SUFFIX);
        try {
            transcribe(file, staging, rewritten);
            confirm(staging, rewritten);
            Files.createDirectories(target.getParent());
            Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staging);
        }

        if (!target.equals(file)) Files.deleteIfExists(file);
        Files.setLastModifiedTime(target, modified);
        return target;
    }

    private static void transcribe(Path source, Path staging, RecordingHeader header) throws IOException {
        try (RecordingReader reader = new RecordingReader(source);
             RecordingWriter writer = new RecordingWriter(staging, header)) {
            RecordingFrame frame;
            while ((frame = reader.next()) != null) writer.write(frame);
        }
    }

    private static void confirm(Path staging, RecordingHeader expected) throws IOException {
        try (RecordingReader reader = new RecordingReader(staging)) {
            RecordingHeader written = reader.getHeader();
            if (written.label() != expected.label()
                    || !written.scenario().equals(expected.scenario())
                    || !written.tags().equals(expected.tags())
                    || !written.note().equals(expected.note())) {
                throw new IOException("the rewritten recording lost its new metadata");
            }
        }
    }

    private static RecordingHeader withMetadata(RecordingHeader header, RecordingLabel label,
                                                String scenario, List<String> tags, String note) {
        return new RecordingHeader(
                header.formatVersion(),
                header.pluginVersion(),
                header.gitHash(),
                header.serverProtocol(),
                header.clientProtocol(),
                header.supportsEndTick(),
                header.playerName(),
                header.playerUuid(),
                header.startEpochMillis(),
                header.startNanos(),
                label,
                scenario,
                note,
                tags,
                header.observeOnly(),
                header.checkSnapshot(),
                header.physicsConfig(),
                header.versionGates(),
                header.blockStateTable(),
                header.filterVersion());
    }
}
