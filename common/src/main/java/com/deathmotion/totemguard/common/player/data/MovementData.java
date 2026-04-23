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

package com.deathmotion.totemguard.common.player.data;

import com.deathmotion.totemguard.common.util.MathUtil;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;
import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public class MovementData {

    private final Set<Integer> pendingTeleports = new LinkedHashSet<>();
    private Location current = emptyLocation();
    private Location previous = emptyLocation();
    private boolean lastFlyingPositionChanged;
    private boolean lastFlyingRotationChanged;
    private boolean lastServerPositionChanged;
    private boolean lastServerRotationChanged;
    private boolean onGround;
    private boolean horizontalCollision;
    private boolean pendingTeleportResync;
    private boolean pendingServerRotationSync;
    private float pendingServerYaw;
    private float pendingServerPitch;

    public void handleFlying(WrapperPlayClientPlayerFlying packet) {
        previous = copy(current);

        Location packetLocation = packet.getLocation();
        double x = packet.hasPositionChanged() ? packetLocation.getX() : previous.getX();
        double y = packet.hasPositionChanged() ? packetLocation.getY() : previous.getY();
        double z = packet.hasPositionChanged() ? packetLocation.getZ() : previous.getZ();
        float yaw = packet.hasRotationChanged() ? normalizeRotation(packetLocation.getYaw()) : previous.getYaw();
        float pitch = packet.hasRotationChanged() ? normalizeRotation(packetLocation.getPitch()) : previous.getPitch();

        boolean teleportResync = pendingTeleportResync;
        pendingTeleportResync = false;
        boolean serverRotationResync = isServerRotationResync(packet, yaw, pitch);
        current = new Location(new Vector3d(x, y, z), yaw, pitch);

        boolean positionDifferent = hasPositionChanged(previous, current);
        boolean rotationDifferent = hasRotationChanged(previous, current);

        if (teleportResync || serverRotationResync) {
            lastFlyingPositionChanged = false;
            lastFlyingRotationChanged = false;
        } else {
            lastFlyingPositionChanged = packet.hasPositionChanged() && positionDifferent;
            lastFlyingRotationChanged = packet.hasRotationChanged() && rotationDifferent;
        }

        onGround = packet.isOnGround();
        horizontalCollision = packet.isHorizontalCollision();
    }

    public void trackTeleport(int teleportId) {
        pendingTeleports.add(teleportId);
    }

    public void handleTeleportConfirm(TeleportData.TeleportConfirmResult confirmResult) {
        if (!confirmResult.valid()) {
            return;
        }

        for (int skippedTeleportId : confirmResult.skippedTeleportIds()) {
            pendingTeleports.remove(skippedTeleportId);
        }

        pendingTeleports.remove(confirmResult.teleportId());
        pendingTeleportResync = true;
    }

    public void handleServerSync(WrapperPlayServerPlayerPositionAndLook packet) {
        previous = copy(current);
        current = resolveTeleportLocation(packet, previous);
        lastServerPositionChanged = hasPositionChanged(previous, current);
        lastServerRotationChanged = hasRotationChanged(previous, current);
        queueServerRotationSync();
    }

    public void handleServerSync(WrapperPlayServerPlayerRotation packet) {
        previous = copy(current);

        float yaw = packet.isRelativeYaw()
                ? normalizeRotation(previous.getYaw() + packet.getYaw())
                : normalizeRotation(packet.getYaw());
        float pitch = packet.isRelativePitch()
                ? normalizeRotation(previous.getPitch() + packet.getPitch())
                : normalizeRotation(packet.getPitch());

        current = new Location(new Vector3d(previous.getX(), previous.getY(), previous.getZ()), yaw, pitch);
        lastServerPositionChanged = false;
        lastServerRotationChanged = hasRotationChanged(previous, current);
        queueServerRotationSync();
    }

    public void reset() {
        current = emptyLocation();
        previous = emptyLocation();
        lastFlyingPositionChanged = false;
        lastFlyingRotationChanged = false;
        lastServerPositionChanged = false;
        lastServerRotationChanged = false;
        onGround = false;
        horizontalCollision = false;
        pendingTeleportResync = false;
        pendingServerRotationSync = false;
        pendingServerYaw = 0.0F;
        pendingServerPitch = 0.0F;
        pendingTeleports.clear();
    }

    private boolean hasPositionChanged(Location from, Location to) {
        return Double.compare(from.getX(), to.getX()) != 0
                || Double.compare(from.getY(), to.getY()) != 0
                || Double.compare(from.getZ(), to.getZ()) != 0;
    }

    private boolean hasRotationChanged(Location from, Location to) {
        return Float.compare(from.getYaw(), to.getYaw()) != 0
                || Float.compare(from.getPitch(), to.getPitch()) != 0;
    }

    private Location copy(Location location) {
        return new Location(
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch()
        );
    }

    private Location emptyLocation() {
        return new Location(Vector3d.zero(), 0.0F, 0.0F);
    }

    private float normalizeRotation(float rotation) {
        return (float) MathUtil.clamp180(rotation);
    }

    private Location resolveTeleportLocation(WrapperPlayServerPlayerPositionAndLook packet, Location base) {
        Vector3d packetPosition = packet.getPosition();
        double x = packet.isRelativeFlag(RelativeFlag.X) ? base.getX() + packetPosition.getX() : packetPosition.getX();
        double y = packet.isRelativeFlag(RelativeFlag.Y) ? base.getY() + packetPosition.getY() : packetPosition.getY();
        double z = packet.isRelativeFlag(RelativeFlag.Z) ? base.getZ() + packetPosition.getZ() : packetPosition.getZ();
        float yaw = packet.isRelativeFlag(RelativeFlag.YAW)
                ? normalizeRotation(base.getYaw() + packet.getYaw())
                : normalizeRotation(packet.getYaw());
        float pitch = packet.isRelativeFlag(RelativeFlag.PITCH)
                ? normalizeRotation(base.getPitch() + packet.getPitch())
                : normalizeRotation(packet.getPitch());
        return new Location(new Vector3d(x, y, z), yaw, pitch);
    }

    private boolean isServerRotationResync(WrapperPlayClientPlayerFlying packet, float yaw, float pitch) {
        if (!packet.hasRotationChanged()) {
            return false;
        }

        boolean matches = pendingServerRotationSync
                && Float.compare(yaw, pendingServerYaw) == 0
                && Float.compare(pitch, pendingServerPitch) == 0;

        pendingServerRotationSync = false;
        return matches;
    }

    private void queueServerRotationSync() {
        if (!lastServerRotationChanged) {
            return;
        }

        pendingServerRotationSync = true;
        pendingServerYaw = current.getYaw();
        pendingServerPitch = current.getPitch();
    }
}
