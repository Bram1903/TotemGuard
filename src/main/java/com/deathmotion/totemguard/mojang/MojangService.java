/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.mojang;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.mojang.models.CacheEntry;
import com.deathmotion.totemguard.mojang.models.Callback;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MojangService {
    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final long CACHE_EXPIRY_DURATION = TimeUnit.MINUTES.toMillis(10);

    private final TotemGuard plugin;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public MojangService(TotemGuard plugin) {
        this.plugin = plugin;
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> cleanupCache(), 10, 10, TimeUnit.MINUTES);
    }

    private void cleanupCache() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> now - entry.getValue().timestamp() >= CACHE_EXPIRY_DURATION);
    }

    public Callback getUUID(String name) {
        name = name.toLowerCase();

        CacheEntry entry = cache.get(name);
        if (entry != null && (System.currentTimeMillis() - entry.timestamp() < CACHE_EXPIRY_DURATION)) {
            return entry.value();
        }

        try {
            HttpURLConnection connection = createConnection(name);
            int responseCode = connection.getResponseCode();
            String responseMessage = readResponse(connection);

            return handleResponse(responseCode, responseMessage, name);
        } catch (Exception e) {
            plugin.getLogger().severe("Error while getting UUID for " + name + ": " + e.getMessage());
            return null;
        }
    }

    private HttpURLConnection createConnection(String name) throws Exception {
        URL url = new URL(API_URL + name);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        }
    }

    private Callback handleResponse(int responseCode, String responseMessage, String name) {
        return switch (responseCode) {
            case HttpURLConnection.HTTP_OK -> handleFoundResponse(responseMessage, name);
            case HttpURLConnection.HTTP_BAD_REQUEST -> handleBadRequest(responseMessage, name);
            case HttpURLConnection.HTTP_NOT_FOUND -> handleNotFound(name);
            case 429 -> new Callback(429, null);
            default -> {
                plugin.getLogger().warning("Unexpected response code: " + responseCode);
                yield null;
            }
        };
    }

    private Callback handleFoundResponse(String responseMessage, String name) {
        JSONObject jsonResponse = new JSONObject(responseMessage);
        String rawUuid = jsonResponse.getString("id");
        String formattedUuid = formatUUID(rawUuid);
        UUID uuid = UUID.fromString(formattedUuid);
        String username = jsonResponse.getString("name");

        Callback response = new Callback(username, uuid);
        cache.put(name, new CacheEntry(response, System.currentTimeMillis())); // Cache with timestamp
        return response;
    }

    private Callback handleBadRequest(String responseMessage, String name) {
        JSONObject jsonResponse = new JSONObject(responseMessage);
        String errorMessage = jsonResponse.getString("errorMessage");
        Callback response = new Callback(HttpURLConnection.HTTP_BAD_REQUEST, errorMessage);
        cache.put(name, new CacheEntry(response, System.currentTimeMillis()));
        return response;
    }

    private Callback handleNotFound(String name) {
        Callback response = new Callback(HttpURLConnection.HTTP_NOT_FOUND, null);
        cache.put(name, new CacheEntry(response, System.currentTimeMillis()));
        return response;
    }

    private String formatUUID(String rawUuid) {
        // Insert hyphens into the UUID string
        return rawUuid.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
    }
}
