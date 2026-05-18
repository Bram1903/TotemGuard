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

package com.deathmotion.totemguard.api.event.events;

import com.deathmotion.totemguard.api.event.Cancellable;
import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when a staff member toggles {@code /tg focus}, and re-fired when a
 * cached focus is restored on join. Target fields are populated only when
 * {@link #isEnabling()} is {@code true}. On a disable they are all {@code null}.
 */
public interface TGFocusEvent extends TGEvent, Cancellable {

    @NotNull UUID getCallerUuid();

    @Nullable UUID getTargetUuid();

    @Nullable String getTargetName();

    /**
     * The target's {@link TGUser} handle when online on this server. {@code null} otherwise.
     */
    @Nullable TGUser getTargetUser();

    @Nullable UUID getTargetServerInstanceId();

    @Nullable String getTargetServerName();

    /**
     * {@code true} when the call sets a focus, {@code false} when it clears one.
     */
    boolean isEnabling();

    /**
     * {@code true} when this is a cached-focus restore on join.
     */
    boolean isRestore();
}
