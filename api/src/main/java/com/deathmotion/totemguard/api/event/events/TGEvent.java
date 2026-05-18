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

import org.jetbrains.annotations.NotNull;

/**
 * Root marker for every event TotemGuard fires through its public event bus.
 * <p>
 * Subscribing to {@code TGEvent.class} fans the handler out across every
 * concrete event. Useful for metrics, debug logging, or audit hooks that need
 * to see everything without enumerating each type. Cast or pattern-match on
 * the concrete subtype when you need field access:
 * <pre>{@code
 * bus.get(TGEvent.class).subscribe(pluginContext, event -> {
 *     if (event instanceof TGUserFlagEvent flag) {
 *         metrics.increment("flags." + flag.getCheck().getName());
 *     }
 * });
 * }</pre>
 */
public interface TGEvent {

    /**
     * Convenience name for this event. Defaults to the implementing class's
     * simple name, which is good enough for log lines and dashboards.
     */
    default @NotNull String getName() {
        return getClass().getSimpleName();
    }
}
