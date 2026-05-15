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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public final class ModrinthSource implements VersionResolver {

    static final String PROJECT_ID = "wRuOKIM4";
    private static final String API_BASE = "https://api.modrinth.com/v2";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private static JsonObject pickVersion(JsonArray versions, String requested) {
        String upper = requested.toUpperCase(Locale.ROOT);
        for (JsonElement element : versions) {
            JsonObject version = element.getAsJsonObject();
            String type = version.has("version_type") ? version.get("version_type").getAsString() : "release";
            String number = version.has("version_number") ? version.get("version_number").getAsString() : "";

            boolean match = switch (upper) {
                case "LATEST" -> type.equals("release");
                case "EXPERIMENTAL" -> type.equals("alpha") || type.equals("beta");
                default -> number.equals(requested);
            };
            if (match) return version;
        }
        return null;
    }

    @Override
    public String sourceName() {
        return "Modrinth";
    }

    @Override
    public Artifact resolve(LoaderConfig config, HostPlatform platform, LoaderPaths paths) throws Exception {
        String loaderFilter = URLEncoder.encode("[\"" + platform.modrinthLoader() + "\"]", StandardCharsets.UTF_8);
        HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/project/" + PROJECT_ID + "/version?loaders=" + loaderFilter))
                .timeout(READ_TIMEOUT)
                .header("Accept", "application/json")
                .GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Modrinth returned " + response.statusCode()
                    + " for project " + PROJECT_ID);
        }

        JsonElement parsed = JsonParser.parseString(response.body());
        if (!parsed.isJsonArray()) {
            throw new IOException("Modrinth versions response was not a JSON array");
        }

        String requested = config.version();
        JsonObject picked = pickVersion(parsed.getAsJsonArray(), requested);
        if (picked == null) {
            throw new IOException("No Modrinth version matched '" + requested + "' for " + PROJECT_ID);
        }

        String number = picked.get("version_number").getAsString();
        JsonArray files = picked.getAsJsonArray("files");
        JsonObject primary = null;
        for (JsonElement fileEl : files) {
            JsonObject file = fileEl.getAsJsonObject();
            if (file.has("primary") && file.get("primary").getAsBoolean()) {
                primary = file;
                break;
            }
        }
        if (primary == null && !files.isEmpty()) primary = files.get(0).getAsJsonObject();
        if (primary == null) throw new IOException("Modrinth version " + number + " has no files");

        String fileName = primary.get("filename").getAsString();
        String url = primary.get("url").getAsString();
        JsonObject hashes = primary.has("hashes") ? primary.getAsJsonObject("hashes") : new JsonObject();
        String sha512 = hashes.has("sha512") ? hashes.get("sha512").getAsString() : null;
        if (sha512 == null) {
            throw new IOException("Modrinth file " + fileName
                    + " is missing a SHA-512 hash; refusing to load without checksum verification");
        }

        return new Artifact(number, URI.create(url), Artifact.HashAlgorithm.SHA_512,
                sha512, fileName, "Modrinth " + PROJECT_ID + " @ " + number);
    }
}
