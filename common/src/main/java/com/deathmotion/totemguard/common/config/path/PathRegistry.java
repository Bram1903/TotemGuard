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

package com.deathmotion.totemguard.common.config.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PathRegistry {

    private final ConcurrentHashMap<String, Integer> ids = new ConcurrentHashMap<>();
    private final List<String[]> tokensById = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public PathRegistry() {
        tokensById.add(new String[0]);
    }

    private static String[] tokenize(String path) {
        String p = path.trim();
        if (p.isEmpty()) return new String[0];
        return p.split("\\.");
    }

    public void clear() {
        ids.clear();
        tokensById.clear();
        tokensById.add(new String[0]);
        nextId.set(1);
    }

    public PathId register(String path) {
        int id = ids.computeIfAbsent(path, p -> {
            int newId = nextId.getAndIncrement();
            ensureCapacity(newId);
            tokensById.set(newId, tokenize(p));
            return newId;
        });
        return new PathId(id);
    }

    public PathId resolve(String path) {
        Integer id = ids.get(path);
        if (id == null) return PathId.invalid();
        return new PathId(id);
    }

    public String[] tokens(PathId id) {
        int idx = id.id();
        if (idx <= 0 || idx >= tokensById.size()) return new String[0];
        String[] t = tokensById.get(idx);
        return t == null ? new String[0] : t;
    }

    public void registerAllPaths(Map<String, Object> root) {
        walk("", root);
    }

    private void walk(String prefix, Object node) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                if (key.isBlank()) continue;

                register(key);

                String path = prefix.isEmpty() ? key : prefix + "." + key;
                register(path);

                walk(path, e.getValue());
            }
        }
    }

    private synchronized void ensureCapacity(int id) {
        while (tokensById.size() <= id) tokensById.add(null);
    }
}
