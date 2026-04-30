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

package com.deathmotion.totemguard.api.result;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of a single asynchronous repository call. Always present on a settled future,
 * so callers never have to attach {@code .exceptionally(...)} just to learn that the
 * database was offline. Inspect {@link #ok()} first; on {@code false}, {@link #error()}
 * and {@link #message()} carry the reason.
 *
 * <pre>
 *     repo.someCall().thenAccept(result -&gt; {
 *         if (result.ok()) {
 *             render(result.value());
 *         } else {
 *             player.sendMessage("Unavailable: " + result.message());
 *         }
 *     });
 * </pre>
 *
 * @param ok      {@code true} if the call succeeded; {@code value} is non-null in that case.
 * @param value   the result, or {@code null} when {@code ok} is {@code false}.
 * @param error   the failure category, or {@code null} when {@code ok} is {@code true}.
 * @param message human-readable reason, never {@code null} when {@code ok} is {@code false}.
 */
public record Result<T>(
        boolean ok,
        @Nullable T value,
        @Nullable ResultError error,
        @Nullable String message
) {

    @Contract(value = "_ -> new", pure = true)
    public static <T> @NotNull Result<T> ok(@NotNull T value) {
        return new Result<>(true, value, null, null);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static <T> @NotNull Result<T> failure(@NotNull ResultError error, @NotNull String message) {
        return new Result<>(false, null, error, message);
    }

    /**
     * Convenience overload that derives a default message from the error constant.
     */
    @Contract(value = "_ -> new", pure = true)
    public static <T> @NotNull Result<T> failure(@NotNull ResultError error) {
        return failure(error, defaultMessage(error));
    }

    private static String defaultMessage(ResultError error) {
        return switch (error) {
            case DATABASE_UNAVAILABLE -> "Database is disabled or currently unreachable";
            case INTERNAL_ERROR -> "Internal error";
        };
    }
}
