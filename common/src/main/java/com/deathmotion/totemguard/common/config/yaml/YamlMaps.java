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

package com.deathmotion.totemguard.common.config.yaml;

import java.util.*;

public final class YamlMaps {

    private YamlMaps() {
    }

    public static Map<String, Object> toLinkedMap(Map<?, ?> in) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : in.entrySet()) {
            out.put(String.valueOf(e.getKey()), normalize(e.getValue()));
        }
        return out;
    }

    private static Object normalize(Object v) {
        if (v instanceof Map<?, ?> m) return toLinkedMap(m);
        if (v instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object o : list) out.add(normalize(o));
            return out;
        }
        return v;
    }

    /**
     * Walks a dot-separated path through nested maps. Returns empty if any segment is
     * missing, the leaf value is null (e.g. {@code key:} with no value in YAML), or a
     * non-map is encountered before the final segment.
     * <p>
     * Use {@link #containsPath} to distinguish "missing" from "present but null".
     */
    public static Optional<Object> walk(Map<String, Object> root, String dottedPath) {
        if (dottedPath.isEmpty()) return Optional.empty();

        String[] tokens = dottedPath.split("\\.");
        Object cur = root;
        for (String key : tokens) {
            if (!(cur instanceof Map<?, ?> map)) return Optional.empty();
            cur = map.get(key);
            if (cur == null) return Optional.empty();
        }
        return Optional.of(cur);
    }

    /**
     * Returns true if the leaf key at the given dotted path exists in the map, regardless
     * of whether its value is null. Useful for telling YAML-null values ({@code key:} with
     * no scalar) apart from missing keys.
     */
    public static boolean containsPath(Map<String, Object> root, String dottedPath) {
        if (dottedPath.isEmpty()) return false;

        String[] tokens = dottedPath.split("\\.");
        Object cur = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            if (!(cur instanceof Map<?, ?> map)) return false;
            cur = map.get(tokens[i]);
            if (cur == null) return false;
        }
        return cur instanceof Map<?, ?> map && map.containsKey(tokens[tokens.length - 1]);
    }

    /**
     * Sets a dot-separated path in a nested map, creating intermediate maps as needed.
     * Returns the previous value at the path, or null if absent.
     */
    @SuppressWarnings("unchecked")
    public static Object setPath(Map<String, Object> root, String dottedPath, Object value) {
        String[] tokens = dottedPath.split("\\.");
        Map<String, Object> cur = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            Object next = cur.get(tokens[i]);
            if (next instanceof Map<?, ?> m) {
                cur = (Map<String, Object>) m;
            } else {
                Map<String, Object> fresh = new LinkedHashMap<>();
                cur.put(tokens[i], fresh);
                cur = fresh;
            }
        }
        return cur.put(tokens[tokens.length - 1], value);
    }

    /**
     * Removes a dot-separated path from a nested map. Returns the removed value, or null if absent.
     * Empty intermediate maps left behind are not pruned.
     */
    public static Object removePath(Map<String, Object> root, String dottedPath) {
        String[] tokens = dottedPath.split("\\.");
        Map<String, Object> cur = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            Object next = cur.get(tokens[i]);
            if (!(next instanceof Map<?, ?>)) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) next;
            cur = casted;
        }
        return cur.remove(tokens[tokens.length - 1]);
    }
}
