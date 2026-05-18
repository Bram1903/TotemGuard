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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.TotemData;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Translates inventory-change dispatches into totem-replenish dispatches by
 * tracking when a hand slot gains a totem of undying within the tracked
 * interval after the last activation.
 */
public final class TotemReplenishedDetector implements InventoryChangedChannel.Handler {

    private final TotemReplenishedChannel replenished;

    public TotemReplenishedDetector(@NotNull TotemReplenishedChannel replenished) {
        this.replenished = replenished;
    }

    @Override
    public void onInventoryChanged(@NotNull TGPlayer player,
                                   @Nullable CarriedItem updatedCarriedItem,
                                   @NotNull List<InventorySlot> changedSlots,
                                   @NotNull Issuer lastIssuer) {
        if (lastIssuer == Issuer.SERVER) return;

        Long lastTotemUse = player.getLastTotemUse();
        if (lastTotemUse == null) return;

        capturePickup(player, updatedCarriedItem);

        PacketInventory inventory = player.getInventory();
        Long replenishedAt = null;

        for (InventorySlot inventorySlot : changedSlots) {
            int slot = inventorySlot.getSlot();
            if (!inventory.isHandSlot(slot)) continue;

            boolean slotHadTotem = inventorySlot.getPrevious().item().getType() == ItemTypes.TOTEM_OF_UNDYING;
            boolean slotHasTotem = inventorySlot.getItem().getType() == ItemTypes.TOTEM_OF_UNDYING;
            if (slotHadTotem || !slotHasTotem) continue;

            long updated = inventorySlot.getUpdated();
            replenishedAt = (replenishedAt == null) ? updated : Math.min(replenishedAt, updated);
        }

        if (replenishedAt == null) return;

        long deltaRaw = replenishedAt - lastTotemUse;
        if (deltaRaw < 0 || deltaRaw > TotemData.MAX_TRACKED_INTERVAL_MS) {
            clearTotemState(player);
            return;
        }

        Long pickupAt = player.getLastTotemPickup();
        TotemData totemData = player.getTotemData();
        totemData.getIntervals().add(deltaRaw);

        if (pickupAt != null) {
            long reaction = pickupAt - lastTotemUse;
            long click = replenishedAt - pickupAt;
            if (reaction >= 0 && reaction <= TotemData.MAX_TRACKED_INTERVAL_MS) {
                totemData.getReactionIntervals().add(reaction);
            }
            if (click >= 0 && click <= TotemData.MAX_TRACKED_INTERVAL_MS) {
                totemData.getClickIntervals().add(click);
            }
        }

        replenished.fire(player, lastTotemUse, replenishedAt, pickupAt);

        clearTotemState(player);
    }

    private void capturePickup(TGPlayer player, @Nullable CarriedItem updatedCarriedItem) {
        if (player.getLastTotemPickup() != null) return;
        if (updatedCarriedItem == null) return;
        if (updatedCarriedItem.getPrevious().item().getType() == ItemTypes.TOTEM_OF_UNDYING) return;
        if (!player.getInventory().isCarryingTotem()) return;

        player.setLastTotemPickup(updatedCarriedItem.getTimestamp());
    }

    private void clearTotemState(TGPlayer player) {
        player.setLastTotemUse(null);
        player.setLastTotemPickup(null);
        player.getDebugOverlayManager().refresh();
    }
}
