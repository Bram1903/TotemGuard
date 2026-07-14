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

package com.deathmotion.totemguard.common.check.impl.autototem;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.EventCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.enums.SlotAction;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CheckData(description = "Impossible click time difference", type = CheckType.AUTO_TOTEM)
public class AutoTotemA extends CheckImpl implements EventCheck {

    private static final long MAX_CLICK_DIFF_MS = 5L;
    private static final long MAX_USE_TO_PLACE_DIFF_MS = 1500L;

    private Long popTimestamp;
    private Long pickupTimestamp;

    public AutoTotemA(TGPlayer player) {
        super(player);
    }

    @Override
    public void onTotemActivated(long timestamp) {
        popTimestamp = timestamp;
        pickupTimestamp = null;
    }

    @Override
    public void onInventoryChanged(@Nullable CarriedItem updatedCarriedItem,
                                   @NotNull List<InventorySlot> changedSlots,
                                   @NotNull Issuer lastIssuer) {
        if (lastIssuer != Issuer.CLIENT) return;

        detectTotemPickedUp(updatedCarriedItem);
        detectTotemPlacedInHand(changedSlots);
    }

    private void detectTotemPickedUp(@Nullable CarriedItem carried) {
        if (carried == null) return;
        if (carried.getPrevious().item().getType() == ItemTypes.TOTEM_OF_UNDYING) return;
        if (!inventory.isCarryingTotem()) return;

        pickupTimestamp = carried.getTimestamp();
    }

    private void detectTotemPlacedInHand(@NotNull List<InventorySlot> changedSlots) {
        if (popTimestamp == null || pickupTimestamp == null) return;

        for (InventorySlot changedSlot : changedSlots) {
            if (changedSlot.getSlot() != InventoryConstants.SLOT_OFFHAND) continue;
            if (changedSlot.getSlotAction() != SlotAction.CLICK) continue;
            if (changedSlot.getPrevious().item().getType() == ItemTypes.TOTEM_OF_UNDYING) continue;
            if (changedSlot.getItem().getType() != ItemTypes.TOTEM_OF_UNDYING) continue;

            evaluate(changedSlot.getUpdated());
            return;
        }
    }

    private void evaluate(long placedAt) {
        long clickDiff = placedAt - pickupTimestamp;
        long useDiff = placedAt - popTimestamp;

        if (clickDiff >= 0 && clickDiff <= MAX_CLICK_DIFF_MS && useDiff >= 0 && useDiff <= MAX_USE_TO_PLACE_DIFF_MS) {
            fail("clickDiff={0}ms,useDiff={1}ms", clickDiff, useDiff);
        }

        popTimestamp = null;
        pickupTimestamp = null;
    }
}
