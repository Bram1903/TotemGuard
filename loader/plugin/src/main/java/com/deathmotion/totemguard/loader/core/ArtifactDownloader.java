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

import com.deathmotion.totemguard.loader.download.LoaderHttp;
import com.deathmotion.totemguard.loader.source.Artifact;
import com.deathmotion.totemguard.loader.source.HttpStatusText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.logging.Logger;

final class ArtifactDownloader {

    private final Logger logger;

    ArtifactDownloader(Logger logger) {
        this.logger = logger;
    }

    private static MessageDigest newDigest(Artifact artifact) throws IOException {
        try {
            return MessageDigest.getInstance(artifact.hashAlgorithm().jdkName());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    byte[] download(Artifact artifact) throws IOException {
        URI uri = artifact.downloadUri();
        if ("file".equals(uri.getScheme())) {
            byte[] bytes = Files.readAllBytes(Path.of(uri));
            verify(bytes, artifact);
            return bytes;
        }
        return LoaderHttp.retry(logger, "download " + artifact.fileName(), () -> downloadHttp(artifact));
    }

    private byte[] downloadHttp(Artifact artifact) throws IOException, InterruptedException {
        logger.info("Downloading " + artifact.fileName() + " from " + artifact.sourceLabel());

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(artifact.downloadUri())
                .timeout(LoaderHttp.READ_TIMEOUT)
                .header("User-Agent", "TotemGuard-Loader")
                .GET();
        for (Map.Entry<String, String> header : artifact.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        HttpResponse<InputStream> response = LoaderHttp.client().send(builder.build(),
                HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            int status = response.statusCode();
            String message = "HTTP " + HttpStatusText.describe(status) + " for " + artifact.downloadUri();
            // 5xx is transient: throwing IOException lets LoaderHttp.retry kick in.
            if (status >= 500 || status == 408 || status == 429) {
                throw new IOException(message);
            }
            throw new PermanentHttpException(message);
        }

        MessageDigest digest = newDigest(artifact);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(1 << 20);
        try (InputStream in = response.body();
             DigestInputStream digestIn = new DigestInputStream(in, digest)) {
            digestIn.transferTo(buffer);
        }
        byte[] bytes = buffer.toByteArray();
        String actual = HexFormat.of().formatHex(digest.digest());
        if (!actual.equalsIgnoreCase(artifact.hashHex())) {
            throw new IOException("Downloaded " + artifact.fileName()
                    + " hash " + actual + " did not match advertised " + artifact.hashHex());
        }
        return bytes;
    }

    private void verify(byte[] bytes, Artifact artifact) throws IOException {
        MessageDigest digest = newDigest(artifact);
        digest.update(bytes);
        String actual = HexFormat.of().formatHex(digest.digest());
        if (!actual.equalsIgnoreCase(artifact.hashHex())) {
            throw new IOException("Downloaded " + artifact.fileName()
                    + " hash " + actual + " did not match advertised " + artifact.hashHex());
        }
    }

    /**
     * Sentinel IOException that {@link LoaderHttp#retry} treats as non-retryable.
     */
    static final class PermanentHttpException extends IOException implements LoaderHttp.Permanent {
        PermanentHttpException(String message) {
            super(message);
        }
    }
}
