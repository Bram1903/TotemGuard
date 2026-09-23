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
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.InputData;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.screen.ClientScreen;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;

@RequiresTickEnd
@CheckData(description = "Moving during inventory interaction", type = CheckType.INVENTORY)
public class InventoryB extends CheckImpl implements PacketCheck {

    private final InputData inputData;
    private final ClientScreen screen;

    private int revisionAtTickEnd;
    private boolean pendingClose;
    private int pendingCloseServerTick;

    public InventoryB(TGPlayer player) {
        super(player);
        this.inputData = data.getInputData();
        this.screen = player.getScreen();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.CLIENT_TICK_END) {
            revisionAtTickEnd = screen.getRevision();
            evaluatePendingClose();
            return;
        }

        if (data.isInVehicle()) return;

        if (packetType == PacketType.Play.Client.CLICK_WINDOW) {
            int windowId = new WrapperPlayClientClickWindow(event).getWindowId();
            if (player.isModDetectionWindow(windowId)) return;
            if (!inputData.walkingAtTickEnd() || screenOpenedSinceTickEnd()) return;
            failInventory("click (move)");
        } else if (packetType == PacketType.Play.Client.CLOSE_WINDOW) {
            if (data.isInventoryMitigatedThisTick()) return;
            if (new WrapperPlayClientCloseWindow(event).getWindowId() != InventoryConstants.PLAYER_WINDOW_ID) return;
            if (!inputData.walkingAtTickEnd()) return;

            if (pendingClose && platform.getCurrentServerTick() != pendingCloseServerTick) {
                resolvePendingClose();
            }

            pendingClose = true;
            pendingCloseServerTick = platform.getCurrentServerTick();
        }
    }

    // A frame that runs no tick can click a new container before the key release is sent
    private boolean screenOpenedSinceTickEnd() {
        return screen.getRevision() != revisionAtTickEnd || screen.openPossiblyApplied();
    }

    private void evaluatePendingClose() {
        if (!pendingClose) return;
        if (platform.getCurrentServerTick() == pendingCloseServerTick) return;
        resolvePendingClose();
    }

    private void resolvePendingClose() {
        pendingClose = false;

        if (data.isInNetherPortal()) return;
        fail("close (move)");
    }
}
