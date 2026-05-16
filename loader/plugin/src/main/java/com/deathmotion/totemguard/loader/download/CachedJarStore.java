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

package com.deathmotion.totemguard.loader.download;

import com.deathmotion.totemguard.loader.config.LoaderConfig;
import com.deathmotion.totemguard.loader.core.HostPlatform;
import com.deathmotion.totemguard.loader.source.Artifact;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CachedJarStore {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final String CHANNELS_DIR_NAME = ".channels";

    private final Path versionsDir;
    private final Logger logger;
    private final HttpClient client;

    public CachedJarStore(Path versionsDir, Logger logger) throws IOException {
        this.versionsDir = versionsDir;
        Files.createDirectories(versionsDir);
        this.logger = logger;
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private static void verifyAndMove(Path partial, Path destination, Artifact artifact) throws IOException {
        String actual = Checksums.hashFile(partial, artifact.hashAlgorithm());
        if (!actual.equalsIgnoreCase(artifact.hashHex())) {
            throw new IOException("Local jar " + artifact.fileName()
                    + " hash " + actual + " did not match expected " + artifact.hashHex());
        }
        Files.move(partial, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static MessageDigest newDigest(Artifact artifact) throws IOException {
        try {
            return MessageDigest.getInstance(artifact.hashAlgorithm().jdkName());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._+-]", "_");
    }

    public Path getOrFetch(Artifact artifact, HostPlatform platform) throws IOException {
        Path lockFile = versionsDir.resolve(".lock");
        try (FileChannel lockChannel = FileChannel.open(lockFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = lockChannel.lock()) {
            return getOrFetchLocked(artifact, platform);
        }
    }

    private Path getOrFetchLocked(Artifact artifact, HostPlatform platform) throws IOException {
        String hashPrefix = artifact.hashHex().substring(0, Math.min(10, artifact.hashHex().length()));
        Path cached = versionsDir.resolve("TotemGuard-" + platform.name().toLowerCase()
                + "-" + sanitize(artifact.version())
                + "-" + hashPrefix + ".jar");

        if (Files.isRegularFile(cached)) {
            String actual = Checksums.hashFile(cached, artifact.hashAlgorithm());
            if (actual.equalsIgnoreCase(artifact.hashHex())) {
                logger.fine("Reusing cached jar " + cached.getFileName());
                return cached;
            }
            logger.warning("Cached jar " + cached.getFileName() + " hash mismatch. Redownloading.");
            Files.deleteIfExists(cached);
        }

        URI uri = artifact.downloadUri();
        return "file".equals(uri.getScheme())
                ? copyLocal(uri, cached, artifact)
                : downloadHttp(uri, cached, artifact);
    }

    private Path copyLocal(URI uri, Path destination, Artifact artifact) throws IOException {
        Path source = Path.of(uri);
        Path partial = destination.resolveSibling(destination.getFileName() + ".partial");
        try {
            Files.copy(source, partial, StandardCopyOption.REPLACE_EXISTING);
            verifyAndMove(partial, destination, artifact);
            return destination;
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    private Path downloadHttp(URI uri, Path destination, Artifact artifact) throws IOException {
        logger.info("Downloading " + artifact.fileName() + " from " + artifact.sourceLabel());
        Path partial = destination.resolveSibling(destination.getFileName() + ".partial");
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(READ_TIMEOUT)
                    .header("User-Agent", "TotemGuard-Loader")
                    .GET();
            for (Map.Entry<String, String> header : artifact.headers().entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }

            HttpResponse<InputStream> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }

            MessageDigest digest = newDigest(artifact);
            try (InputStream in = response.body();
                 OutputStream out = Files.newOutputStream(partial,
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[16_384];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                }
            }

            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equalsIgnoreCase(artifact.hashHex())) {
                throw new IOException("Downloaded " + artifact.fileName()
                        + " hash " + actual + " did not match advertised " + artifact.hashHex());
            }

            Files.move(partial, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", ex);
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    /**
     * Records that {@code jar} is the most recently fetched build for {@code channel}
     * (one of {@code LATEST}, {@code EXPERIMENTAL}, {@code GIT}). The pointer is used
     * by {@link #locateFallback} when a later resolve or download fails.
     */
    public void recordChannel(String channel, Path jar) throws IOException {
        Path channelsDir = versionsDir.resolve(CHANNELS_DIR_NAME);
        Files.createDirectories(channelsDir);
        Path pointer = channelsDir.resolve(channel.toUpperCase(Locale.ROOT) + ".txt");
        Path tmp = pointer.resolveSibling(pointer.getFileName() + ".tmp");
        Files.writeString(tmp, jar.getFileName().toString(), StandardCharsets.UTF_8);
        Files.move(tmp, pointer, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Strict fallback lookup: returns a previously cached jar for the exact target
     * the user asked for. For channel requests this is the jar pinned by the channel
     * pointer. For pinned versions this is the newest cached jar carrying that
     * version string in its filename. Channel requests never fall through to a
     * different channel, pinned requests never fall through to a different version.
     */
    public Optional<Path> locateFallback(LoaderConfig config, HostPlatform platform) {
        String channel = config.channel();
        if (channel != null) {
            return findChannelJar(channel);
        }
        return findPinnedJar(config.version(), platform);
    }

    private Optional<Path> findChannelJar(String channel) {
        Path pointer = versionsDir.resolve(CHANNELS_DIR_NAME).resolve(channel.toUpperCase(Locale.ROOT) + ".txt");
        if (!Files.isRegularFile(pointer)) return Optional.empty();
        try {
            String name = Files.readString(pointer, StandardCharsets.UTF_8).trim();
            if (name.isEmpty()) return Optional.empty();
            Path candidate = versionsDir.resolve(name);
            return Files.isRegularFile(candidate) ? Optional.of(candidate) : Optional.empty();
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public List<String> listCachedVersions(HostPlatform platform) {
        String prefix = "TotemGuard-" + platform.name().toLowerCase(Locale.ROOT) + "-";
        Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix)
                + "(.+)-[0-9a-f]{1,16}\\.jar$");
        TreeSet<String> versions = new TreeSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsDir, prefix + "*.jar")) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                Matcher m = pattern.matcher(p.getFileName().toString());
                if (m.matches()) {
                    versions.add(m.group(1));
                }
            }
        } catch (IOException ignored) {
        }
        return new ArrayList<>(versions);
    }

    /**
     * Returns the newest jar in the catalog matching the exact pinned version string.
     * Used both as a fallback for unreachable sources and as a fast path for pinned
     * loads so the loader can skip the network when the build is already on disk.
     */
    public Optional<Path> findPinnedJar(String version, HostPlatform platform) {
        String prefix = "TotemGuard-" + platform.name().toLowerCase(Locale.ROOT)
                + "-" + sanitize(version) + "-";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsDir, prefix + "*.jar")) {
            Path newest = null;
            FileTime newestTime = null;
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                FileTime t = Files.getLastModifiedTime(p);
                if (newestTime == null || t.compareTo(newestTime) > 0) {
                    newest = p;
                    newestTime = t;
                }
            }
            return Optional.ofNullable(newest);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    /**
     * Last-resort lookup: returns the newest jar in the version catalog for this
     * platform, ignoring channel pointers and the requested version. Used by the
     * loader's resilience path when the configured source is unreachable and no
     * better-matching fallback exists.
     */
    public Optional<Path> findNewestCachedJar(HostPlatform platform) {
        String prefix = "TotemGuard-" + platform.name().toLowerCase(Locale.ROOT) + "-";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsDir, prefix + "*.jar")) {
            Path newest = null;
            FileTime newestTime = null;
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                FileTime t = Files.getLastModifiedTime(p);
                if (newestTime == null || t.compareTo(newestTime) > 0) {
                    newest = p;
                    newestTime = t;
                }
            }
            return Optional.ofNullable(newest);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }
}
