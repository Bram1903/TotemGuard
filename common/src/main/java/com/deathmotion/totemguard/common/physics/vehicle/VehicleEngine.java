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

package com.deathmotion.totemguard.common.physics.vehicle;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.area.OutwardResidual;
import com.deathmotion.totemguard.common.physics.medium.FlowSolver;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugContext;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugLevel;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.VehicleData;
import com.deathmotion.totemguard.common.util.ClientMath;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import lombok.Getter;

public final class VehicleEngine {

    private static final double VERTICAL_PAD = 0.02;
    private static final double HORIZONTAL_PAD = 0.05;
    private static final double MOUNT_PAD = 1.5;

    private final TGPlayer player;
    private final Data data;
    private final WorldMirror world;
    private final BoatModel boat = new BoatModel();
    private final MediumSample flowScratch = new MediumSample();

    private double vx, vy, vz;
    private boolean seeded;

    @Getter
    private VehicleVerdict verdict = VehicleVerdict.NONE;

    public VehicleEngine(TGPlayer player) {
        this.player = player;
        this.data = player.getData();
        this.world = player.getWorldMirror();
    }

    public void onVehicleMove(double x, double y, double z, float yaw, float pitch) {
        verdict = VehicleVerdict.NONE;
        VehicleData vehicle = data.getVehicleData();
        vehicle.handleMove(x, y, z, yaw, pitch);

        int vehicleId = data.getVehicleId();
        if (vehicleId < 0) {
            disengage();
            return;
        }
        TrackedEntity ridden = world.entities().resolve(vehicleId);
        if (ridden == null) {
            disengage();
            return;
        }
        EntityType type = ridden.type();
        if (!EntityRoles.clientAuthoritativeVehicle(type)) {
            disengage();
            return;
        }
        if (!EntityRoles.boat(type)) {
            seedFrom(vehicle);
            return;
        }
        if (!world.readiness().ready()) {
            seedFrom(vehicle);
            return;
        }

        if (!seeded || vehicle.isSeedFromMount()) {
            seedFrom(vehicle);
            return;
        }

        judgeBoat(vehicle);
    }

    public void reset() {
        disengage();
        boat.reset();
        verdict = VehicleVerdict.NONE;
    }

    private void judgeBoat(VehicleData vehicle) {
        double obsX = vehicle.deltaX();
        double obsY = vehicle.deltaY();
        double obsZ = vehicle.deltaZ();

        double half = BoatModel.BOAT_WIDTH / 2.0;
        double startX = vehicle.getPrevX(), startY = vehicle.getPrevY(), startZ = vehicle.getPrevZ();
        BlockReader reader = world.reader();
        boat.resolve(reader, startX - half, startY, startZ - half,
                startX + half, startY + BoatModel.BOAT_HEIGHT, startZ + half);

        double advancedVy = boat.advanceVertical(vy, startY);
        double ceiling = advancedVy + VERTICAL_PAD;
        double floor = advancedVy - VERTICAL_PAD;
        double ascent = Math.max(0.0, obsY - ceiling);
        double descent = Math.max(0.0, floor - obsY);

        double friction = boat.horizontalFriction(true);
        double centerX = vx * friction;
        double centerZ = vz * friction;
        applyFlowShift(reader, startX, startY, startZ, half);
        centerX += flowScratch.pushX();
        centerZ += flowScratch.pushZ();
        double radius = boat.controlAccel() + HORIZONTAL_PAD;
        double horizontal = OutwardResidual.excess(obsX, obsZ, centerX, centerZ, radius);

        vy = Math.min(Math.max(obsY, floor), ceiling);
        double deviation = OutwardResidual.deviation(obsX, obsZ, centerX, centerZ);
        if (deviation <= radius || deviation <= 0.0) {
            vx = obsX;
            vz = obsZ;
        } else {
            double s = radius / deviation;
            vx = OutwardResidual.collapseAxis(obsX, centerX, s);
            vz = OutwardResidual.collapseAxis(obsZ, centerZ, s);
        }

        if (descent > VERTICAL_PAD) {
            verdict = new VehicleVerdict(true, "boat-fly", obsY, floor, descent);
        } else if (ascent > VERTICAL_PAD) {
            verdict = new VehicleVerdict(true, "boat-ascend", obsY, ceiling, ascent);
        } else if (horizontal > HORIZONTAL_PAD) {
            verdict = new VehicleVerdict(true, "boat-speed", ClientMath.horizontalDistance(obsX, obsZ),
                    ClientMath.horizontalDistance(centerX, centerZ) + radius, horizontal);
        }

        emitTrace(obsX, obsY, obsZ, centerX, centerZ, radius, floor, ceiling);
    }

    private void emitTrace(double obsX, double obsY, double obsZ,
                           double centerX, double centerZ, double radius,
                           double floor, double ceiling) {
        ConfigView view = TGPlatform.getInstance().getConfigRepository().configView();
        PhysicsDebugLevel level = view.physicsDebugLevel();
        if (!level.recording() || !view.physicsDebugContexts().contains(PhysicsDebugContext.VEHICLE)) return;
        boolean breach = verdict.breach();
        if (level != PhysicsDebugLevel.TRACE && !breach) return;

        String outcome = breach ? verdict.label() + " over=" + fmt(verdict.excess()) : "ok";
        TGPlatform.getInstance().getLogger().info(
                "[PhysicsTrace] " + player.getUser().getName() + " VEHICLE"
                        + " obs=(" + fmt(obsX) + "," + fmt(obsY) + "," + fmt(obsZ) + ")"
                        + " c=(" + fmt(centerX) + "," + fmt(centerZ) + ")"
                        + " r=" + fmt(radius)
                        + " vy=[" + fmt(floor) + "," + fmt(ceiling) + "]"
                        + " -> " + outcome);
    }

    private static String fmt(double value) {
        return String.format("%.4f", value);
    }

    private void applyFlowShift(BlockReader reader, double startX, double startY, double startZ, double half) {
        flowScratch.reset();
        FlowSolver.solve(reader, flowScratch, false,
                startX - half, startY, startZ - half,
                startX + half, startY + BoatModel.BOAT_HEIGHT, startZ + half);
    }

    private void seedFrom(VehicleData vehicle) {
        seeded = true;
        vx = clamp(vehicle.deltaX(), MOUNT_PAD);
        vy = clamp(vehicle.deltaY(), MOUNT_PAD);
        vz = clamp(vehicle.deltaZ(), MOUNT_PAD);
    }

    private void disengage() {
        seeded = false;
        vx = vy = vz = 0.0;
    }

    private static double clamp(double value, double bound) {
        return Math.max(-bound, Math.min(bound, value));
    }
}
