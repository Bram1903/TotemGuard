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

    private static final boolean DEBUG = false;
    private static final long MAX_REASONABLE_DELAY_MS = 5_000;

    @Override
    public void accept(InventoryChangedEvent event) {
        if (event.getLastIssuer() == Issuer.SERVER) return;

        TGPlayer player = event.getPlayer();
        PacketInventory inventory = player.getInventory();
        String playerName = player.getName();

        for (InventorySlot inventorySlot : event.getChangedSlots()) {
            int slot = inventorySlot.getSlot();

            if (!inventory.isHandSlot(slot) || !inventory.isTotemInSlot(slot)) {
                continue;
            }

            Long lastTotemUseCompensated = player.getLastTotemUseCompensated();
            Long lastTotemUse = player.getLastTotemUse();

            if (lastTotemUse == null) {
                if (DEBUG) {
                    TGPlatform.getInstance().getLogger().warning(String.format(
                            "[TotemReplenished] player=%s FOUND totem in hand slot=%d but lastTotemUse is null (updated=%d)",
                            playerName, slot, inventorySlot.getUpdated()
                    ));
                }
                return;
            }

            if ( lastTotemUseCompensated == null) {
                if (DEBUG) {
                    TGPlatform.getInstance().getLogger().warning(
                            "[TotemReplenished] Skipping replenishment for player " + playerName +
                                    ": lastTotemUseCompensated is null (lastTotemUse=" + lastTotemUse + ")"
                    );
                }
                return;
            }

            long replenishedAt = inventorySlot.getUpdated();

            long deltaRaw = replenishedAt - lastTotemUse;
            long deltaComp = replenishedAt - lastTotemUseCompensated;

            // Sanity logging: huge/negative values indicate mismatched time bases or stale timestamps
            if (DEBUG && (deltaComp < 0 || deltaComp > MAX_REASONABLE_DELAY_MS)) {
                TGPlatform.getInstance().getLogger().warning(String.format(
                        "[TotemReplenished] player=%s UNUSUAL deltaC=%dms (delta=%dms) slot=%d intervalsSize=%d",
                        playerName, deltaComp, deltaRaw, slot,
                        player.getTotemData().getIntervals().size()
                ));

                return;
            }

            //TGPlatform.getInstance().getLogger().info("[TotemReplenished] player=" + playerName + " deltaC=" + deltaComp + "ms");
            player.getTotemData().getIntervals().add(deltaComp);

            TotemReplenishedEvent replenishedEvent = new TotemReplenishedEvent(
                    player,
                    lastTotemUseCompensated,
                    replenishedAt
            );
            TGPlatform.getInstance().getEventRepository().post(replenishedEvent);

            player.setLastTotemUseCompensated(null);
            player.setLastTotemUse(null);

            return;
        }
    }
}
