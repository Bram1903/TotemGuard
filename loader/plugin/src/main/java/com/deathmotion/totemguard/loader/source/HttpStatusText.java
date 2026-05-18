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

package com.deathmotion.totemguard.loader.source;

/**
 * Formats an HTTP status as {@code "code (reason)"} so operators reading the log don't
 * need to remember what 429 or 503 mean. Only the codes the loader actually surfaces via
 * upstream APIs (GitHub, Modrinth) are covered; anything else falls through to bare
 * "code (status N)".
 */
public final class HttpStatusText {

    private HttpStatusText() {
    }

    public static String describe(int code) {
        String reason = switch (code) {
            case 400 -> "bad request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden, likely rate-limited";
            case 404 -> "not found";
            case 408 -> "request timeout";
            case 422 -> "unprocessable entity";
            case 429 -> "rate limited";
            case 500 -> "internal server error";
            case 502 -> "bad gateway";
            case 503 -> "service unavailable";
            case 504 -> "gateway timeout";
            default -> null;
        };
        return reason == null ? code + " (status " + code + ")" : code + " (" + reason + ")";
    }
}
