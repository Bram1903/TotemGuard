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

package com.deathmotion.totemguard.loader.catalog;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CatalogRetention {

    private static final int PER_BUCKET = 2;

    private final CatalogIndex index;
    private final Logger logger;

    public CatalogRetention(CatalogIndex index, Logger logger) {
        this.index = index;
        this.logger = logger;
    }

    public int sweep(Set<String> protectedShas) {
        List<CatalogIndex.Entry> entries = index.readAll();
        if (entries.isEmpty()) return 0;

        Map<String, List<CatalogIndex.Entry>> byBucket = new LinkedHashMap<>();
        for (CatalogIndex.Entry entry : entries) {
            for (String source : entry.sidecar().sources()) {
                byBucket.computeIfAbsent(source, k -> new ArrayList<>()).add(entry);
            }
        }

        Set<String> keepShas = new HashSet<>();
        if (protectedShas != null) {
            for (String sha : protectedShas) {
                if (sha != null && !sha.isBlank()) keepShas.add(sha.toLowerCase(Locale.ROOT));
            }
        }

        for (Map.Entry<String, List<CatalogIndex.Entry>> bucket : byBucket.entrySet()) {
            List<CatalogIndex.Entry> sorted = new ArrayList<>(bucket.getValue());
            sorted.sort(Comparator.comparing((CatalogIndex.Entry e) -> e.sidecar().firstSeen()).reversed());
            int kept = 0;
            for (CatalogIndex.Entry entry : sorted) {
                if (kept >= PER_BUCKET) break;
                String sha = entry.sidecar().sha256();
                if (sha != null && !sha.isBlank()) {
                    keepShas.add(sha.toLowerCase(Locale.ROOT));
                }
                kept++;
            }
        }

        int deleted = 0;
        for (CatalogIndex.Entry entry : entries) {
            String sha = entry.sidecar().sha256();
            String normSha = sha == null ? "" : sha.toLowerCase(Locale.ROOT);
            if (keepShas.contains(normSha)) continue;
            try {
                index.delete(entry);
                deleted++;
                logger.info("Retention dropped " + entry.jar().getFileName()
                        + " (sources: " + entry.sidecar().sources() + ").");
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to delete " + entry.jar().getFileName()
                        + " during retention", ex);
            }
        }
        return deleted;
    }
}
