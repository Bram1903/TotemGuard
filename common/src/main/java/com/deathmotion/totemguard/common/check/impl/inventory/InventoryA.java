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
import com.deathmotion.totemguard.common.player.data.InputData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(description = "Impossible action with open inventory", type = CheckType.INVENTORY)
public class InventoryA extends CheckImpl implements PacketCheck {

    public InventoryA(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!data.isOpenInventory()) return;
        if (data.isServerOpenedInventoryThisTick()) return;
        final var packetType = event.getPacketType();

        if (WrapperPlayClientPlayerFlying.isFlying(packetType) && data.getMovementData().isLastFlyingRotationChanged()) {
            if (data.getGameMode() == GameMode.SPECTATOR) return;
            failInventory("aim");
            return;
        }

        if (packetType == PacketType.Play.Client.PLAYER_INPUT && player.supportsEndTick()) {
            final InputData inputData = data.getInputData();
            // We love the auto jump setting
            if (!inputData.current().jumping() && inputData.previous().jumping()) return;

            failInventory("move");
            return;
        }

        if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            failInventory("place");
            return;
        }

        if (packetType == PacketType.Play.Client.USE_ITEM) {
            failInventory("use");
            return;
        }

        if (packetType == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            failInventory("change slot");
            return;
        }

        if (packetType == PacketType.Play.Client.PICK_ITEM) {
            failInventory("pick item");
            return;
        }

        if (packetType == PacketType.Play.Client.ATTACK) {
            failInventory("attack");
            return;
        }

        if (packetType == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity.InteractAction action = new WrapperPlayClientInteractEntity(event).getAction();
            failInventory(action == WrapperPlayClientInteractEntity.InteractAction.ATTACK ? "attack" : "interact");
            return;
        }

        if (packetType == PacketType.Play.Client.ENTITY_ACTION) {
            failInventory("entity action");
            return;
        }

        if (packetType == PacketType.Play.Client.PLAYER_DIGGING && new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.START_DIGGING) {
            failInventory("break");
        }
    }
}
