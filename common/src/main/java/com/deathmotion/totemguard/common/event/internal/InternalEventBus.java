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

package com.deathmotion.totemguard.common.event.internal;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the event channels TotemGuard fires for its own subsystems. These
 * never reach the public {@link com.deathmotion.totemguard.api.event.EventBus},
 * so third-party plugins cannot listen for or trigger them.
 * <p>
 * Each channel is exposed through its own getter. Firing an internal event
 * just calls the channel directly with the relevant values, without wrapping
 * them in an event object.
 */
@Getter
public final class InternalEventBus {

    private final TotemActivatedChannel totemActivated = new TotemActivatedChannel();
    private final TotemReplenishedChannel totemReplenished = new TotemReplenishedChannel();
    private final InventoryChangedChannel inventoryChanged = new InventoryChangedChannel();

    /**
     * Sweeps every internal subscription owned by the given context.
     */
    public void unregisterAll(@NotNull Object pluginContext) {
        totemActivated.unsubscribeAllFromPlugin(pluginContext);
        totemReplenished.unsubscribeAllFromPlugin(pluginContext);
        inventoryChanged.unsubscribeAllFromPlugin(pluginContext);
    }
}
