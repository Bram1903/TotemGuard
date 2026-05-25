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
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(description = "Moving during inventory interaction", type = CheckType.INVENTORY)
public class InventoryB extends CheckImpl implements PacketCheck {

    private final InputData inputData;

    private String pendingCloseReason;
    private int pendingCloseServerTick;

    public InventoryB(TGPlayer player) {
        super(player);
        this.inputData = player.getData().getInputData();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (isTickBoundary(packetType)) {
            evaluatePendingClose();
            return;
        }

        if (data.isServerOpenedInventoryThisTick()) return;
        if (data.isInVehicle()) return;

        boolean sprinting = data.isSprinting() && !data.isSwimming();

        if (packetType == PacketType.Play.Client.CLICK_WINDOW) {
            if (sprinting) {
                failInventory("click (sprinting)");
            } else if (inputData.hasMovement(true)) {
                failInventory("click (move)");
            }
        } else if (packetType == PacketType.Play.Client.CLOSE_WINDOW) {
            if (data.isInventoryMitigatedThisTick()) return;

            String reason = null;
            if (sprinting) {
                reason = "close (sprinting)";
            } else if (inputData.hasMovement(true)) {
                reason = "close (move)";
            }
            if (reason == null) return;

            if (pendingCloseReason != null && platform.getCurrentServerTick() != pendingCloseServerTick) {
                resolvePendingClose();
            }

            pendingCloseReason = reason;
            pendingCloseServerTick = platform.getCurrentServerTick();
        }
    }

    private boolean isTickBoundary(PacketTypeCommon packetType) {
        return player.supportsEndTick()
                ? packetType == PacketType.Play.Client.CLIENT_TICK_END
                : WrapperPlayClientPlayerFlying.isFlying(packetType);
    }

    private void evaluatePendingClose() {
        if (pendingCloseReason == null) return;
        if (platform.getCurrentServerTick() == pendingCloseServerTick) return;
        resolvePendingClose();
    }

    private void resolvePendingClose() {
        String reason = pendingCloseReason;
        pendingCloseReason = null;

        if (data.isInNetherPortal()) return;
        fail(reason);
    }
}
