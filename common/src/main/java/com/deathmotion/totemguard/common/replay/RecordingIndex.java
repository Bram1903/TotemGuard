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

import com.deathmotion.totemguard.common.replay.format.RecordingLabel;
import com.deathmotion.totemguard.common.replay.format.RecordingReader;
import com.deathmotion.totemguard.common.util.Scheduler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RecordingIndex {

    private static final long STALE_MILLIS = 5_000L;

    private final RecordingLibrary library;
    private final Scheduler scheduler;
    private final AtomicBoolean scanning = new AtomicBoolean();

    private volatile List<Entry> snapshot = List.of();
    private volatile long scannedAtMillis;
    private volatile CompletableFuture<List<Entry>> inFlight = CompletableFuture.completedFuture(List.of());

    public RecordingIndex(RecordingLibrary library, Scheduler scheduler) {
        this.library = library;
        this.scheduler = scheduler;
    }

    public static List<Entry> matching(List<Entry> entries, @Nullable String needle) {
        if (needle == null || needle.isBlank()) return entries;
        String lower = needle.toLowerCase(Locale.ROOT).replace('\\', '/');
        List<Entry> matched = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.path().toLowerCase(Locale.ROOT).contains(lower)
                    || entry.tags().contains(lower)) {
                matched.add(entry);
            }
        }
        return matched;
    }

    private static @Nullable RecordingLabel labelOf(String path) {
        int slash = path.indexOf('/');
        return slash <= 0 ? null : RecordingLabel.parse(path.substring(0, slash));
    }

    private static String scenarioOf(Path file) {
        String name = file.getFileName().toString();
        int dot = name.indexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    private static List<String> tagsOf(Path file) {
        try (RecordingReader reader = new RecordingReader(file)) {
            return reader.getHeader().tags();
        } catch (IOException | RuntimeException unreadable) {
            return List.of();
        }
    }

    public List<Entry> current() {
        if (stale()) rescan();
        return snapshot;
    }

    public CompletableFuture<List<Entry>> entries() {
        if (!stale()) return CompletableFuture.completedFuture(snapshot);
        CompletableFuture<List<Entry>> scan = rescan();
        return snapshot.isEmpty() ? scan : CompletableFuture.completedFuture(snapshot);
    }

    public CompletableFuture<List<Entry>> refreshed() {
        return stale() ? rescan() : CompletableFuture.completedFuture(snapshot);
    }

    public void invalidate() {
        scannedAtMillis = 0L;
    }

    private boolean stale() {
        return scannedAtMillis == 0L || System.currentTimeMillis() - scannedAtMillis >= STALE_MILLIS;
    }

    private CompletableFuture<List<Entry>> rescan() {
        if (!scanning.compareAndSet(false, true)) return inFlight;
        CompletableFuture<List<Entry>> scan = new CompletableFuture<>();
        inFlight = scan;
        scheduler.runAsyncTask(() -> {
            List<Entry> scanned;
            try {
                scanned = read();
            } catch (RuntimeException failure) {
                scanned = snapshot;
            }
            snapshot = scanned;
            scannedAtMillis = System.currentTimeMillis();
            scanning.set(false);
            scan.complete(scanned);
        });
        return scan;
    }

    private List<Entry> read() {
        Path root = library.root();
        List<Entry> entries = new ArrayList<>();
        for (Path file : library.all()) {
            String path = root.relativize(file).toString().replace('\\', '/');
            long bytes = 0L;
            long modified = 0L;
            try {
                bytes = Files.size(file);
                modified = Files.getLastModifiedTime(file).toMillis();
            } catch (IOException ignored) {
            }
            entries.add(new Entry(path, labelOf(path), scenarioOf(file), tagsOf(file), bytes, modified));
        }
        entries.sort(Comparator.comparingLong(Entry::modifiedMillis).reversed());
        return List.copyOf(entries);
    }

    public record Entry(String path, @Nullable RecordingLabel label, String scenario, List<String> tags,
                        long bytes, long modifiedMillis) {
    }
}
