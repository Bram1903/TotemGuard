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

import java.util.*;

@Getter
public class MovementData {

    private final Set<Integer> pendingTeleports = new LinkedHashSet<>();
    // One entry per outbound PlayerRotation (no teleport ID to bind to), drained as
    // matching client echoes arrive. Unbounded because the transaction watchdog kicks
    // unresponsive clients within 30s, so the queue can't grow without bound in practice.
    private final Deque<ExpectedRotation> pendingServerRotationSyncs = new ArrayDeque<>();
    private Location current = emptyLocation();
    private Location previous = emptyLocation();
    private boolean lastFlyingPositionChanged;
    private boolean lastFlyingRotationChanged;
    private boolean lastServerPositionChanged;
    private boolean lastServerRotationChanged;
    private boolean onGround;
    private boolean horizontalCollision;
    private boolean pendingTeleportResync;
    private boolean cameraIsSelf = true;
    private boolean pendingCameraResync;

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
        boolean cameraResync = !cameraIsSelf || pendingCameraResync;
        pendingCameraResync = false;
        current = new Location(new Vector3d(x, y, z), yaw, pitch);

        boolean positionDifferent = hasPositionChanged(previous, current);
        boolean rotationDifferent = hasRotationChanged(previous, current);

        if (teleportResync || serverRotationResync || cameraResync) {
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
        // Don't queue. The teleport ID round-trip (pendingTeleportResync, set on
        // TELEPORT_CONFIRM) is the precise binding for the echo of this packet.
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
        // Queue the expected echo. PlayerRotation packets carry no teleport ID, so we have
        // no round-trip to bind the response to. Match by FIFO of (yaw, pitch) instead.
        queueServerRotationSync();
    }

    public void handleCameraChange(boolean isSelf) {
        // While the camera targets another entity, the vanilla client suppresses flying packets;
        // when it returns to self, the next flying packet may carry a yaw/pitch that diverges from
        // our last record, so mask that single delta to avoid false rotation flags.
        if (this.cameraIsSelf != isSelf) {
            pendingCameraResync = true;
        }
        this.cameraIsSelf = isSelf;
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
        pendingServerRotationSyncs.clear();
        cameraIsSelf = true;
        pendingCameraResync = false;
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
        if (!packet.hasRotationChanged() || pendingServerRotationSyncs.isEmpty()) {
            return false;
        }

        Iterator<ExpectedRotation> it = pendingServerRotationSyncs.iterator();
        int skipBeforeMatch = 0;
        while (it.hasNext()) {
            ExpectedRotation entry = it.next();
            if (Float.compare(yaw, entry.yaw()) == 0 && Float.compare(pitch, entry.pitch()) == 0) {
                for (int i = 0; i <= skipBeforeMatch; i++) {
                    pendingServerRotationSyncs.pollFirst();
                }
                return true;
            }
            skipBeforeMatch++;
        }
        return false;
    }

    private void queueServerRotationSync() {
        if (!lastServerRotationChanged) {
            return;
        }
        pendingServerRotationSyncs.addLast(new ExpectedRotation(current.getYaw(), current.getPitch()));
    }

    private record ExpectedRotation(float yaw, float pitch) {
    }
}
