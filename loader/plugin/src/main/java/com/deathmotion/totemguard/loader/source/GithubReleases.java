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

import com.deathmotion.totemguard.loader.fleet.FleetCacheRef;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Optional;

final class GithubReleases {

    static final String L2_KEY = "totemguard:loader:cache:github:releases";
    private static final String API_BASE = "https://api.github.com";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int PER_PAGE = 100;

    private GithubReleases() {
    }

    static JsonArray fetchAll(@Nullable FleetCacheRef fleetCacheRef) throws IOException, InterruptedException {
        Path cacheFile = cachePath();
        JsonArray cached = readCacheIfFresh(cacheFile);
        if (cached != null) return cached;

        boolean l2Ready = fleetCacheRef != null && fleetCacheRef.isApiReady();
        if (l2Ready) {
            Optional<byte[]> l2 = fleetCacheRef.l2Get(L2_KEY);
            if (l2.isPresent()) {
                String body = new String(l2.get(), StandardCharsets.UTF_8);
                JsonElement parsed = JsonParser.parseString(body);
                if (parsed.isJsonArray()) {
                    writeCache(cacheFile, body);
                    return parsed.getAsJsonArray();
                }
            }
        }

        HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/repos/" + GithubSource.REPOSITORY
                        + "/releases?per_page=" + PER_PAGE))
                .timeout(READ_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub returned " + HttpStatusText.describe(response.statusCode())
                    + " for the releases listing.");
        }
        JsonElement parsed = JsonParser.parseString(response.body());
        if (!parsed.isJsonArray()) {
            throw new IOException("GitHub releases response was not a JSON array");
        }

        writeCache(cacheFile, response.body());
        if (l2Ready) {
            fleetCacheRef.l2Put(L2_KEY, response.body().getBytes(StandardCharsets.UTF_8), CACHE_TTL);
        }
        return parsed.getAsJsonArray();
    }

    private static JsonArray readCacheIfFresh(Path cacheFile) {
        try {
            if (!Files.isRegularFile(cacheFile)) return null;
            long age = System.currentTimeMillis() - Files.getLastModifiedTime(cacheFile).toMillis();
            if (age > CACHE_TTL.toMillis()) return null;
            String json = Files.readString(cacheFile);
            JsonElement parsed = JsonParser.parseString(json);
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeCache(Path cacheFile, String body) {
        try {
            Files.createDirectories(cacheFile.getParent());
            Path tmp = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");
            Files.writeString(tmp, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private static Path cachePath() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "totemguard-loader-cache", "github-releases.json");
    }
}
