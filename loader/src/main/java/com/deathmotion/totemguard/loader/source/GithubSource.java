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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class GithubSource implements VersionResolver {

    static final String REPOSITORY = "Bram1903/TotemGuard";
    private static final String API_BASE = "https://api.github.com";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private static JsonObject pickRelease(JsonArray releases, String requested) {
        String upper = requested.toUpperCase(java.util.Locale.ROOT);
        for (JsonElement element : releases) {
            JsonObject release = element.getAsJsonObject();
            if (release.has("draft") && release.get("draft").getAsBoolean()) continue;
            boolean prerelease = release.has("prerelease") && release.get("prerelease").getAsBoolean();
            String tag = release.has("tag_name") ? release.get("tag_name").getAsString() : "";

            boolean match = switch (upper) {
                case "LATEST" -> !prerelease;
                case "EXPERIMENTAL" -> prerelease;
                case "GIT" -> true;
                default -> tag.equals(requested) || tag.equals("v" + requested);
            };
            if (match) return release;
        }
        return null;
    }

    @Override
    public String sourceName() {
        return "GitHub";
    }

    @Override
    public Artifact resolve(LoaderConfig config, HostPlatform platform, LoaderPaths paths) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/repos/" + REPOSITORY + "/releases?per_page=30"))
                .timeout(READ_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub returned " + response.statusCode()
                    + " for " + REPOSITORY + " releases");
        }

        JsonElement parsed = JsonParser.parseString(response.body());
        if (!parsed.isJsonArray()) {
            throw new IOException("GitHub releases response was not a JSON array");
        }

        String requested = config.version();
        JsonObject picked = pickRelease(parsed.getAsJsonArray(), requested);
        if (picked == null) {
            throw new IOException("No GitHub release matched '" + requested + "' for " + REPOSITORY);
        }

        String tag = picked.get("tag_name").getAsString();
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        String assetSuffix = platform.assetSuffix();

        JsonArray assets = picked.has("assets") ? picked.getAsJsonArray("assets") : new JsonArray();
        for (JsonElement assetEl : assets) {
            JsonObject asset = assetEl.getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (!name.contains(assetSuffix) || !name.endsWith(".jar")) continue;

            String url = asset.get("browser_download_url").getAsString();
            String digest = asset.has("digest") && !asset.get("digest").isJsonNull()
                    ? asset.get("digest").getAsString() : null;
            if (digest == null || !digest.startsWith("sha256:")) {
                throw new IOException("GitHub asset " + name
                        + " is missing a SHA-256 digest; refusing to load without checksum verification");
            }
            String hashHex = digest.substring("sha256:".length());

            return new Artifact(version, URI.create(url), Artifact.HashAlgorithm.SHA_256,
                    hashHex, name, "GitHub " + REPOSITORY + " @ " + tag);
        }

        throw new IOException("No asset matching '*" + assetSuffix + "*.jar' on GitHub release " + tag);
    }
}
