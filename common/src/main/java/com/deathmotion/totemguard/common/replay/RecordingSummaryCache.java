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

import com.deathmotion.totemguard.common.util.Scheduler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RecordingSummaryCache {

    private final RecordingLibrary library;
    private final Scheduler scheduler;
    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final Set<String> reading = ConcurrentHashMap.newKeySet();

    public RecordingSummaryCache(RecordingLibrary library, Scheduler scheduler) {
        this.library = library;
        this.scheduler = scheduler;
    }

    private static String describe(Exception failure) {
        String reason = failure.getMessage();
        return reason == null ? failure.getClass().getSimpleName() : reason;
    }

    public @Nullable Entry peek(RecordingIndex.Entry entry) {
        Entry cached = entries.get(entry.path());
        return cached != null && cached.matches(entry.bytes(), entry.modifiedMillis()) ? cached : null;
    }

    public void warm(List<RecordingIndex.Entry> wanted, Runnable onEach) {
        for (RecordingIndex.Entry entry : wanted) {
            if (peek(entry) != null) continue;
            if (!reading.add(entry.path())) continue;
            scheduler.runAsyncTask(() -> {
                try {
                    entries.put(entry.path(), read(entry.path(), entry.bytes(), entry.modifiedMillis()));
                } finally {
                    reading.remove(entry.path());
                    onEach.run();
                }
            });
        }
    }

    public Entry require(String path) {
        Path file = library.root().resolve(path);
        long bytes;
        long modified;
        try {
            bytes = Files.size(file);
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException failure) {
            return new Entry(0L, 0L, null, describe(failure));
        }

        Entry cached = entries.get(path);
        if (cached != null && cached.matches(bytes, modified)) return cached;

        Entry loaded = read(path, bytes, modified);
        entries.put(path, loaded);
        return loaded;
    }

    private Entry read(String path, long bytes, long modified) {
        Path file = library.root().resolve(path);
        try {
            return new Entry(bytes, modified, RecordingSummary.read(library.root(), file), null);
        } catch (IOException | RuntimeException failure) {
            return new Entry(bytes, modified, null, describe(failure));
        }
    }

    public void invalidate(String path) {
        entries.remove(path);
    }

    public void invalidate() {
        entries.clear();
    }

    public record Entry(long bytes, long modifiedMillis, @Nullable RecordingSummary summary,
                        @Nullable String error) {

        private boolean matches(long otherBytes, long otherModified) {
            return bytes == otherBytes && modifiedMillis == otherModified;
        }
    }
}
