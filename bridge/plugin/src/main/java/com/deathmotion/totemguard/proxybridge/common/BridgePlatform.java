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

package com.deathmotion.totemguard.proxybridge.common;

import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public interface BridgePlatform {

    @NotNull
    Kind kind();

    @NotNull
    Logger logger();

    @NotNull
    Set<String> registeredBackendNames();

    @NotNull
    Map<String, InetSocketAddress> registeredBackends();

    void connect(@NotNull UUID playerUuid, @NotNull String targetBackend);

    void scheduleRepeating(@NotNull Runnable task, long delay, long period, @NotNull TimeUnit unit);

    enum Kind {VELOCITY, BUNGEECORD}
}
