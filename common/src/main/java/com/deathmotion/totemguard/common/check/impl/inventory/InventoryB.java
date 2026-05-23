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

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.InputData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

@CheckData(description = "Moving during inventory interaction", type = CheckType.INVENTORY)
public class InventoryB extends CheckImpl implements PacketCheck {

    private final InputData inputData;

    public InventoryB(TGPlayer player) {
        super(player);
        this.inputData = player.getData().getInputData();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();
        if (data.isServerOpenedInventoryThisTick()) return;

        // Pose.SWIMMING (real swim or 1-block crawl) keeps the sprint flag set vanilla-side
        // until shouldStopSwimSprinting fires.
        boolean sprinting = data.isSprinting() && data.getPose() != EntityPose.SWIMMING;

        if (packetType == PacketType.Play.Client.CLICK_WINDOW) {
            if (sprinting) {
                failInventory("click (sprinting)");
            } else if (inputData.hasMovement(true)) {
                failInventory("click (move)");
            }
        } else if (packetType == PacketType.Play.Client.CLOSE_WINDOW) {
            if (data.isInventoryMitigatedThisTick()) return;

            if (sprinting) {
                fail("close (sprinting)");
            } else if (inputData.hasMovement(true)) {
                fail("close (move)");
            }
        }
    }
}
