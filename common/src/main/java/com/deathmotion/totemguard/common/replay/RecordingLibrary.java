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
import com.deathmotion.totemguard.common.replay.format.ReplayFormat;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class RecordingLibrary {

    private static final int MAX_SCENARIO_LENGTH = 40;

    private final Path root;

    public RecordingLibrary(Path root) {
        this.root = root;
    }

    public static String display(String path) {
        String forward = path.replace('\\', '/');
        return forward.endsWith(ReplayFormat.EXTENSION)
                ? forward.substring(0, forward.length() - ReplayFormat.EXTENSION.length())
                : forward;
    }

    public static boolean validScenario(@Nullable String value) {
        if (value == null || value.isBlank() || value.length() > MAX_SCENARIO_LENGTH) return false;
        return sanitize(value).equals(value.toLowerCase(Locale.ROOT));
    }

    public static String sanitize(String value) {
        StringBuilder clean = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            clean.append(Character.isLetterOrDigit(c) || c == '-' || c == '_' ? c : '-');
        }
        String result = clean.toString().toLowerCase(Locale.ROOT);
        return result.isBlank() ? "unnamed" : result;
    }

    public static String scenarioFrom(String value) {
        String clean = sanitize(value);
        return clean.length() <= MAX_SCENARIO_LENGTH ? clean : clean.substring(0, MAX_SCENARIO_LENGTH);
    }

    public static List<String> parseTags(String raw) {
        return raw == null || raw.isBlank() ? List.of() : normalizeTags(List.of(raw.split("[,\\s]+")));
    }

    public static List<String> normalizeTags(Collection<String> raw) {
        Set<String> tags = new LinkedHashSet<>();
        for (String part : raw) {
            if (part == null || part.isBlank()) continue;
            String tag = sanitize(part);
            if (!tag.isBlank()) tags.add(tag);
        }
        return List.copyOf(tags);
    }

    public Path root() {
        return root;
    }

    public Path allocate(RecordingLabel label, String scenario, String clientTag) {
        return allocate(label, null, scenario, clientTag);
    }

    public Path allocate(RecordingLabel label, @Nullable String folder, String scenario, String clientTag) {
        Path directory = folder == null
                ? root.resolve(label.id())
                : root.resolve(label.id()).resolve(sanitize(folder));
        String base = sanitize(scenario) + "." + sanitize(clientTag);
        Path candidate = directory.resolve(base + ReplayFormat.EXTENSION);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(base + "." + suffix + ReplayFormat.EXTENSION);
            suffix++;
        }
        return candidate;
    }

    public Path folder(RecordingLabel label, String folder) {
        return root.resolve(label.id()).resolve(sanitize(folder));
    }

    public List<Path> all() {
        List<Path> found = new ArrayList<>();
        if (!Files.isDirectory(root)) return found;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(ReplayFormat.EXTENSION))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(found::add);
        } catch (IOException ignored) {
        }
        return found;
    }

    public @Nullable Path resolve(String query) {
        Path direct = Path.of(query);
        if (Files.isRegularFile(direct)) return direct;

        Path underRoot = root.resolve(query);
        if (Files.isRegularFile(underRoot)) return underRoot;

        Path withExtension = root.resolve(query + ReplayFormat.EXTENSION);
        if (Files.isRegularFile(withExtension)) return withExtension;

        String normalized = query.endsWith(ReplayFormat.EXTENSION) ? query : query + ReplayFormat.EXTENSION;
        for (Path candidate : all()) {
            if (candidate.getFileName().toString().equalsIgnoreCase(normalized)) return candidate;
            if (root.relativize(candidate).toString().replace('\\', '/').equalsIgnoreCase(normalized)) return candidate;
        }
        return null;
    }
}
