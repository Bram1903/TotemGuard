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

import com.deathmotion.totemguard.api.event.Event;
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.ExtendedCheck;
import com.deathmotion.totemguard.common.event.internal.impl.TotemActivatedEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

@CheckData(description = "Invalid click time difference")
public class AutoTotemA extends CheckImpl implements ExtendedCheck {

    private Long lastTotemActivatedTimestamp;
    private Long lastTotemClickTimestamp;

    public AutoTotemA(TGPlayer player) {
        super(player);
    }

    @Override
    public <T extends Event> void handleEvent(T event) {
        if (!(event instanceof TotemActivatedEvent totemActivatedEvent)) return;
        lastTotemActivatedTimestamp = totemActivatedEvent.getTimestamp();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        if (packet.getWindowId() != InventoryConstants.PLAYER_WINDOW_ID) return;

        PacketInventory inventory = player.getInventory();
        boolean isCarryingTotem = inventory.getCarriedItem().getType() == ItemTypes.TOTEM_OF_UNDYING;
        int slot = packet.getSlot();

        if (slot == InventoryConstants.SLOT_OFFHAND || slot == inventory.getMainHandSlot() && isCarryingTotem) {
            if (lastTotemClickTimestamp != null && lastTotemActivatedTimestamp != null) {
                handle();
            }
            return;
        }

        if (isCarryingTotem) {
            lastTotemClickTimestamp = System.currentTimeMillis();
        }
    }

    private void handle() {
        long now = System.currentTimeMillis();
        long timeSinceClick = Math.abs(now - lastTotemClickTimestamp);
        long timeSinceTotemUse = Math.abs(now - lastTotemActivatedTimestamp);

        if (timeSinceClick <= 75 && timeSinceTotemUse <= 1500) {
            if (buffer.increase(5) >= 10) {
                fail("click time: " + timeSinceClick + "ms, totemUseTimeDiff: " + timeSinceTotemUse + "ms");
            }
        } else {
            buffer.decrease();
        }

        this.lastTotemActivatedTimestamp = null;
    }
}
