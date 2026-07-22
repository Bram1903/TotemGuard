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
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
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

    public static final double DUPLICATE_THRESHOLD_LEGACY = 0.03;
    public static final double DUPLICATE_THRESHOLD_MODERN = 0.0002;

    private final Map<Integer, Boolean> pendingTeleports = new LinkedHashMap<>();
    private final Deque<ExpectedRotation> pendingServerRotationSyncs = new ArrayDeque<>();
    private Location current = emptyLocation();
    private Location previous = emptyLocation();
    private boolean lastFlyingPositionChanged;
    private boolean lastFlyingCarriedPosition;
    private boolean lastFlyingRotationChanged;
    private boolean lastFlyingWasResync;
    private boolean lastFlyingWasTeleportResync;
    private boolean lastFlyingTeleportVelocityReset;
    private boolean lastFlyingWasDuplicate;
    private Vector3d lastRealFlyingPosition;
    private boolean lastServerPositionChanged;
    private boolean lastServerRotationChanged;
    private boolean onGround;
    private boolean horizontalCollision;
    private boolean pendingTeleportResync;
    private boolean pendingTeleportVelocityReset;
    private boolean cameraIsSelf = true;
    private boolean pendingCameraResync;
    private boolean pendingVehicleSwitchResync;

    public void handleFlying(WrapperPlayClientPlayerFlying packet, boolean teleportResponse,
                             boolean inVehicle, ClientVersion clientVersion) {
        boolean previousOnGround = onGround;
        lastFlyingWasDuplicate = isMojangDuplicate(packet, teleportResponse, inVehicle, clientVersion, previousOnGround);
        lastFlyingCarriedPosition = packet.hasPositionChanged();

        previous = copy(current);

        Location packetLocation = packet.getLocation();
        double x = packet.hasPositionChanged() ? packetLocation.getX() : previous.getX();
        double y = packet.hasPositionChanged() ? packetLocation.getY() : previous.getY();
        double z = packet.hasPositionChanged() ? packetLocation.getZ() : previous.getZ();
        float yaw = packet.hasRotationChanged() ? normalizeRotation(packetLocation.getYaw()) : previous.getYaw();
        float pitch = packet.hasRotationChanged() ? normalizeRotation(packetLocation.getPitch()) : previous.getPitch();

        boolean teleportResync = pendingTeleportResync;
        pendingTeleportResync = false;
        lastFlyingWasTeleportResync = teleportResync;
        lastFlyingTeleportVelocityReset = teleportResync && pendingTeleportVelocityReset;
        if (teleportResync) pendingTeleportVelocityReset = false;
        boolean serverRotationResync = isServerRotationResync(packet, yaw, pitch);
        boolean cameraResync = !cameraIsSelf || pendingCameraResync;
        pendingCameraResync = false;
        boolean vehicleSwitchResync = pendingVehicleSwitchResync;
        pendingVehicleSwitchResync = false;
        current = new Location(new Vector3d(x, y, z), yaw, pitch);

        boolean positionDifferent = hasPositionChanged(previous, current);
        boolean rotationDifferent = hasRotationChanged(previous, current);

        lastFlyingWasResync = teleportResync || serverRotationResync || cameraResync || vehicleSwitchResync;
        if (lastFlyingWasResync) {
            lastFlyingPositionChanged = false;
            lastFlyingRotationChanged = false;
            if ((teleportResync || serverRotationResync) && packet.hasRotationChanged()) {
                previous = new Location(
                        new Vector3d(previous.getX(), previous.getY(), previous.getZ()), yaw, pitch);
            }
        } else {
            lastFlyingPositionChanged = packet.hasPositionChanged() && positionDifferent;
            lastFlyingRotationChanged = packet.hasRotationChanged() && rotationDifferent;
        }

        onGround = packet.isOnGround();
        horizontalCollision = packet.isHorizontalCollision();

        if (!lastFlyingWasDuplicate && packet.hasPositionChanged()) {
            lastRealFlyingPosition = new Vector3d(x, y, z);
        }
    }

    private boolean isMojangDuplicate(WrapperPlayClientPlayerFlying packet, boolean teleportResponse,
                                      boolean inVehicle, ClientVersion clientVersion, boolean previousOnGround) {
        if (teleportResponse) return false;
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_21)) return false;
        if (!packet.hasPositionChanged() || !packet.hasRotationChanged()) return false;
        if (inVehicle) return true;
        if (clientVersion.isOlderThan(ClientVersion.V_1_17)) return false;
        if (packet.isOnGround() != previousOnGround) return false;
        if (lastRealFlyingPosition == null) return false;

        double threshold = clientVersion.isOlderThan(ClientVersion.V_1_18_2)
                ? DUPLICATE_THRESHOLD_LEGACY
                : DUPLICATE_THRESHOLD_MODERN;
        Vector3d position = packet.getLocation().getPosition();
        double dx = position.getX() - lastRealFlyingPosition.getX();
        double dy = position.getY() - lastRealFlyingPosition.getY();
        double dz = position.getZ() - lastRealFlyingPosition.getZ();
        return dx * dx + dy * dy + dz * dz < threshold * threshold;
    }

    public void trackTeleport(int teleportId, boolean velocityReset) {
        pendingTeleports.put(teleportId, velocityReset);
    }

    public boolean hasPendingVelocityPreservingTeleport() {
        return pendingTeleports.containsValue(Boolean.FALSE);
    }

    public void markVehicleSwitchResync() {
        pendingVehicleSwitchResync = true;
    }

    public void handleTeleportConfirm(TeleportData.TeleportConfirmResult confirmResult) {
        if (!confirmResult.valid()) {
            return;
        }

        for (int skippedTeleportId : confirmResult.skippedTeleportIds()) {
            pendingTeleports.remove(skippedTeleportId);
        }

        Boolean velocityReset = pendingTeleports.remove(confirmResult.teleportId());
        pendingTeleportResync = true;
        pendingTeleportVelocityReset = velocityReset == null || velocityReset;
    }

    public void handleServerSync(WrapperPlayServerPlayerPositionAndLook packet) {
        previous = copy(current);
        Location target = resolveTeleportLocation(packet, previous);
        current = new Location(
                new Vector3d(target.getX(), target.getY(), target.getZ()),
                previous.getYaw(),
                previous.getPitch()
        );
        lastServerPositionChanged = hasPositionChanged(previous, current);
        lastServerRotationChanged = hasRotationChanged(previous, target);
        queueServerRotationSync(target.getYaw(), target.getPitch());
    }

    public void handleServerSync(WrapperPlayServerPlayerRotation packet) {
        float yaw = packet.isRelativeYaw()
                ? normalizeRotation(current.getYaw() + packet.getYaw())
                : normalizeRotation(packet.getYaw());
        float pitch = packet.isRelativePitch()
                ? normalizeRotation(current.getPitch() + packet.getPitch())
                : normalizeRotation(packet.getPitch());

        lastServerPositionChanged = false;
        lastServerRotationChanged = Float.compare(current.getYaw(), yaw) != 0
                || Float.compare(current.getPitch(), pitch) != 0;
        queueServerRotationSync(yaw, pitch);
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
        lastFlyingCarriedPosition = false;
        lastFlyingRotationChanged = false;
        lastFlyingWasResync = false;
        lastFlyingWasTeleportResync = false;
        lastFlyingTeleportVelocityReset = false;
        lastFlyingWasDuplicate = false;
        lastRealFlyingPosition = null;
        lastServerPositionChanged = false;
        lastServerRotationChanged = false;
        onGround = false;
        horizontalCollision = false;
        pendingTeleportResync = false;
        pendingTeleportVelocityReset = false;
        pendingServerRotationSyncs.clear();
        cameraIsSelf = true;
        pendingCameraResync = false;
        pendingVehicleSwitchResync = false;
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

    private void queueServerRotationSync(float yaw, float pitch) {
        if (!lastServerRotationChanged) {
            return;
        }
        pendingServerRotationSyncs.addLast(new ExpectedRotation(yaw, pitch));
    }

    private record ExpectedRotation(float yaw, float pitch) {
    }
}
