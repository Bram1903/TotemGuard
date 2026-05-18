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

package com.deathmotion.totemguard.api.event;

/**
 * Reference to a single registration on a channel. Calling
 * {@link #unsubscribe()} detaches just that one handler.
 * <p>
 * For the common case of cleaning up on plugin disable, do not bother
 * keeping these around. Hand a plugin context to subscribe and rely on
 * {@link EventBus#unregisterAll(Object)} to clear them in bulk. Hold on to
 * a {@code Subscription} only if you want to detach one specific handler
 * while the plugin is still running.
 */
public interface Subscription {

    /**
     * Detaches the handler this subscription points at. Idempotent.
     */
    void unsubscribe();
}
