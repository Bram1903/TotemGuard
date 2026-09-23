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
import com.deathmotion.totemguard.common.player.inventory.screen.ClientScreen;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@RequiresTickEnd
@CheckData(description = "Attack in the tick a screen closed", type = CheckType.INVENTORY, experimental = true)
public class InventoryF extends CheckImpl implements PacketCheck {

    private final ClientScreen screen;

    private boolean closedItself;
    private boolean openAtTickEnd;

    public InventoryF(TGPlayer player) {
        super(player);
        this.screen = player.getScreen();
    }

    // Closing a screen sets missTime to 10000, so the first handleKeybinds after it cannot start an attack
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.CLIENT_TICK_END) {
            closedItself = false;
            openAtTickEnd = screen.certainlyOpen();
            return;
        }

        if (type == PacketType.Play.Client.CLOSE_WINDOW) {
            if (data.isInventoryMitigatedThisTick()) return;
            if (player.isModDetectionWindow(new WrapperPlayClientCloseWindow(event).getWindowId())) return;
            closedItself = true;
            return;
        }

        if (!attacks(type, event) || ticksMayBeMerged()) return;

        if (closedItself) {
            fail("own close");
        } else if (openAtTickEnd && screen.certainlyClosed()) {
            fail("server close");
        }
    }

    private static boolean attacks(PacketTypeCommon type, PacketReceiveEvent event) {
        if (type == PacketType.Play.Client.ATTACK) return true;
        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            return new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }
        if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            return switch (new WrapperPlayClientPlayerDigging(event).getAction()) {
                case START_DIGGING, CHANGE_DESTROY_DIRECTION, STAB -> true;
                default -> false;
            };
        }
        return false;
    }
}
