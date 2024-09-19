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
import com.deathmotion.totemguard.mojang.models.Callback;
import net.jodah.expiringmap.ExpiringMap;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MojangService {
    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    private final TotemGuard plugin;
    private final ExpiringMap<String, Callback> cache = ExpiringMap.builder()
            .expiration(10, TimeUnit.MINUTES)
            .build();

    public MojangService(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public Callback getUUID(String name) {
        name = name.toLowerCase();

        Callback cachedCallback = cache.get(name);
        if (cachedCallback != null) {
            return cachedCallback;
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
            case HttpURLConnection.HTTP_BAD_REQUEST -> handleBadRequest(responseMessage);
            case HttpURLConnection.HTTP_NOT_FOUND -> handleNotFound();
            case 429 -> new Callback(429, responseMessage);
            default -> {
                plugin.getLogger().warning("Unexpected response code: " + responseCode);
                yield new Callback(-1, null);
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
        cache.put(name, response); // Cache with automatic expiry
        return response;
    }

    private Callback handleBadRequest(String responseMessage) {
        JSONObject jsonResponse = new JSONObject(responseMessage);
        String errorMessage = jsonResponse.getString("errorMessage");
        return new Callback(HttpURLConnection.HTTP_BAD_REQUEST, errorMessage);
    }

    private Callback handleNotFound() {
        return new Callback(HttpURLConnection.HTTP_NOT_FOUND, null);
    }

    private String formatUUID(String rawUuid) {
        // Insert hyphens into the UUID string
        return rawUuid.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
    }
}