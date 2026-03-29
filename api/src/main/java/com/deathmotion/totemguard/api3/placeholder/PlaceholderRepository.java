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

package com.deathmotion.totemguard.api3.placeholder;

import com.deathmotion.totemguard.api3.check.Check;
import com.deathmotion.totemguard.api3.user.TGUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Handles placeholder replacement and registration.
 * <p>
 * Placeholders use the format {@code %key%}.
 */
public interface PlaceholderRepository {

    /**
     * Replaces placeholders using the supplied resolution context.
     *
     * @param message input message
     * @param context resolution context
     * @return message with placeholders replaced
     */
    @NotNull
    String replace(@NotNull String message, @NotNull PlaceholderContext context);

    /**
     * Replace placeholders without any context.
     *
     * @param message input message
     * @return message with placeholders replaced
     */
    default @NotNull String replace(@NotNull String message) {
        return replace(message, PlaceholderContext.empty());
    }

    /**
     * Replace placeholders using a user context.
     *
     * @param message input message
     * @param user    user context (nullable)
     * @return message with placeholders replaced
     */
    default @NotNull String replace(@NotNull String message, @Nullable TGUser user) {
        return replace(message, new PlaceholderContext(user, null));
    }

    /**
     * Replace placeholders using a check context.
     *
     * @param message input message
     * @param check   check context (nullable)
     * @return message with placeholders replaced
     */
    default @NotNull String replace(@NotNull String message, @Nullable Check check) {
        return replace(message, new PlaceholderContext(null, check));
    }

    /**
     * Replace placeholders using both user and check context.
     *
     * @param message input message
     * @param user    user context (nullable)
     * @param check   check context (nullable)
     * @return message with placeholders replaced
     */
    default @NotNull String replace(@NotNull String message, @Nullable TGUser user, @Nullable Check check) {
        return replace(message, new PlaceholderContext(user, check));
    }

    /**
     * Returns all currently registered placeholder keys for holders that implement {@link PlaceholderProvider}.
     *
     * <p>Keys are returned without surrounding '%' characters.</p>
     *
     * @return an immutable set of registered placeholder keys
     */
    @NotNull
    Set<String> registeredKeys();

    /**
     * Returns the currently registered dynamic placeholder patterns.
     *
     * <p>Patterns are descriptive metadata exposed by holders implementing
     * {@link PlaceholderProvider}. They are not required to be exhaustive.</p>
     *
     * @return an immutable set of registered placeholder patterns
     */
    default @NotNull Set<String> registeredPatterns() {
        return Set.of();
    }

    /**
     * Register a custom placeholder holder.
     *
     * @param holder holder to register
     * @return true if registered, false if already present
     */
    boolean registerHolder(@NotNull PlaceholderHolder holder);

    /**
     * Unregister a previously registered placeholder holder.
     *
     * @param holder holder to unregister
     * @return true if removed, false if not present
     */
    boolean unregisterHolder(@NotNull PlaceholderHolder holder);
}
