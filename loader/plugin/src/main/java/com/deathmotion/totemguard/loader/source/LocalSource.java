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

package com.deathmotion.totemguard.loader.source;

import com.deathmotion.totemguard.loader.download.Checksums;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Picks a TotemGuard plugin jar from the version cache that the {@code LocalImporter}
 * populated. The cache is the canonical store for both remote-downloaded and locally-
 * imported builds, so this source just performs version matching against it.
 */
public final class LocalSource implements VersionResolver {

    private static boolean isChannel(String requested) {
        String upper = requested.toUpperCase(Locale.ROOT);
        return upper.equals("LATEST") || upper.equals("EXPERIMENTAL") || upper.equals("GIT");
    }

    private static Path pickPinned(List<Path> candidates, String requested, Pattern pattern) throws IOException {
        Path best = null;
        FileTime bestTime = null;
        for (Path candidate : candidates) {
            Matcher matcher = pattern.matcher(candidate.getFileName().toString());
            if (!matcher.matches()) continue;
            String version = matcher.group(1);
            if (!version.equals(requested)) continue;
            FileTime t = Files.getLastModifiedTime(candidate);
            if (bestTime == null || t.compareTo(bestTime) > 0) {
                best = candidate;
                bestTime = t;
            }
        }
        if (best == null) {
            throw new IOException("No cached jar matches version '" + requested + "'. "
                    + "Run /tgloader versions to see what's available.");
        }
        return best;
    }

    private static Path newestByModified(List<Path> candidates) throws IOException {
        Path best = null;
        FileTime bestTime = null;
        for (Path candidate : candidates) {
            FileTime t = Files.getLastModifiedTime(candidate);
            if (bestTime == null || t.compareTo(bestTime) > 0) {
                best = candidate;
                bestTime = t;
            }
        }
        return best;
    }

    @Override
    public String sourceName() {
        return "Local";
    }

    @Override
    public Artifact resolve(ResolverContext context) throws Exception {
        String prefix = "TotemGuard-" + context.platform().name().toLowerCase(Locale.ROOT) + "-";
        Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(.+)-([0-9a-f]+)\\.jar$");

        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(context.paths().versionsDir(), prefix + "*.jar")) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            throw new IOException("No TotemGuard jars in the version cache. Drop one in "
                    + context.paths().localDir() + " and restart the server.");
        }

        String requested = context.config().version();
        Path chosen = isChannel(requested)
                ? newestByModified(candidates)
                : pickPinned(candidates, requested, pattern);

        Matcher matcher = pattern.matcher(chosen.getFileName().toString());
        String resolvedVersion = matcher.matches() ? matcher.group(1) : requested;
        String hash = Checksums.hashFile(chosen, Artifact.HashAlgorithm.SHA_256);

        return new Artifact(resolvedVersion, chosen.toUri(), Artifact.HashAlgorithm.SHA_256,
                hash, chosen.getFileName().toString(), "Local " + context.paths().versionsDir());
    }
}
