/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.common.event.internal.listeners;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryClientSetItemEvent;
import com.deathmotion.totemguard.common.event.internal.impl.TotemReplenishedEvent;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

import java.util.function.Consumer;

public class TotemReplenishedListener implements Consumer<InventoryClientSetItemEvent> {

    @Override
    public void accept(InventoryClientSetItemEvent event) {
        var player = event.getPlayer();
        int slot = event.getSlot();

        if (slot != player.getInventory().getMainHandSlot() && slot != InventoryConstants.SLOT_OFFHAND) return;
        if (event.getNewStack().getType() != ItemTypes.TOTEM_OF_UNDYING) return;

        var lastTotemUse = player.getLastTotemUse();
        if (lastTotemUse == null) return;
        player.setLastTotemUse(null);

        long replenishedAt = event.getTimestamp();

        TotemReplenishedEvent replenishedEvent = new TotemReplenishedEvent(
                player,
                lastTotemUse,
                replenishedAt
        );

        TGPlatform.getInstance().getEventRepository().post(replenishedEvent);
    }
}
