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

package com.deathmotion.totemguard.common.check.impl.inventory;

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(description = "Impossible action with open inventory", type = CheckType.INVENTORY)
public class InventoryA extends CheckImpl implements PacketCheck {

    public InventoryA(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final var packetType = event.getPacketType();
        final boolean isWindowClick = packetType == PacketType.Play.Client.CLICK_WINDOW;

        if (player.getData().isSprinting() && isWindowClick) {
            fail("sprinting");
            return;
        }

        if (player.getData().isInput() && isWindowClick) {
            fail("move");
            return;
        }

        // From here we only run checks if the player has an open inventory
        if (!player.getData().isOpenInventory()) return;

        if (packetType == PacketType.Play.Client.PLAYER_INPUT && player.getData().isInput()) {
            failAndCloseInventory("move (post)");
            return;
        }

        if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            failAndCloseInventory("place");
            return;
        }

        if (packetType == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            failAndCloseInventory("change slot");
            return;
        }

        if (packetType == PacketType.Play.Client.INTERACT_ENTITY && new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            failAndCloseInventory("attack");
            return;
        }

        if (packetType == PacketType.Play.Client.PLAYER_DIGGING && new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.START_DIGGING) {
            failAndCloseInventory("break");
        }
    }

    private void failAndCloseInventory(String reason) {
        fail(reason);

        // Yes, I now this is stupid,
        // but clients like Wurst and Meteor just don't close the inventory after interacting with the inventory
        player.getData().setOpenInventory(false);
    }
}
