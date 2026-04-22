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
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
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

        final PacketTypeCommon type = event.getPacketType();

        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            if (data.getMovementData().isLastFlyingRotationChanged() && data.getGameMode() != GameMode.SPECTATOR) {
                failInventory("aim");
            }
            return;
        }

        String reason = staticReason(type);
        if (reason != null) {
            failInventory(reason);
            return;
        }

        if (type == PacketType.Play.Client.PLAYER_INPUT && player.supportsEndTick()) {
            final InputData input = data.getInputData();
            // Auto-jump releases PLAYER_INPUT packets even when idle — ignore the jump release tick.
            if (input.current().jumping() || !input.previous().jumping()) {
                failInventory("move");
            }
            return;
        }

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity.InteractAction action = new WrapperPlayClientInteractEntity(event).getAction();
            failInventory(action == WrapperPlayClientInteractEntity.InteractAction.ATTACK ? "attack" : "interact");
            return;
        }

        if (type == PacketType.Play.Client.PLAYER_DIGGING && new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.START_DIGGING) {
            failInventory("break");
        }
    }

    private static String staticReason(PacketTypeCommon type) {
        if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return "place";
        if (type == PacketType.Play.Client.USE_ITEM) return "use";
        if (type == PacketType.Play.Client.HELD_ITEM_CHANGE) return "change slot";
        if (type == PacketType.Play.Client.PICK_ITEM) return "pick item";
        if (type == PacketType.Play.Client.ATTACK) return "attack";
        if (type == PacketType.Play.Client.ENTITY_ACTION) return "entity action";
        return null;
    }
}
