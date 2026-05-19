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

package com.deathmotion.totemguard.common.util;

import com.deathmotion.totemguard.common.TGPlatform;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public final class LoggerSuppressor {

    private static final Method SET_LEVEL_METHOD = resolveSetLevelMethod();

    private LoggerSuppressor() {
    }

    private static Method resolveSetLevelMethod() {
        try {
            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            return configuratorClass.getMethod("setLevel", String.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return null;
        }
    }

    public static void suppress(Collection<String> loggerNames) {
        if (SET_LEVEL_METHOD == null) {
            return;
        }

        for (String loggerName : loggerNames) {
            try {
                SET_LEVEL_METHOD.invoke(null, loggerName, "ERROR");
            } catch (ReflectiveOperationException e) {
                TGPlatform.getInstance().getLogger().severe("Failed to suppress logger '" + loggerName + "'" + e.getMessage());
            }
        }
    }

    public static void suppressDefaultNoise() {
        suppress(List.of(
                "com.deathmotion.totemguard.common.libs.lettuce.core.protocol.ConnectionWatchdog",
                "com.deathmotion.totemguard.common.libs.lettuce.core.protocol.ReconnectionHandler",
                "com.deathmotion.totemguard.common.libs.hikari"
        ));
    }
}