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

package com.deathmotion.totemguard.common.antivpn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public abstract class AntiVPNAdapter {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    private @Nullable String apiKey;

    public abstract @NotNull String getName();

    public boolean requiresApiKey() {
        return false;
    }

    public final void configure(@Nullable String apiKey) {
        this.apiKey = (apiKey == null || apiKey.isBlank()) ? null : apiKey;
    }

    protected final @Nullable String getApiKey() {
        return apiKey;
    }

    protected final @NotNull HttpClient getHttpClient() {
        return httpClient;
    }

    protected final @NotNull Duration getHttpTimeout() {
        return HTTP_TIMEOUT;
    }

    // null = inconclusive lookup; the repository treats it as not-VPN.
    public abstract @Nullable Boolean lookup(@NotNull String ip) throws IOException, InterruptedException;
}
