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

package com.deathmotion.totemguard.common.mitigation;

import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;

public class MitigationService {

    private static final int PENDING_TIMEOUT_TICKS = 100;

    private final Data data;

    private boolean pendingSetback;
    private int pendingTicks;
    private double pendingSetbackDy;
    private boolean setbackConfirmedThisTick;

    public MitigationService(Data data) {
        this.data = data;
    }

    public void onFlying() {
        setbackConfirmedThisTick = false;
        if (!pendingSetback) return;
        MovementData movement = data.getMovementData();
        if (movement.isLastFlyingWasTeleportResync()) {
            pendingSetback = false;
            pendingTicks = 0;
            setbackConfirmedThisTick = true;
        } else if (++pendingTicks > PENDING_TIMEOUT_TICKS) {
            pendingSetback = false;
            pendingTicks = 0;
        }
    }

    public boolean setbackPending() {
        return pendingSetback;
    }

    public boolean setbackConfirmedThisTick() {
        return setbackConfirmedThisTick;
    }

    public double pendingSetbackDy() {
        return pendingSetbackDy;
    }

    public boolean setbackIssuable() {
        PlatformPlayer platformPlayer = data.getPlayer().getPlatformPlayer();
        return platformPlayer != null && platformPlayer.getWorldName() != null;
    }

    public boolean setback(Vector3d target) {
        if (pendingSetback) return false;
        PlatformPlayer platformPlayer = data.getPlayer().getPlatformPlayer();
        if (platformPlayer == null) return false;
        String worldName = platformPlayer.getWorldName();
        if (worldName == null) return false;

        pendingSetback = true;
        pendingTicks = 0;
        Location current = data.getMovementData().getCurrent();
        pendingSetbackDy = target.getY() - current.getY();
        platformPlayer.teleport(worldName, target.getX(), target.getY(), target.getZ(),
                current.getYaw(), current.getPitch());
        return true;
    }

    public boolean closeInventory() {
        if (!data.isOpenInventory()) return false;
        if (data.isInventoryMitigated()) return false;
        data.setInventoryMitigated(true);
        data.getPlayer().getUser().sendPacket(InventoryConstants.SERVER_CLOSE_WINDOW);
        return true;
    }

    public boolean dealFallDamage(double amount) {
        if (amount <= 0.0) return false;
        PlatformPlayer platformPlayer = data.getPlayer().getPlatformPlayer();
        if (platformPlayer == null) return false;
        return platformPlayer.dealFallDamage(amount);
    }

    public void reset() {
        pendingSetback = false;
        pendingTicks = 0;
        pendingSetbackDy = 0.0;
        setbackConfirmedThisTick = false;
    }
}
