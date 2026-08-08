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

/**
 * Handle for TotemGuard's ban animation (a fake totem-pop with a hacker player head).
 * Purely visual, does not touch the user's inventory.
 */
public interface BanAnimation {

    /**
     * Plays the animation, bypassing the {@code ban-animation.enabled} config toggle.
     * No-op on clients older than 1.21.2 (gate with {@link #isSupported()}). Caller
     * times any follow-up kick or ban, see {@link #getDurationMs()}.
     */
    void play();

    /**
     * Whether the client can render the hacker-head variant (1.21.2+).
     */
    boolean isSupported();

    /**
     * Animation duration in milliseconds, for timing a follow-up kick or ban.
     */
    long getDurationMs();
}
