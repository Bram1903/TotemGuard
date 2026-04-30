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

package com.deathmotion.totemguard.api.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Repository interface for accessing {@link TGUser} instances.
 */
public interface UserRepository {

    /**
     * Retrieves a user by their unique UUID.
     * <p>
     * This is a synchronous, in-memory lookup against the set of users currently tracked
     * on this server. It is safe to call from any thread (including Bukkit event
     * handlers) and never touches the database.
     *
     * @param uuid the UUID of the user
     * @return the {@link TGUser} if currently online and tracked, or {@code null} if the
     * user has never joined this server, has logged off, or has not yet been registered
     */
    @Nullable TGUser getUser(@NotNull UUID uuid);
}
