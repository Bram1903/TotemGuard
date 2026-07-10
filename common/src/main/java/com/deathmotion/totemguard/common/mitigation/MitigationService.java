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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;

public class MitigationService {

    private static final int PENDING_TIMEOUT_TICKS = 100;
    private static final int RESEND_INTERVAL = 10;
    private static final double PACKET_SETBACK_MAX = 7.0;
    private static final byte ROTATION_RELATIVE = RelativeFlag.YAW.or(RelativeFlag.PITCH).getMask();

    private final Data data;

    private boolean pendingSetback;
    private boolean pendingIsPacket;
    private int pendingTicks;
    private double pendingSetbackDy;
    private boolean setbackConfirmedThisTick;
    private int teleportId = -1;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

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
            return;
        }
        if (++pendingTicks > PENDING_TIMEOUT_TICKS) {
            pendingSetback = false;
            pendingTicks = 0;
        } else if (pendingIsPacket && pendingTicks % RESEND_INTERVAL == 0) {
            sendCorrection(lastTargetX, lastTargetY, lastTargetZ);
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

        Location current = data.getMovementData().getCurrent();
        double dx = target.getX() - current.getX();
        double dy = target.getY() - current.getY();
        double dz = target.getZ() - current.getZ();
        boolean packet = dx * dx + dy * dy + dz * dz <= PACKET_SETBACK_MAX * PACKET_SETBACK_MAX;

        if (TGPlatform.getInstance().getEventBus().getSetback().fire(
                data.getPlayer(), current.getX(), current.getY(), current.getZ(),
                target.getX(), target.getY(), target.getZ(), packet)) {
            return false;
        }

        pendingSetbackDy = dy;
        pendingIsPacket = packet;
        if (pendingIsPacket) {
            lastTargetX = target.getX();
            lastTargetY = target.getY();
            lastTargetZ = target.getZ();
            sendCorrection(target.getX(), target.getY(), target.getZ());
            platformPlayer.resetFallDistance();
        } else {
            platformPlayer.teleport(worldName, target.getX(), target.getY(), target.getZ(),
                    current.getYaw(), current.getPitch());
        }

        pendingSetback = true;
        pendingTicks = 0;
        return true;
    }

    public boolean bootRider(double x, double y, double z, boolean supportsEndTick) {
        if (!setback(new Vector3d(x, y, z))) return false;
        if (supportsEndTick) {
            data.getPlayer().getUser().receivePacket(
                    new WrapperPlayClientPlayerInput(false, false, false, false, false, true, false));
        } else {
            data.getPlayer().getUser().receivePacket(new WrapperPlayClientSteerVehicle(0.0f, 0.0f, (byte) 0x02));
        }
        return true;
    }

    private void sendCorrection(double x, double y, double z) {
        int id = teleportId;
        teleportId = teleportId <= Integer.MIN_VALUE + 1 ? -1 : teleportId - 1;
        data.getPlayer().getUser().sendPacket(new WrapperPlayServerPlayerPositionAndLook(
                x, y, z, 0.0F, 0.0F, ROTATION_RELATIVE, id, false));
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
        pendingIsPacket = false;
        pendingTicks = 0;
        pendingSetbackDy = 0.0;
        setbackConfirmedThisTick = false;
    }
}
