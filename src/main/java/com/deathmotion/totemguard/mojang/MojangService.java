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
import com.deathmotion.totemguard.mojang.models.BadRequest;
import com.deathmotion.totemguard.mojang.models.Found;
import com.deathmotion.totemguard.mojang.models.NoContent;
import com.deathmotion.totemguard.mojang.models.TooManyRequests;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MojangService {

    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private final TotemGuard plugin;
    private final ConcurrentHashMap<String, ApiResponse> cache = new ConcurrentHashMap<>();

    public MojangService(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public ApiResponse getUUID(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
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

    private ApiResponse handleResponse(int responseCode, String responseMessage, String name) {
        return switch (responseCode) {
            case HttpURLConnection.HTTP_OK -> handleFoundResponse(responseMessage, name);
            case HttpURLConnection.HTTP_NO_CONTENT -> new NoContent(HttpURLConnection.HTTP_NO_CONTENT);
            case HttpURLConnection.HTTP_BAD_REQUEST -> handleBadRequest(responseMessage, name);
            case 429 -> new TooManyRequests(429);
            default -> {
                plugin.getLogger().warning("Unexpected response code: " + responseCode);
                yield null;
            }
        };
    }

    private Found handleFoundResponse(String responseMessage, String name) {
        JSONObject jsonResponse = new JSONObject(responseMessage);
        String rawUuid = jsonResponse.getString("id");
        String formattedUuid = formatUUID(rawUuid);
        UUID uuid = UUID.fromString(formattedUuid);
        String username = jsonResponse.getString("name");

        Found response = new Found(HttpURLConnection.HTTP_OK, uuid, username);
        cache.put(name, response);
        return response;
    }

    private BadRequest handleBadRequest(String responseMessage, String name) {
        JSONObject jsonResponse = new JSONObject(responseMessage);
        String errorMessage = jsonResponse.getString("errorMessage");
        plugin.getLogger().warning("Bad request for " + name + ": " + errorMessage);
        return new BadRequest(HttpURLConnection.HTTP_BAD_REQUEST, errorMessage);
    }

    private String formatUUID(String rawUuid) {
        // Insert hyphens into the UUID string
        return rawUuid.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
    }
}
