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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class YamlPatcher {

    private YamlPatcher() {
    }

    public static String setScalar(String text, String key, String newValue) {
        String[] lines = splitLines(text);

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String trimmed = raw.stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (trimmed.startsWith(key + ":")) {
                String prefix = raw.substring(0, raw.indexOf(trimmed));
                lines[i] = prefix + key + ": " + newValue;
                return joinLines(lines);
            }
        }

        int insertAt = firstNonHeaderIndex(lines);
        List<String> out = new ArrayList<>(Arrays.asList(lines));
        out.add(insertAt, key + ": " + newValue);
        return joinLines(out.toArray(new String[0]));
    }

    public static String addMissingDefaults(String currentText, Map<String, Object> currentMap, Map<String, Object> defaultsMap) {
        String text = currentText;

        for (Map.Entry<String, Object> e : defaultsMap.entrySet()) {
            String topKey = e.getKey();
            Object defVal = e.getValue();

            if (!currentMap.containsKey(topKey)) {
                text = appendTopLevel(text, topKey, defVal);
                continue;
            }

            Object curVal = currentMap.get(topKey);
            if (defVal instanceof Map<?, ?> defM && curVal instanceof Map<?, ?> curM) {
                text = addMissingIntoExistingBlock(text, topKey,
                        YamlMaps.toLinkedMap(defM),
                        YamlMaps.toLinkedMap(curM));
            }
        }

        return text;
    }

    private static String addMissingIntoExistingBlock(String text, String topKey, Map<String, Object> defaults, Map<String, Object> current) {
        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            String childKey = e.getKey();
            if (current.containsKey(childKey)) continue;

            text = insertIntoBlock(text, topKey, childKey, e.getValue());
        }
        return text;
    }

    private static String appendTopLevel(String text, String key, Object value) {
        StringBuilder sb = new StringBuilder(text);
        if (!text.endsWith("\n")) sb.append('\n');
        sb.append('\n');
        sb.append(renderKeyValue(0, key, value));
        return sb.toString();
    }

    private static String insertIntoBlock(String text, String topKey, String childKey, Object childValue) {
        String[] lines = splitLines(text);

        int start = -1;
        int startIndent = 0;

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String trimmed = raw.stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (trimmed.startsWith(topKey + ":")) {
                start = i;
                startIndent = raw.indexOf(trimmed);
                break;
            }
        }
        if (start == -1) return text;

        int end = lines.length;
        for (int i = start + 1; i < lines.length; i++) {
            String raw = lines[i];
            String trimmed = raw.stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            int indent = raw.indexOf(trimmed);
            if (indent <= startIndent) {
                end = i;
                break;
            }
        }

        int childIndent = startIndent + 2;
        String rendered = renderKeyValue(childIndent, childKey, childValue);

        List<String> out = new ArrayList<>(Arrays.asList(lines));
        String[] addLines = rendered.split("\n", -1);

        for (int j = 0; j < addLines.length; j++) {
            if (addLines[j].isEmpty() && j == addLines.length - 1) continue;
            out.add(end + j, addLines[j]);
        }

        return joinLines(out.toArray(new String[0]));
    }

    private static String renderKeyValue(int indent, String key, Object value) {
        String pad = " ".repeat(Math.max(0, indent));

        if (value instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder();
            sb.append(pad).append(key).append(":").append("\n");
            Map<String, Object> lm = YamlMaps.toLinkedMap(m);
            for (Map.Entry<String, Object> e : lm.entrySet()) {
                sb.append(renderKeyValue(indent + 2, e.getKey(), e.getValue()));
            }
            return sb.toString();
        }

        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            sb.append(pad).append(key).append(":").append("\n");
            for (Object o : list) {
                sb.append(pad).append("  - ").append(o).append("\n");
            }
            return sb.toString();
        }

        return pad + key + ": " + value + "\n";
    }

    private static int firstNonHeaderIndex(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].stripLeading();
            if (t.isEmpty() || t.startsWith("#")) continue;
            return i;
        }
        return lines.length;
    }

    private static String[] splitLines(String text) {
        return text.split("\n", -1);
    }

    private static String joinLines(String[] lines) {
        return String.join("\n", lines);
    }
}
