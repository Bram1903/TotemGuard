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

package com.deathmotion.totemguard.loader.core;

import com.deathmotion.totemguard.loader.source.Artifact;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Downloads an {@link Artifact}'s bytes into memory and verifies the source-native
 * checksum. Used by the fleet update path where the bytes need to be re-broadcast
 * rather than cached on disk.
 */
final class ArtifactDownloader {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final Logger logger;
    private final HttpClient client;

    ArtifactDownloader(Logger logger) {
        this.logger = logger;
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    byte[] download(Artifact artifact) throws IOException {
        URI uri = artifact.downloadUri();
        if ("file".equals(uri.getScheme())) {
            byte[] bytes = Files.readAllBytes(Path.of(uri));
            verify(bytes, artifact);
            return bytes;
        }
        return downloadHttp(artifact);
    }

    private byte[] downloadHttp(Artifact artifact) throws IOException {
        logger.info("Downloading " + artifact.fileName() + " from " + artifact.sourceLabel());
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(artifact.downloadUri())
                    .timeout(READ_TIMEOUT)
                    .header("User-Agent", "TotemGuard-Loader")
                    .GET();
            for (Map.Entry<String, String> header : artifact.headers().entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }

            HttpResponse<InputStream> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode() + " for " + artifact.downloadUri());
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream(1 << 20);
            try (InputStream in = response.body()) {
                in.transferTo(buffer);
            }
            byte[] bytes = buffer.toByteArray();
            verify(bytes, artifact);
            return bytes;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", ex);
        }
    }

    private void verify(byte[] bytes, Artifact artifact) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(artifact.hashAlgorithm().jdkName());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
        digest.update(bytes);
        String actual = HexFormat.of().formatHex(digest.digest());
        if (!actual.equalsIgnoreCase(artifact.hashHex())) {
            throw new IOException("Downloaded " + artifact.fileName()
                    + " hash " + actual + " did not match advertised " + artifact.hashHex());
        }
    }
}
