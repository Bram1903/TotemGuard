/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.messaging;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.messaging.impl.PluginMessageProxyMessenger;
import com.deathmotion.totemguard.messaging.impl.RedisProxyMessenger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Map.entry;

public class AlertMessengerRegistry {
    private static final Map<String, Function<TotemGuard, ProxyAlertMessenger>> types = Map.ofEntries(
            entry("redis", RedisProxyMessenger::new),
            entry("plugin-messaging", PluginMessageProxyMessenger::new)
    );

    private AlertMessengerRegistry() {
    }

    public static Optional<Function<TotemGuard, ProxyAlertMessenger>> getMessengerSupplier(@NotNull String identifier) {
        return Optional.of(types.get(identifier));
    }

    public static Optional<ProxyAlertMessenger> getMessenger(@NotNull String identifier, @NotNull TotemGuard instance) {
        return getMessengerSupplier(identifier).map((func) -> func.apply(instance));
    }
}
