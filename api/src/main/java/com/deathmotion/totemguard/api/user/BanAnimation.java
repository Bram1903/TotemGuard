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
 * Handle for TotemGuard's ban animation, accessed via
 * {@link TGUser#getBanAnimation()}. The animation shows a fake totem-pop with
 * a "hacker" player head and is purely visual. The user's real inventory is
 * not touched.
 */
public interface BanAnimation {

    /**
     * Plays the ban animation for this user. Bypasses the
     * {@code ban-animation.enabled} config toggle, so callers that want to
     * integrate the effect into their own punishment flow can do so
     * regardless of the operator's setting. The caller is responsible for
     * timing any follow-up kick or ban. See {@link #getDurationMs()} for the
     * recommended pause.
     * <p>
     * No-op when the client is older than 1.21.2. The totem-pop check on
     * those versions is hardcoded to the totem item type and can't render
     * the head. Use {@link #isSupported()} to gate before calling.
     */
    void play();

    /**
     * @return {@code true} if this user's client can render the hacker-head
     * variant of the ban animation (1.21.2+). Older clients return
     * {@code false}, and calling {@link #play()} on them is a no-op.
     */
    boolean isSupported();

    /**
     * @return how long the animation takes to play, in milliseconds. Use
     * this when scheduling a follow-up kick or ban so the user actually sees
     * the pop before being disconnected.
     */
    long getDurationMs();
}
