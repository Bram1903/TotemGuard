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

package com.deathmotion.totemguard.common.check.impl.autototem;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.api.event.Event;
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.EventCheck;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.event.internal.impl.TotemActivatedEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

@CheckData(description = "Impossible click time difference", type = CheckType.AUTO_TOTEM)
public class AutoTotemA extends CheckImpl implements EventCheck {

    private Long lastTotemActivatedTimestamp;
    private Long totemPickUpTimestamp;

    public AutoTotemA(TGPlayer player) {
        super(player);
    }

    @Override
    public <T extends Event> void handleEvent(T event) {
        if (event instanceof TotemActivatedEvent totemActivatedEvent) {
            onTotemActivated(totemActivatedEvent);
        } else if (event instanceof InventoryChangedEvent setItemEvent) {
            onInventoryChanged(setItemEvent);
        }
    }

    private void onTotemActivated(TotemActivatedEvent event) {
        lastTotemActivatedTimestamp = event.getTimestamp();
    }

    private void onInventoryChanged(InventoryChangedEvent event) {
        if (event.getLastIssuer() != Issuer.CLIENT) return;

        final CarriedItem carriedItem = event.getUpdatedCarriedItem();
        if (carriedItem == null) return;

        final int clickedSlot = carriedItem.getSlot();
        final long clickedTime = carriedItem.getTimestamp();
        final boolean wasCarryingTotem = carriedItem.getPrevious().item().getType() == ItemTypes.TOTEM_OF_UNDYING;

        if (!inventory.isCarryingTotem() && wasCarryingTotem) {
            if (inventory.isHandSlot(clickedSlot) && inventory.isTotemInSlot(clickedSlot) && totemPickUpTimestamp != null && lastTotemActivatedTimestamp != null) {
                evaluate(clickedTime);
            }
        } else if (inventory.isCarryingTotem() && !wasCarryingTotem) {
            totemPickUpTimestamp = carriedItem.getTimestamp();
        }
    }

    private void evaluate(long now) {
        final long clickDiff = Math.abs(now - totemPickUpTimestamp);
        final long useDiff = Math.abs(now - lastTotemActivatedTimestamp);

        if (clickDiff <= 75 && useDiff <= 1500) {
            if (buffer.increase(5) >= 10) {
                //fail("click time: " + clickDiff + "ms, totemUseTimeDiff: " + useDiff + "ms");
            }

            fail("click time: " + clickDiff + "ms, totemUseTimeDiff: " + useDiff + "ms");
        } else {
            buffer.decrease();
        }

        lastTotemActivatedTimestamp = null;
        totemPickUpTimestamp = null;
    }
}
