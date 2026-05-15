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

import com.deathmotion.totemguard.loader.config.LoaderConfig;
import com.deathmotion.totemguard.loader.core.HostPlatform;
import com.deathmotion.totemguard.loader.core.LoaderPaths;
import com.deathmotion.totemguard.loader.download.Checksums;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Picks an inner jar from a local directory. Intended for development; not documented
 * in the bundled loader-config.yml.
 */
public final class LocalSource implements VersionResolver {

    private static final Pattern VERSION_FROM_FILENAME =
            Pattern.compile("(\\d+\\.\\d+\\.\\d+(?:\\+[0-9a-f]+)?(?:-SNAPSHOT)?)");

    private static String parseVersion(String fileName) {
        Matcher m = VERSION_FROM_FILENAME.matcher(fileName);
        return m.find() ? m.group(1) : fileName;
    }

    @Override
    public String sourceName() {
        return "Local";
    }

    @Override
    public Artifact resolve(LoaderConfig config, HostPlatform platform, LoaderPaths paths) throws Exception {
        Path dir = paths.localDir();
        Files.createDirectories(dir);
        String pattern = "TotemGuard-" + platform.assetSuffix() + "-*.jar";
        PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:" + pattern);

        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                if (matcher.matches(p.getFileName())) candidates.add(p);
            }
        }
        if (candidates.isEmpty()) {
            throw new IOException("No jars matching '" + pattern + "' in " + dir);
        }

        String requested = config.version();
        Path chosen;
        if (requested.equalsIgnoreCase("LATEST")
                || requested.equalsIgnoreCase("EXPERIMENTAL")
                || requested.equalsIgnoreCase("GIT")) {
            chosen = candidates.stream()
                    .max(Comparator.comparing(p -> parseVersion(p.getFileName().toString())))
                    .orElseThrow();
        } else {
            chosen = candidates.stream()
                    .filter(p -> p.getFileName().toString().contains(requested))
                    .findFirst()
                    .orElseThrow(() -> new IOException(
                            "No local jar contains version string '" + requested + "' in " + dir));
        }

        String version = parseVersion(chosen.getFileName().toString());
        String hash = Checksums.hashFile(chosen, Artifact.HashAlgorithm.SHA_256);

        return new Artifact(version, chosen.toUri(), Artifact.HashAlgorithm.SHA_256,
                hash, chosen.getFileName().toString(), "Local " + dir);
    }
}
