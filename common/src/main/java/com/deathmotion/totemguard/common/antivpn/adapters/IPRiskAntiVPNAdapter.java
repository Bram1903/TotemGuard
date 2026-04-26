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

package com.deathmotion.totemguard.common.antivpn.adapters;

import com.deathmotion.totemguard.common.antivpn.AntiVPNAdapter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class IPRiskAntiVPNAdapter extends AntiVPNAdapter {

    private static final String API_URL = "https://api.iprisk.info/v1/";

    private static boolean readBoolean(@NotNull JsonObject json, @NotNull String key) {
        return json.has(key) && !json.get(key).isJsonNull() && json.get(key).getAsBoolean();
    }

    @Override
    public @NotNull String getName() {
        return "IPRisk";
    }

    @Override
    public @Nullable Boolean lookup(@NotNull String ip) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + URLEncoder.encode(ip, StandardCharsets.UTF_8)))
                .header("Accept", "application/json")
                .header("User-Agent", "TotemGuard/3")
                .timeout(getHttpTimeout())
                .GET()
                .build();

        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        if (response.statusCode() != 200 || body == null || body.isBlank()) {
            return null;
        }

        JsonObject json;
        try {
            //noinspection deprecation
            json = new JsonParser().parse(body).getAsJsonObject();
        } catch (RuntimeException ex) {
            return null;
        }

        return readBoolean(json, "vpn")
                || readBoolean(json, "open_proxy")
                || readBoolean(json, "data_center");
    }
}
