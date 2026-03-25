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

package com.deathmotion.totemguard.common.event.internal.listeners;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.event.internal.impl.TotemReplenishedEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;

import java.util.function.Consumer;

public class TotemReplenishedListener implements Consumer<InventoryChangedEvent> {

    private static final long MAX_REASONABLE_DELAY_MS = 5_000;

    @Override
    public void accept(InventoryChangedEvent event) {
        if (event.getLastIssuer() == Issuer.SERVER) return;

        TGPlayer player = event.getPlayer();
        PacketInventory inventory = player.getInventory();

        for (InventorySlot inventorySlot : event.getChangedSlots()) {
            int slot = inventorySlot.getSlot();

            if (!inventory.isHandSlot(slot) || !inventory.isTotemInSlot(slot)) {
                continue;
            }

            Long lastTotemUse = player.getLastTotemUse();

            if (lastTotemUse == null) {
                return;
            }

            long replenishedAt = inventorySlot.getUpdated();
            long deltaRaw = replenishedAt - lastTotemUse;

            if (deltaRaw > MAX_REASONABLE_DELAY_MS) {
                return;
            }

            player.getTotemData().getIntervals().add(deltaRaw);

            TotemReplenishedEvent replenishedEvent = new TotemReplenishedEvent(
                    player,
                    deltaRaw,
                    replenishedAt
            );
            TGPlatform.getInstance().getEventRepository().post(replenishedEvent);

            player.setLastTotemUse(null);

            return;
        }
    }
}
