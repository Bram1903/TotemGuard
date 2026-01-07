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

/**
 * Handles placeholder replacement and registration.
 * <p>
 * Placeholders use the format {@code %key%}.
 */
public interface PlaceholderRepository {

    /**
     * Replace placeholders without any context.
     *
     * @param message input message
     * @return message with placeholders replaced
     */
    @NotNull
    String replace(@NotNull String message);

    /**
     * Replace placeholders using a user context.
     *
     * @param message input message
     * @param user    user context (nullable)
     * @return message with placeholders replaced
     */
    @NotNull
    String replace(@NotNull String message, @Nullable TGUser user);

    /**
     * Replace placeholders using a check context.
     *
     * @param message input message
     * @param check   check context (nullable)
     * @return message with placeholders replaced
     */
    @NotNull
    String replace(@NotNull String message, @Nullable Check check);

    /**
     * Replace placeholders using both user and check context.
     *
     * @param message input message
     * @param user    user context (nullable)
     * @param check   check context (nullable)
     * @return message with placeholders replaced
     */
    @NotNull
    String replace(@NotNull String message, @Nullable TGUser user, @Nullable Check check);

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

