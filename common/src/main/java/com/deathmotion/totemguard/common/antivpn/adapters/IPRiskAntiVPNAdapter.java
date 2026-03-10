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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class IPRiskAntiVPNAdapter extends AntiVPNAdapter {

    private static final String API_URL = "https://api.iprisk.info/v1/";

    @Override
    public @NotNull String getName() {
        return "IPRisk";
    }

    @Override
    public boolean isVpn(@NotNull String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + URLEncoder.encode(ip, StandardCharsets.UTF_8)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return false;
            }

            JsonObject json = getJsonParser().parse(response.body()).getAsJsonObject();

            return getBoolean(json, "vpn") || getBoolean(json, "open_proxy") || getBoolean(json, "data_center");
        } catch (IOException | InterruptedException | IllegalStateException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private boolean getBoolean(@NotNull JsonObject json, @NotNull String key) {
        return json.has(key) && !json.get(key).isJsonNull() && json.get(key).getAsBoolean();
    }
}