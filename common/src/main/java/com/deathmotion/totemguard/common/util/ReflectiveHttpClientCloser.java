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

import java.lang.reflect.Method;
import java.net.http.HttpClient;

// Compiled with --release 17 but runs on JDK 21+, where HttpClient is AutoCloseable.
// Reflectively call close() so the selector + executor threads are released on disable.
public final class ReflectiveHttpClientCloser {

    private static final Method CLOSE = lookup();

    private ReflectiveHttpClientCloser() {
    }

    private static Method lookup() {
        try {
            return HttpClient.class.getMethod("close");
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static void tryClose(HttpClient client) {
        if (client == null || CLOSE == null) return;
        try {
            CLOSE.invoke(client);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
