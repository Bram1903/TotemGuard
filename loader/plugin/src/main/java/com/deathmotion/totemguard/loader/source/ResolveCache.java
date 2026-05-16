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

import com.deathmotion.totemguard.api.fleet.FleetCache;
import com.deathmotion.totemguard.loader.fleet.FleetCacheRef;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process-shared cache of {@link Artifact} resolutions. Three layers, in order:
 * <ol>
 *     <li><b>L1:</b> {@code java.io.tmpdir/totemguard-loader-cache} on the local host</li>
 *     <li><b>L2:</b> Redis (when a {@link FleetCacheRef} is attached), shared fleet-wide</li>
 *     <li><b>L3:</b> the source itself (GitHub, Modrinth)</li>
 * </ol>
 *
 * <p>Channels (LATEST/EXPERIMENTAL/GIT) get a short TTL since they move; pinned versions
 * get a long TTL since the URL+sha never change. Negative results (resolve threw) get a
 * very short TTL so a transient failure doesn't spam the upstream API for 10 minutes.</p>
 */
public final class ResolveCache {

    private static final Duration CHANNEL_TTL = Duration.ofMinutes(10);
    private static final Duration PINNED_TTL = Duration.ofHours(24);
    private static final Duration NEGATIVE_TTL = Duration.ofSeconds(60);
    private static final Gson GSON = new Gson();
    private static final String L2_PREFIX = "totemguard:loader:cache:resolve:";
    private static final String L2_NEG_PREFIX = "totemguard:loader:cache:resolve:neg:";

    private final Path cacheDir;
    private final Logger logger;
    private final @Nullable FleetCacheRef fleetCacheRef;

    public ResolveCache(Logger logger, @Nullable FleetCacheRef fleetCacheRef) {
        this.cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "totemguard-loader-cache");
        this.logger = logger;
        this.fleetCacheRef = fleetCacheRef;
    }

    private static Duration ttlFor(String token) {
        if (token == null) return CHANNEL_TTL;
        String upper = token.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "LATEST", "EXPERIMENTAL", "GIT" -> CHANNEL_TTL;
            default -> PINNED_TTL;
        };
    }

    private static String keyHash(String source, String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update((source + ":" + token).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static JsonObject artifactToJson(Artifact artifact) {
        JsonObject root = new JsonObject();
        root.addProperty("version", artifact.version());
        root.addProperty("downloadUri", artifact.downloadUri().toString());
        root.addProperty("hashAlgorithm", artifact.hashAlgorithm().name());
        root.addProperty("hashHex", artifact.hashHex());
        root.addProperty("fileName", artifact.fileName());
        root.addProperty("sourceLabel", artifact.sourceLabel());
        JsonObject headers = new JsonObject();
        for (Map.Entry<String, String> entry : artifact.headers().entrySet()) {
            headers.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("headers", headers);
        return root;
    }

    private static Artifact artifactFromJson(JsonObject root) {
        Map<String, String> headers = new HashMap<>();
        if (root.has("headers")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("headers").entrySet()) {
                headers.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return new Artifact(
                root.get("version").getAsString(),
                URI.create(root.get("downloadUri").getAsString()),
                Artifact.HashAlgorithm.valueOf(root.get("hashAlgorithm").getAsString()),
                root.get("hashHex").getAsString(),
                root.get("fileName").getAsString(),
                root.get("sourceLabel").getAsString(),
                headers
        );
    }

    public Artifact get(String source, String token) {
        Artifact local = readLocal(source, token);
        if (local != null) return local;

        Optional<FleetCache> fleet = fleetCacheRef == null ? Optional.empty() : fleetCacheRef.available();
        if (fleet.isPresent()) {
            try {
                Optional<byte[]> bytes = fleet.get().get(L2_PREFIX + keyHash(source, token));
                if (bytes.isPresent()) {
                    JsonObject root = JsonParser.parseString(new String(bytes.get(), StandardCharsets.UTF_8)).getAsJsonObject();
                    writeLocal(source, token, root);
                    if (root.has("expiresAt") && System.currentTimeMillis() > root.get("expiresAt").getAsLong()) {
                        return null;
                    }
                    return artifactFromJson(root);
                }
            } catch (Throwable t) {
                logger.log(Level.FINE, "L2 resolve get failed for " + source + ":" + token, t);
            }
        }
        return null;
    }

    private Artifact readLocal(String source, String token) {
        Path file = entryPath(source, token);
        if (!Files.isRegularFile(file)) return null;
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ);
             FileLock ignored = channel.lock(0L, Long.MAX_VALUE, true)) {
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            long expiresAt = root.get("expiresAt").getAsLong();
            if (System.currentTimeMillis() > expiresAt) return null;
            return artifactFromJson(root);
        } catch (Exception ex) {
            logger.log(Level.FINE, "ResolveCache miss (read failed) for " + source + ":" + token, ex);
            return null;
        }
    }

    public void put(String source, String token, Artifact artifact) {
        JsonObject root = artifactToJson(artifact);
        root.addProperty("expiresAt", System.currentTimeMillis() + ttlFor(token).toMillis());
        writeLocal(source, token, root);

        Optional<FleetCache> fleet = fleetCacheRef == null ? Optional.empty() : fleetCacheRef.available();
        if (fleet.isPresent()) {
            try {
                fleet.get().put(L2_PREFIX + keyHash(source, token),
                        GSON.toJson(root).getBytes(StandardCharsets.UTF_8),
                        ttlFor(token));
            } catch (Throwable t) {
                logger.log(Level.FINE, "L2 resolve put failed for " + source + ":" + token, t);
            }
        }
    }

    private void writeLocal(String source, String token, JsonObject root) {
        Path file = entryPath(source, token);
        try {
            Files.createDirectories(file.getParent());
            byte[] bytes = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);

            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (FileChannel channel = FileChannel.open(tmp,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 FileLock ignored = channel.lock()) {
                channel.write(ByteBuffer.wrap(bytes));
            }
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.log(Level.FINE, "ResolveCache put failed for " + source + ":" + token, ex);
        }
    }

    /**
     * Cached negative result for "I asked source/token and it threw". Empty means we
     * don't have a recent negative for this pair.
     */
    public Optional<String> getNegative(String source, String token) {
        Path file = negativePath(source, token);
        if (Files.isRegularFile(file)) {
            try {
                String body = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                long expiresAt = root.has("expiresAt") ? root.get("expiresAt").getAsLong() : 0;
                if (System.currentTimeMillis() <= expiresAt) {
                    return Optional.of(root.has("message") ? root.get("message").getAsString() : "");
                }
            } catch (Throwable ignored) {
            }
        }
        Optional<FleetCache> fleet = fleetCacheRef == null ? Optional.empty() : fleetCacheRef.available();
        if (fleet.isPresent()) {
            try {
                Optional<byte[]> bytes = fleet.get().get(L2_NEG_PREFIX + keyHash(source, token));
                if (bytes.isPresent()) {
                    JsonObject root = JsonParser.parseString(new String(bytes.get(), StandardCharsets.UTF_8)).getAsJsonObject();
                    long expiresAt = root.has("expiresAt") ? root.get("expiresAt").getAsLong() : 0;
                    if (System.currentTimeMillis() <= expiresAt) {
                        return Optional.of(root.has("message") ? root.get("message").getAsString() : "");
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return Optional.empty();
    }

    public void putNegative(String source, String token, String message) {
        long expiresAt = System.currentTimeMillis() + NEGATIVE_TTL.toMillis();
        JsonObject root = new JsonObject();
        root.addProperty("expiresAt", expiresAt);
        root.addProperty("message", message == null ? "" : message);
        byte[] bytes = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        try {
            Path file = negativePath(source, token);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
        Optional<FleetCache> fleet = fleetCacheRef == null ? Optional.empty() : fleetCacheRef.available();
        if (fleet.isPresent()) {
            try {
                fleet.get().put(L2_NEG_PREFIX + keyHash(source, token), bytes, NEGATIVE_TTL);
            } catch (Throwable ignored) {
            }
        }
    }

    private Path entryPath(String source, String token) {
        return cacheDir.resolve(keyHash(source, token) + ".json");
    }

    private Path negativePath(String source, String token) {
        return cacheDir.resolve(keyHash(source, token) + ".neg.json");
    }
}
