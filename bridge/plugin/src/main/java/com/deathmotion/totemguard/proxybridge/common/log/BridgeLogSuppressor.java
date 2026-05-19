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

package com.deathmotion.totemguard.proxybridge.common.log;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BridgeLogSuppressor {

    private static final List<String> NOISY = List.of(
            "com.deathmotion.totemguard.proxybridge.libs.lettuce.core.protocol.ConnectionWatchdog",
            "com.deathmotion.totemguard.proxybridge.libs.lettuce.core.protocol.ReconnectionHandler"
    );

    private static final Method LOG4J_SET_LEVEL = resolveLog4j();
    private static final Method LOGBACK_GET_LOGGER = resolveLogback();
    private static final Object LOGBACK_ERROR_LEVEL = resolveLogbackLevel();

    private BridgeLogSuppressor() {
    }

    public static void suppressDefaultNoise() {
        for (String name : NOISY) suppress(name);
    }

    private static void suppress(@NotNull String name) {
        if (suppressLog4j(name)) return;
        if (suppressLogback(name)) return;
        Logger.getLogger(name).setLevel(Level.SEVERE);
    }

    private static boolean suppressLog4j(@NotNull String name) {
        if (LOG4J_SET_LEVEL == null) return false;
        try {
            LOG4J_SET_LEVEL.invoke(null, name, "ERROR");
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean suppressLogback(@NotNull String name) {
        if (LOGBACK_GET_LOGGER == null || LOGBACK_ERROR_LEVEL == null) return false;
        try {
            Object logger = LOGBACK_GET_LOGGER.invoke(null, name);
            if (logger == null) return false;
            Method setLevel = logger.getClass().getMethod("setLevel", LOGBACK_ERROR_LEVEL.getClass());
            setLevel.invoke(logger, LOGBACK_ERROR_LEVEL);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Method resolveLog4j() {
        try {
            Class<?> cfg = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            return cfg.getMethod("setLevel", String.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method resolveLogback() {
        try {
            Class<?> factory = Class.forName("org.slf4j.LoggerFactory");
            return factory.getMethod("getLogger", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object resolveLogbackLevel() {
        try {
            Class<?> level = Class.forName("ch.qos.logback.classic.Level");
            return level.getField("ERROR").get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }
}
