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

package com.deathmotion.totemguard.api.placeholder;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Placeholder replacement and registration. Placeholders use {@code %key%} syntax.
 */
public interface PlaceholderRepository {

    /**
     * Replaces every {@code %key%} in {@code message} using the supplied context. Unknown
     * keys are left literal (the original {@code %key%} text). Built-in keys resolve first,
     * registered holders only see keys the core could not handle.
     */
    @NotNull
    String replace(@NotNull String message, @NotNull PlaceholderContext context);

    /**
     * Replace with the {@linkplain PlaceholderContext#empty() empty context}. User-bound
     * and check-bound placeholders won't resolve.
     */
    default @NotNull String replace(@NotNull String message) {
        return replace(message, PlaceholderContext.empty());
    }

    /**
     * Replace binding only a user, so {@code %tg_player%} and related user keys resolve
     * but check-bound keys don't.
     */
    default @NotNull String replace(@NotNull String message, @Nullable TGUser user) {
        return replace(message, new PlaceholderContext(user, null));
    }

    /**
     * Replace binding only a check, so check-bound keys (name, violation count) resolve
     * but user-bound keys don't.
     */
    default @NotNull String replace(@NotNull String message, @Nullable Check check) {
        return replace(message, new PlaceholderContext(null, check));
    }

    /**
     * Replace binding both a user and a check, the typical call site for alert and
     * punishment-command rendering.
     */
    default @NotNull String replace(@NotNull String message, @Nullable TGUser user, @Nullable Check check) {
        return replace(message, new PlaceholderContext(user, check));
    }

    /**
     * Registered placeholder keys from holders implementing {@link PlaceholderProvider},
     * without surrounding '%'.
     */
    @NotNull
    Set<String> registeredKeys();

    /**
     * Registered dynamic placeholder patterns. Descriptive metadata, not guaranteed
     * exhaustive.
     */
    default @NotNull Set<String> registeredPatterns() {
        return Set.of();
    }

    /**
     * Register a holder.
     *
     * @return {@code true} if registered, {@code false} if already present
     */
    boolean registerHolder(@NotNull PlaceholderHolder holder);

    /**
     * Unregister a holder.
     *
     * @return {@code true} if removed, {@code false} if not present
     */
    boolean unregisterHolder(@NotNull PlaceholderHolder holder);
}
