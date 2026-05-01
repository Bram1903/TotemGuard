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

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Text-level YAML editing utilities that preserve user comments and structure.
 * <p>
 * Two operations are exposed:
 * <ul>
 *     <li>{@link #addMissingDefaults} — given user text and a defaults map, surgically inserts
 *     any missing top-level keys at file-end and any missing nested keys at the end of their
 *     parent block. User comments and ordering are preserved.</li>
 *     <li>{@link #dumpFresh} — emits a complete YAML document for a parsed map using SnakeYAML.
 *     Used when migrations have rewritten the file and comment preservation is no longer
 *     possible.</li>
 * </ul>
 */
public final class YamlMerger {

    private YamlMerger() {
    }

    /**
     * Returns {@code currentText} with any keys present in {@code defaultsMap} but missing from
     * {@code currentMap} inserted at the appropriate location:
     * <ul>
     *     <li>Missing top-level keys are appended to the end of the file.</li>
     *     <li>Missing nested keys (where the parent map exists in both) are inserted at the
     *     end of the parent's block, with the correct indentation.</li>
     * </ul>
     * Existing keys are left untouched, including their values, comments, and ordering.
     */
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
                text = mergeIntoNestedBlock(text, List.of(topKey),
                        YamlMaps.toLinkedMap(defM),
                        YamlMaps.toLinkedMap(curM));
            }
        }

        return text;
    }

    /**
     * Returns {@code currentText} with the value at the given nested key path replaced.
     * <p>
     * Scalars: the leaf line's indent, key, and inline comment (if any) are preserved.
     * Lists: the leaf line plus all subsequent lines belonging to its block (indent strictly
     * greater than the leaf) are rewritten as a fresh block-style list at the leaf indent.
     * Comments interleaved INSIDE the existing list block are not preserved; comments and
     * blank lines AFTER the last block line (before the next sibling) are preserved.
     * <p>
     * If any segment in the path is missing, or the leaf line isn't a {@code key:} entry,
     * the input text is returned unchanged. Block scalars and multi-line scalar values
     * aren't supported.
     */
    public static String setValueAtPath(String currentText, List<String> path, Object value) {
        if (path.isEmpty()) return currentText;
        String[] lines = splitLines(currentText);

        int parentIndent = -1;
        int searchStart = 0;
        int searchEnd = lines.length;
        int leafLine = -1;
        int leafIndent = 0;

        for (int i = 0; i < path.size(); i++) {
            int targetIndent = parentIndent == -1 ? 0 : parentIndent + 2;
            int found = findKeyLine(lines, searchStart, searchEnd, path.get(i), targetIndent);
            if (found == -1) return currentText;

            if (i == path.size() - 1) {
                leafLine = found;
                leafIndent = targetIndent;
            } else {
                parentIndent = targetIndent;
                searchStart = found + 1;
                searchEnd = endOfBlock(lines, found, targetIndent);
            }
        }

        String original = lines[leafLine];
        String trimmed = original.stripLeading();
        int colon = trimmed.indexOf(':');
        if (colon < 0) return currentText;

        String indentPart = original.substring(0, original.length() - trimmed.length());
        String keyPart = trimmed.substring(0, colon);

        if (value instanceof List<?> list) {
            int contentEnd = leafLine;
            for (int i = leafLine + 1; i < lines.length; i++) {
                String raw = lines[i];
                String t = raw.stripLeading();
                if (t.isEmpty() || t.startsWith("#")) continue;
                int indent = raw.length() - t.length();
                if (indent <= leafIndent) break;
                contentEnd = i;
            }

            List<String> rendered = new ArrayList<>();
            if (list.isEmpty()) {
                rendered.add(indentPart + keyPart + ": []");
            } else {
                rendered.add(indentPart + keyPart + ":");
                String itemIndent = " ".repeat(leafIndent + 2);
                for (Object o : list) {
                    rendered.add(itemIndent + "- " + renderScalar(o));
                }
            }

            List<String> out = new ArrayList<>(lines.length);
            for (int i = 0; i < leafLine; i++) out.add(lines[i]);
            out.addAll(rendered);
            for (int i = contentEnd + 1; i < lines.length; i++) out.add(lines[i]);
            return joinLines(out.toArray(new String[0]));
        }

        String afterColon = trimmed.substring(colon + 1);
        String inlineComment = extractInlineComment(afterColon);

        String newLine = indentPart + keyPart + ": " + renderScalar(value);
        if (!inlineComment.isEmpty()) {
            newLine += "  " + inlineComment;
        }

        lines[leafLine] = newLine;
        return joinLines(lines);
    }

    private static String extractInlineComment(String afterColon) {
        boolean inSingle = false;
        boolean inDouble = false;
        char prev = ' ';
        for (int i = 0; i < afterColon.length(); i++) {
            char c = afterColon.charAt(i);
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '#' && !inSingle && !inDouble && Character.isWhitespace(prev)) {
                return afterColon.substring(i);
            }
            prev = c;
        }
        return "";
    }

    /**
     * Renders a complete YAML document for a parsed map. Comments are NOT preserved.
     * Used after a migration changes the file shape, when the safe option is to rebuild.
     */
    public static String dumpFresh(Map<String, Object> root) {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        opts.setIndent(2);
        opts.setIndicatorIndent(0);
        opts.setSplitLines(false);
        return new Yaml(opts).dump(root);
    }

    private static String mergeIntoNestedBlock(String text, List<String> parentPath, Map<String, Object> defaults, Map<String, Object> current) {
        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            String childKey = e.getKey();
            Object defVal = e.getValue();

            if (!current.containsKey(childKey)) {
                text = insertAtBlockEnd(text, parentPath, childKey, defVal);
                continue;
            }

            Object curVal = current.get(childKey);
            if (defVal instanceof Map<?, ?> defM && curVal instanceof Map<?, ?> curM) {
                List<String> nestedPath = new ArrayList<>(parentPath);
                nestedPath.add(childKey);
                text = mergeIntoNestedBlock(text,
                        nestedPath,
                        YamlMaps.toLinkedMap(defM),
                        YamlMaps.toLinkedMap(curM));
            }
        }
        return text;
    }

    private static String appendTopLevel(String text, String key, Object value) {
        StringBuilder sb = new StringBuilder(text);
        if (!text.isEmpty() && !text.endsWith("\n")) sb.append('\n');
        if (!text.isEmpty()) sb.append('\n');
        sb.append(renderKeyValue(0, key, value));
        return sb.toString();
    }

    private static String insertAtBlockEnd(String text, List<String> parentPath, String childKey, Object childValue) {
        String[] lines = splitLines(text);

        int parentIndent = -1;
        int searchStart = 0;
        int searchEnd = lines.length;

        for (String segment : parentPath) {
            int targetIndent = parentIndent + 2;
            if (parentIndent == -1) targetIndent = 0;
            int found = findKeyLine(lines, searchStart, searchEnd, segment, targetIndent);
            if (found == -1) return text;
            parentIndent = targetIndent;
            searchStart = found + 1;
            searchEnd = endOfBlock(lines, found, targetIndent);
        }

        int childIndent = parentIndent + 2;
        String rendered = renderKeyValue(childIndent, childKey, childValue);

        List<String> out = new ArrayList<>(Arrays.asList(lines));
        String[] addLines = rendered.split("\n", -1);

        int insertAt = searchEnd;
        for (int j = 0; j < addLines.length; j++) {
            if (addLines[j].isEmpty() && j == addLines.length - 1) continue;
            out.add(insertAt + j, addLines[j]);
        }
        return joinLines(out.toArray(new String[0]));
    }

    private static int findKeyLine(String[] lines, int from, int to, String key, int requiredIndent) {
        for (int i = from; i < to; i++) {
            String raw = lines[i];
            String trimmed = raw.stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            int indent = raw.length() - trimmed.length();
            if (indent != requiredIndent) continue;

            int colon = trimmed.indexOf(':');
            if (colon < 0) continue;

            String foundKey = trimmed.substring(0, colon).trim();
            if (foundKey.equals(key)) return i;
        }
        return -1;
    }

    private static int endOfBlock(String[] lines, int blockStart, int blockIndent) {
        for (int i = blockStart + 1; i < lines.length; i++) {
            String raw = lines[i];
            String trimmed = raw.stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int indent = raw.length() - trimmed.length();
            if (indent <= blockIndent) return i;
        }
        return lines.length;
    }

    private static String renderKeyValue(int indent, String key, Object value) {
        String pad = " ".repeat(Math.max(0, indent));

        if (value instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder();
            sb.append(pad).append(key).append(":").append("\n");
            Map<String, Object> lm = YamlMaps.toLinkedMap(m);
            if (lm.isEmpty()) {
                return pad + key + ": {}\n";
            }
            for (Map.Entry<String, Object> e : lm.entrySet()) {
                sb.append(renderKeyValue(indent + 2, e.getKey(), e.getValue()));
            }
            return sb.toString();
        }

        if (value instanceof List<?> list) {
            if (list.isEmpty()) return pad + key + ": []\n";
            StringBuilder sb = new StringBuilder();
            sb.append(pad).append(key).append(":").append("\n");
            for (Object o : list) {
                sb.append(pad).append("  - ").append(renderScalar(o)).append("\n");
            }
            return sb.toString();
        }

        return pad + key + ": " + renderScalar(value) + "\n";
    }

    private static String renderScalar(Object v) {
        if (v == null) return "";
        if (v instanceof Boolean || v instanceof Number) return String.valueOf(v);
        String s = String.valueOf(v);
        if (needsQuoting(s)) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return s;
    }

    private static boolean needsQuoting(String s) {
        if (s.isEmpty()) return true;
        if (s.contains("\n")) return true;
        if (s.contains("#")) return true;
        if (s.contains(":")) return true;
        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if (Character.isWhitespace(first) || Character.isWhitespace(last)) return true;
        if ("[]{}|>&*!%@`,?".indexOf(first) >= 0) return true;
        return s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("no")
                || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")
                || s.equalsIgnoreCase("null") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("off");
    }

    private static String[] splitLines(String text) {
        return text.split("\n", -1);
    }

    private static String joinLines(String[] lines) {
        return String.join("\n", lines);
    }
}
