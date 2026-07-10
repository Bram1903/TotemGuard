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
import com.deathmotion.totemguard.common.physics.collision.AxisClip;
import com.deathmotion.totemguard.common.physics.collision.ColliderBuffer;
import com.deathmotion.totemguard.common.physics.collision.ColliderCollector;
import com.deathmotion.totemguard.common.physics.collision.ExemptCells;
import com.deathmotion.totemguard.common.physics.medium.FlowSolver;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugContext;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugLevel;
import com.deathmotion.totemguard.common.physics.trace.VehicleTraceLog;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.VehicleData;
import com.deathmotion.totemguard.common.util.ClientMath;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import lombok.Getter;

public final class VehicleEngine {

    private static final double VERTICAL_PAD = 0.02;
    private static final double HORIZONTAL_PAD = 0.05;
    private static final double MOUNT_PAD = 1.5;
    private static final double HARVEST_MARGIN = 0.5;
    private static final double CLIP_EPS = 1.0E-7;
    private static final double NON_LIVING_BOUNCE = 0.8;

    private static final double GROUND_PROBE = 1.0E-3;
    private static final double BOX_DEFLATE = 0.001;
    private static final long REENTRY_BLOCK_MS = 1000;
    private static final int FLUID_WATER = 1;
    private static final int FLUID_LAVA = 2;

    private static final double DEFAULT_GROUND_SLIP = 0.6;
    private static final double LIVING_GRAVITY_DEFAULT = 0.08;
    private static final double LIVING_VERTICAL_DRAG = 0.98;
    private static final double LIVING_AIR_DRAG = 0.91;
    private static final double LIVING_GROUND_ACCEL = 0.21600002;
    private static final double LIVING_AIR_ACCEL_FACTOR = 0.1;
    private static final double LIVING_WATER_ACCEL = 0.02;
    private static final double LIVING_WATER_DRAG = 0.8;
    private static final double GRAVITY_DIVISOR = 16.0;
    private static final double RIDDEN_FLOAT_RISE = 0.04;
    private static final double HORSE_JUMP_LEAP = 0.4;
    private static final double CAMEL_SPRINT_BONUS = 0.1;
    private static final double CAMEL_DASH_HORIZONTAL = 22.2222;
    private static final double CAMEL_DASH_VERTICAL = 1.4285;
    private static final double PIG_SPEED_FACTOR = 0.225;
    private static final double STRIDER_SPEED_FACTOR = 0.55;
    private static final double BOOST_FACTOR_MAX = 2.15;
    private static final double STEP_HEIGHT_HORSE = 1.0;
    private static final double STEP_HEIGHT_CAMEL = 1.5;
    private static final double STRIDER_LAVA_DRAG = 0.5;
    private static final double STRIDER_LAVA_RISE = 0.05;

    private static final double GHAST_FLYING_SPEED_DEFAULT = 0.05;
    private static final double GHAST_ACCEL_SCALE = 3.9 * (5.0 / 3.0);
    private static final double GHAST_INPUT_UP = 1.5;
    private static final double GHAST_INPUT_DOWN = 1.0;
    private static final double GHAST_INPUT_HORIZONTAL = Math.sqrt(0.98 * 0.98 + 1.0);
    private static final double GHAST_AIR_DRAG = 0.91;
    private static final double GHAST_WATER_DRAG = 0.8;
    private static final double GHAST_LAVA_DRAG = 0.5;

    private final TGPlayer player;
    private final Data data;
    private final WorldMirror world;
    private final BoatModel boat = new BoatModel();
    private final MediumSample flowScratch = new MediumSample();
    private final ColliderBuffer colliders = new ColliderBuffer();
    private final VehicleTraceLog traceLog = new VehicleTraceLog();

    private double vx, vz;
    private double vyLo, vyHi;
    private double lastDy;
    private boolean prevGrounded;
    private double prevSlipMin = DEFAULT_GROUND_SLIP;
    private double prevSlipMax = DEFAULT_GROUND_SLIP;
    private double supportSlipMin, supportSlipMax;
    private boolean seeded;

    private double safeX, safeY, safeZ;
    private boolean safeKnown;

    private int reentryVehicleId = -1;
    private long reentryUntilMs;
    private double reentryX, reentryY, reentryZ;

    private double segClosestX, segClosestZ;

    @Getter
    private VehicleVerdict verdict = VehicleVerdict.NONE;

    public VehicleEngine(TGPlayer player) {
        this.player = player;
        this.data = player.getData();
        this.world = player.getWorldMirror();
    }

    public boolean onVehicleMove(double x, double y, double z, float yaw, float pitch) {
        verdict = VehicleVerdict.NONE;
        VehicleData vehicle = data.getVehicleData();
        int vehicleId = data.getVehicleId();

        if (data.getMitigationService().setbackPending()) return false;

        if (vehicleId >= 0 && vehicleId == reentryVehicleId
                && System.currentTimeMillis() < reentryUntilMs
                && bootRider(reentryX, reentryY, reentryZ)) {
            return false;
        }

        boolean seedRequested = vehicle.isSeedFromMount();
        vehicle.handleMove(x, y, z, yaw, pitch);

        if (vehicleId < 0) {
            disengage();
            return false;
        }
        TrackedEntity ridden = world.entities().resolve(vehicleId);
        if (ridden == null) {
            disengage();
            return false;
        }
        EntityType type = ridden.type();
        if (!EntityRoles.clientAuthoritativeVehicle(type)) {
            disengage();
            return false;
        }
        if (!world.readiness().ready() || !seeded || seedRequested) {
            seedFrom(vehicle);
            vehicle.tickImpulse();
            return false;
        }

        if (EntityRoles.boat(type)) {
            judgeBoat(vehicle);
        } else if (EntityRoles.happyGhast(type)) {
            judgeGhast(vehicle, ridden);
        } else if (EntityRoles.horseFamily(type) || EntityRoles.steerableMob(type)) {
            judgeLiving(vehicle, ridden, type);
        } else {
            seedFrom(vehicle);
        }
        vehicle.tickImpulse();
        if (!verdict.breach()) {
            rememberSafe(vehicle);
        }
        return false;
    }

    public boolean requestSetback() {
        if (!seeded || !safeKnown) return false;
        boolean issued = bootRider(safeX, safeY, safeZ);
        if (issued) {
            reentryVehicleId = data.getVehicleId();
            reentryUntilMs = System.currentTimeMillis() + REENTRY_BLOCK_MS;
            reentryX = safeX;
            reentryY = safeY;
            reentryZ = safeZ;
        }
        return issued;
    }

    private boolean bootRider(double x, double y, double z) {
        if (!data.getMitigationService().setback(new Vector3d(x, y, z))) return false;
        if (player.supportsEndTick()) {
            player.getUser().receivePacket(
                    new WrapperPlayClientPlayerInput(false, false, false, false, false, true, false));
        } else {
            player.getUser().receivePacket(new WrapperPlayClientSteerVehicle(0.0f, 0.0f, (byte) 0x02));
        }
        return true;
    }

    public void reset() {
        disengage();
        verdict = VehicleVerdict.NONE;
    }

    private void judgeBoat(VehicleData vehicle) {
        double obsX = vehicle.deltaX();
        double obsY = vehicle.deltaY();
        double obsZ = vehicle.deltaZ();
        double preVx = vx, preVz = vz, preLo = vyLo, preHi = vyHi;

        double half = BoatModel.BOAT_WIDTH / 2.0;
        double startX = vehicle.getPrevX(), startY = vehicle.getPrevY(), startZ = vehicle.getPrevZ();
        double minX = startX - half, minZ = startZ - half;
        double maxX = startX + half, maxZ = startZ + half;
        double maxY = startY + BoatModel.BOAT_HEIGHT;
        BlockReader reader = world.reader();
        ShapeQuery query = new ShapeQuery(startY, false, false, false);

        boat.resolve(reader, query, minX, startY, minZ, maxX, maxY, maxZ);
        solveFlow(reader, minX, startY, minZ, maxX, maxY, maxZ);

        double advLo = boat.advanceVertical(vyLo + flowScratch.pushY(), startY);
        double advHi = boat.advanceVertical(vyHi + flowScratch.pushY(), startY);
        if (advHi < advLo) {
            double swap = advLo;
            advLo = advHi;
            advHi = swap;
        }

        harvest(reader, query, minX, startY, minZ, maxX, maxY, maxZ,
                obsX, obsZ, Math.min(advLo, obsY), Math.max(advHi, obsY));
        double clipLo = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, startY, minZ, maxX, maxY, maxZ, advLo, true);
        double clipHi = advHi == advLo ? clipLo
                : AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, startY, minZ, maxX, maxY, maxZ, advHi, true);
        double floor = Math.min(clipLo, clipHi) - VERTICAL_PAD;
        double ceiling = Math.max(clipLo, clipHi) + VERTICAL_PAD;
        double mainAscent = Math.max(0.0, obsY - ceiling);
        double mainDescent = Math.max(0.0, floor - obsY);
        double mainVertical = Math.max(mainAscent, mainDescent);

        double friction = boat.horizontalFriction();
        boolean modernTrig = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11);
        double dirX = ClientMath.lookX(vehicle.getYaw(), 0.0f, modernTrig);
        double dirZ = ClientMath.lookZ(vehicle.getYaw(), 0.0f, modernTrig);
        double centerX = (vx + flowScratch.pushX()) * friction;
        double centerZ = (vz + flowScratch.pushZ()) * friction;
        double mainHorizontal = controlResidual(obsX, obsZ, centerX, centerZ, dirX, dirZ,
                BoatModel.CONTROL_MIN, BoatModel.CONTROL_MAX);
        double mainSegX = segClosestX, mainSegZ = segClosestZ;

        boolean snapVertical = false;
        double snapDy = 0.0;
        double snapCenterX = 0.0, snapCenterZ = 0.0;
        double snapHorizontal = Double.MAX_VALUE;
        if (boat.snapEligible()) {
            double surface = boat.waterLevelAbove(reader, minX, minZ, maxX, maxY, maxZ,
                    Math.min(0.0, lastDy));
            snapDy = surface - BoatModel.BOAT_HEIGHT + BoatModel.SNAP_RISE - startY;
            snapVertical = Math.abs(obsY - snapDy) <= VERTICAL_PAD;
            if (snapVertical) {
                snapHorizontal = controlResidual(obsX, obsZ,
                        vx + flowScratch.pushX(), vz + flowScratch.pushZ(), dirX, dirZ,
                        BoatModel.CONTROL_MIN, BoatModel.CONTROL_MAX);
                snapCenterX = segClosestX;
                snapCenterZ = segClosestZ;
            }
        }

        double impulseAdv = 0.0, impulseClip = 0.0;
        double impulseAscent = 0.0, impulseDescent = 0.0;
        double impulseCenterX = 0.0, impulseCenterZ = 0.0;
        double impulseHorizontal = Double.MAX_VALUE;
        double impulseExcess = Double.MAX_VALUE;
        if (vehicle.impulseActive()) {
            impulseAdv = boat.advanceVertical(vehicle.getImpulseY() + flowScratch.pushY(), startY);
            impulseClip = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                    minX, startY, minZ, maxX, maxY, maxZ, impulseAdv, true);
            impulseAscent = Math.max(0.0, obsY - (impulseClip + VERTICAL_PAD));
            impulseDescent = Math.max(0.0, (impulseClip - VERTICAL_PAD) - obsY);
            impulseHorizontal = controlResidual(obsX, obsZ,
                    (vehicle.getImpulseX() + flowScratch.pushX()) * friction,
                    (vehicle.getImpulseZ() + flowScratch.pushZ()) * friction,
                    dirX, dirZ, BoatModel.CONTROL_MIN, BoatModel.CONTROL_MAX);
            impulseCenterX = segClosestX;
            impulseCenterZ = segClosestZ;
            impulseExcess = Math.max(Math.max(impulseAscent, impulseDescent), impulseHorizontal);
        }

        double mainExcess = Math.max(mainVertical, mainHorizontal);
        boolean useSnap = snapVertical && snapHorizontal <= Math.min(mainExcess, impulseExcess);
        boolean useImpulse = !useSnap && impulseExcess < mainExcess;

        double ascentExcess;
        double descentExcess;
        double horizontalExcess;
        double usedSegX;
        double usedSegZ;
        if (useSnap) {
            ascentExcess = 0.0;
            descentExcess = 0.0;
            horizontalExcess = snapHorizontal;
            vyLo = 0.0;
            vyHi = 0.0;
            lastDy = 0.0;
            usedSegX = snapCenterX;
            usedSegZ = snapCenterZ;
        } else if (useImpulse) {
            ascentExcess = impulseAscent;
            descentExcess = impulseDescent;
            horizontalExcess = impulseHorizontal;
            vehicle.consumeImpulse();
            settleBoatVertical(obsY, impulseAdv, impulseAdv, impulseClip,
                    impulseClip - VERTICAL_PAD, impulseClip + VERTICAL_PAD,
                    startY, minX, minZ, maxX, maxZ);
            usedSegX = impulseCenterX;
            usedSegZ = impulseCenterZ;
        } else {
            ascentExcess = mainAscent;
            descentExcess = mainDescent;
            horizontalExcess = mainHorizontal;
            settleBoatVertical(obsY, advLo, advHi, clipLo, floor, ceiling, startY, minX, minZ, maxX, maxZ);
            usedSegX = mainSegX;
            usedSegZ = mainSegZ;
        }
        collapseHorizontal(obsX, obsZ, usedSegX, usedSegZ, HORIZONTAL_PAD);

        emitVerdict("boat", obsY, floor, ceiling, ascentExcess, descentExcess,
                obsX, obsZ, usedSegX, usedSegZ, horizontalExcess);
        if (traceActive()) {
            String detail = " v0=(" + fmt(preVx) + "," + fmt(preVz) + "|" + fmt(preLo) + ".." + fmt(preHi) + ")"
                    + " f=" + fmt(friction)
                    + " adv=[" + fmt(advLo) + "," + fmt(advHi) + "]"
                    + " push=(" + fmt(flowScratch.pushX()) + "," + fmt(flowScratch.pushY())
                    + "," + fmt(flowScratch.pushZ()) + ")"
                    + " v1=(" + fmt(vx) + "," + fmt(vz) + "|" + fmt(vyLo) + ".." + fmt(vyHi) + ")"
                    + (useSnap ? " chose=snap" : useImpulse ? " chose=imp" : "");
            emitTrace("BOAT " + boat.status(), obsX, obsY, obsZ, mainSegX, mainSegZ,
                    HORIZONTAL_PAD, floor, ceiling, snapVertical, snapDy, detail);
        }
    }

    private void judgeLiving(VehicleData vehicle, TrackedEntity ridden, EntityType type) {
        double speed = ridden.movementSpeed();
        if (Double.isNaN(speed)) {
            seedFrom(vehicle);
            return;
        }
        boolean steerable = EntityRoles.steerableMob(type);
        boolean camel = EntityRoles.camel(type);
        boolean strider = type == EntityTypes.STRIDER;
        double preVx = vx, preVz = vz, preLo = vyLo, preHi = vyHi;

        double obsX = vehicle.deltaX();
        double obsY = vehicle.deltaY();
        double obsZ = vehicle.deltaZ();
        double startX = vehicle.getPrevX(), startY = vehicle.getPrevY(), startZ = vehicle.getPrevZ();
        double half = ridden.halfWidth();
        double minX = startX - half, minZ = startZ - half;
        double maxX = startX + half, maxZ = startZ + half;
        double maxY = startY + ridden.height();
        BlockReader reader = world.reader();
        ShapeQuery query = new ShapeQuery(startY, false, false, false);

        solveFlow(reader, minX, startY, minZ, maxX, maxY, maxZ);
        int fluids = fluidFlags(reader, minX, startY, minZ, maxX, maxY, maxZ);
        boolean water = (fluids & FLUID_WATER) != 0;
        boolean lava = (fluids & FLUID_LAVA) != 0;
        if (lava && !strider) {
            seedFrom(vehicle);
            return;
        }

        double gravity = Double.isNaN(ridden.gravity()) ? LIVING_GRAVITY_DEFAULT : ridden.gravity();
        double jumpStrength = ridden.jumpStrength();
        harvest(reader, query, minX, startY, minZ, maxX, maxY, maxZ,
                obsX, obsZ, Math.min(vyLo - gravity, obsY), Math.max(vyHi + STEP_HEIGHT_CAMEL, obsY));
        boolean grounded = groundedProbe(minX, startY, minZ, maxX, maxY, maxZ) || (strider && lava);
        boolean groundLatched = grounded || prevGrounded;

        boolean jumpTick = !water && grounded && !steerable && vehicle.hasJumpClaim();

        double pushX = flowScratch.pushX();
        double pushY = flowScratch.pushY();
        double pushZ = flowScratch.pushZ();

        double bandLo = vyLo + pushY;
        double clipLo = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, startY, minZ, maxX, maxY, maxZ, bandLo, true);
        boolean landing = bandLo < 0.0 && clipLo != bandLo;
        boolean groundContact = grounded || landing;

        double slipMin;
        double slipMax;
        if (groundContact) {
            resolveSupportSlip(startY, minX, minZ, maxX, maxZ);
            slipMin = supportSlipMin;
            slipMax = supportSlipMax;
        } else if (groundLatched) {
            slipMin = prevSlipMin;
            slipMax = prevSlipMax;
        } else {
            slipMin = 1.0;
            slipMax = 1.0;
        }
        double riddenSpeed = steerable
                ? speed * (strider ? STRIDER_SPEED_FACTOR : PIG_SPEED_FACTOR) * BOOST_FACTOR_MAX
                : speed + (camel && data.isSprinting() ? CAMEL_SPRINT_BONUS : 0.0);
        double accel = water ? LIVING_WATER_ACCEL
                : groundLatched ? riddenSpeed * (LIVING_GROUND_ACCEL / (slipMin * slipMin * slipMin))
                : riddenSpeed * LIVING_AIR_ACCEL_FACTOR;

        double jumpSet = 0.0;
        double leapRadius = 0.0;
        double dashVertical = 0.0;
        if (jumpTick) {
            vehicle.tickJumpClaimGrounded();
            if (!Double.isNaN(jumpStrength)) {
                double scale = vehicle.jumpClaimScale();
                if (camel) {
                    leapRadius = CAMEL_DASH_HORIZONTAL * scale * speed;
                    dashVertical = CAMEL_DASH_VERTICAL * scale * jumpStrength;
                } else {
                    leapRadius = HORSE_JUMP_LEAP * scale;
                    jumpSet = jumpStrength * scale;
                }
            }
        }

        double bandHi = vyHi + pushY + dashVertical;
        double clipHi = bandHi == bandLo ? clipLo
                : AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, startY, minZ, maxX, maxY, maxZ, bandHi, true);

        double extraCeiling = Double.NEGATIVE_INFINITY;
        if (jumpSet > 0.0) {
            double jumpClip = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                    minX, startY, minZ, maxX, maxY, maxZ, jumpSet, true);
            extraCeiling = jumpClip + VERTICAL_PAD;
        }
        if (groundContact) {
            double step = camel ? STEP_HEIGHT_CAMEL : STEP_HEIGHT_HORSE;
            extraCeiling = Math.max(extraCeiling, step + VERTICAL_PAD);
        }

        double floor = Math.min(clipLo, clipHi) - VERTICAL_PAD;
        double ceiling = Math.max(Math.max(clipLo, clipHi) + VERTICAL_PAD, extraCeiling);
        double mainAscent = Math.max(0.0, obsY - ceiling);
        double mainDescent = Math.max(0.0, floor - obsY);

        boolean modernTrig = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11);
        double dirX = ClientMath.lookX(vehicle.getYaw(), 0.0f, modernTrig);
        double dirZ = ClientMath.lookZ(vehicle.getYaw(), 0.0f, modernTrig);
        double radius = accel + leapRadius + HORIZONTAL_PAD;
        double centerX = vx + pushX;
        double centerZ = vz + pushZ;
        double mainHorizontal = livingResidual(steerable, obsX, obsZ, centerX, centerZ,
                dirX, dirZ, accel + leapRadius, radius);
        double mainSegX = segClosestX, mainSegZ = segClosestZ;
        double mainExcess = Math.max(Math.max(mainAscent, mainDescent), mainHorizontal);

        double impulseClipValue = 0.0;
        double impulseBandLo = 0.0;
        double impulseAscent = 0.0, impulseDescent = 0.0;
        double impulseHorizontal = Double.MAX_VALUE;
        double impulseExcess = Double.MAX_VALUE;
        double impulseSegX = 0.0, impulseSegZ = 0.0;
        if (vehicle.impulseActive()) {
            impulseBandLo = vehicle.getImpulseY() + pushY;
            impulseClipValue = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                    minX, startY, minZ, maxX, maxY, maxZ, impulseBandLo, true);
            double impulseCeiling = Math.max(impulseClipValue + VERTICAL_PAD, extraCeiling);
            impulseAscent = Math.max(0.0, obsY - impulseCeiling);
            impulseDescent = Math.max(0.0, (impulseClipValue - VERTICAL_PAD) - obsY);
            impulseHorizontal = livingResidual(steerable, obsX, obsZ,
                    vehicle.getImpulseX() + pushX, vehicle.getImpulseZ() + pushZ,
                    dirX, dirZ, accel + leapRadius, radius);
            impulseSegX = segClosestX;
            impulseSegZ = segClosestZ;
            impulseExcess = Math.max(Math.max(impulseAscent, impulseDescent), impulseHorizontal);
        }

        boolean impulseAvailable = vehicle.impulseActive();
        boolean useImpulse = impulseExcess < mainExcess;
        double ascentExcess = useImpulse ? impulseAscent : mainAscent;
        double descentExcess = useImpulse ? impulseDescent : mainDescent;
        double horizontalExcess = useImpulse ? impulseHorizontal : mainHorizontal;
        double chosenBandLo = useImpulse ? impulseBandLo : bandLo;
        double chosenClipLo = useImpulse ? impulseClipValue : clipLo;
        double chosenBandHi = useImpulse ? impulseBandLo : bandHi;
        double chosenFloor = useImpulse ? impulseClipValue - VERTICAL_PAD : floor;
        double chosenCeiling = useImpulse ? Math.max(impulseClipValue + VERTICAL_PAD, extraCeiling) : ceiling;
        if (useImpulse) vehicle.consumeImpulse();

        boolean landed = chosenClipLo != chosenBandLo && obsY <= chosenClipLo + CLIP_EPS;
        boolean canFloat = water && EntityRoles.floatsWhileRidden(type)
                && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11);
        if (landed) {
            boolean unclippedPossible = obsY >= chosenBandLo - CLIP_EPS && obsY <= chosenBandHi + CLIP_EPS;
            double bounce = supportBounce(startY + chosenClipLo, minX, minZ, maxX, maxZ);
            double restingCarry = -gravity * LIVING_VERTICAL_DRAG;
            vyLo = Math.min(unclippedPossible ? Math.min(obsY, 0.0) : 0.0, restingCarry);
            vyHi = bounce > 0.0 ? Math.max(0.0, -chosenBandLo * bounce) : 0.0;
        } else {
            double clamped = Math.min(Math.max(obsY, chosenFloor), chosenCeiling);
            if (water) {
                double advanced = clamped * LIVING_WATER_DRAG - gravity / GRAVITY_DIVISOR;
                vyLo = advanced;
                vyHi = advanced + (canFloat ? RIDDEN_FLOAT_RISE : 0.0);
            } else {
                double advanced = (clamped - gravity) * LIVING_VERTICAL_DRAG;
                boolean stepPossible = groundContact && obsY > Math.max(chosenClipLo, 0.0) + CLIP_EPS;
                if (stepPossible) {
                    vyLo = Math.min(advanced, 0.0);
                    vyHi = Math.max(advanced, 0.0);
                } else if (strider && lava) {
                    double floated = advanced * STRIDER_LAVA_DRAG + STRIDER_LAVA_RISE;
                    vyLo = Math.min(advanced, floated);
                    vyHi = Math.max(advanced, floated);
                } else {
                    vyLo = advanced;
                    vyHi = advanced;
                }
            }
        }
        lastDy = vyLo;
        collapseHorizontal(obsX, obsZ,
                useImpulse ? impulseSegX : mainSegX,
                useImpulse ? impulseSegZ : mainSegZ,
                steerable ? HORIZONTAL_PAD : radius);
        boolean groundDragApplied = grounded && !water;
        double drag = water ? LIVING_WATER_DRAG : grounded ? slipMax * LIVING_AIR_DRAG : LIVING_AIR_DRAG;
        vx *= drag;
        vz *= drag;

        prevGrounded = groundContact && !jumpTick;
        if (groundContact) {
            prevSlipMin = slipMin;
            prevSlipMax = slipMax;
        }

        String kind = camel ? "camel" : strider ? "strider" : type == EntityTypes.PIG ? "pig" : "horse";
        emitVerdict(kind, obsY, chosenFloor, chosenCeiling, ascentExcess, descentExcess,
                obsX, obsZ, useImpulse ? impulseSegX : mainSegX, useImpulse ? impulseSegZ : mainSegZ,
                horizontalExcess);
        if (traceActive()) {
            String detail = " v0=(" + fmt(preVx) + "," + fmt(preVz) + "|" + fmt(preLo) + ".." + fmt(preHi) + ")"
                    + " slip=" + fmt(slipMin) + (slipMin != slipMax ? ".." + fmt(slipMax) : "")
                    + " acc=" + fmt(accel) + " drag=" + (groundDragApplied ? "grd" : "air")
                    + (leapRadius > 0.0 ? " leap=" + fmt(leapRadius) : "")
                    + (jumpSet > 0.0 ? " jumpSet=" + fmt(jumpSet) : "")
                    + (dashVertical > 0.0 ? " dashV=" + fmt(dashVertical) : "")
                    + (vehicle.hasJumpClaim() ? " claim=" + fmt(vehicle.jumpClaimScale()) : "")
                    + " push=(" + fmt(pushX) + "," + fmt(pushY) + "," + fmt(pushZ) + ")"
                    + " extra=" + (extraCeiling == Double.NEGATIVE_INFINITY ? "-" : fmt(extraCeiling))
                    + " v1=(" + fmt(vx) + "," + fmt(vz) + "|" + fmt(vyLo) + ".." + fmt(vyHi) + ")"
                    + (impulseAvailable ? (useImpulse ? " imp=used" : " imp=avail") : "");
            emitTrace(kind.toUpperCase() + (grounded ? " GROUND" : " AIR")
                            + (water ? " WATER" : "") + (lava ? " LAVA" : ""),
                    obsX, obsY, obsZ, mainSegX, mainSegZ, radius, chosenFloor, chosenCeiling,
                    false, 0.0, detail);
        }
    }

    private void judgeGhast(VehicleData vehicle, TrackedEntity ridden) {
        double flyingSpeed = Double.isNaN(ridden.flyingSpeed())
                ? GHAST_FLYING_SPEED_DEFAULT
                : ridden.flyingSpeed();
        double accelScale = GHAST_ACCEL_SCALE * flyingSpeed * flyingSpeed;
        double preVx = vx, preVz = vz, preLo = vyLo, preHi = vyHi;

        double obsX = vehicle.deltaX();
        double obsY = vehicle.deltaY();
        double obsZ = vehicle.deltaZ();
        double startX = vehicle.getPrevX(), startY = vehicle.getPrevY(), startZ = vehicle.getPrevZ();
        double half = ridden.halfWidth();
        double minX = startX - half, minZ = startZ - half;
        double maxX = startX + half, maxZ = startZ + half;
        double maxY = startY + ridden.height();
        BlockReader reader = world.reader();
        ShapeQuery query = new ShapeQuery(startY, false, false, false);

        solveFlow(reader, minX, startY, minZ, maxX, maxY, maxZ);
        int fluids = fluidFlags(reader, minX, startY, minZ, maxX, maxY, maxZ);
        double drag = (fluids & FLUID_WATER) != 0 ? GHAST_WATER_DRAG
                : (fluids & FLUID_LAVA) != 0 ? GHAST_LAVA_DRAG
                : GHAST_AIR_DRAG;

        double up = accelScale * GHAST_INPUT_UP;
        double down = accelScale * GHAST_INPUT_DOWN;
        double bandLo = vyLo + flowScratch.pushY() - down;
        double bandHi = vyHi + flowScratch.pushY() + up;
        harvest(reader, query, minX, startY, minZ, maxX, maxY, maxZ,
                obsX, obsZ, Math.min(bandLo, obsY), Math.max(bandHi, obsY));
        double clipLo = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, startY, minZ, maxX, maxY, maxZ, bandLo, true);
        double clipHi = bandHi == bandLo ? clipLo
                : AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, startY, minZ, maxX, maxY, maxZ, bandHi, true);
        double floor = Math.min(clipLo, clipHi) - VERTICAL_PAD;
        double ceiling = Math.max(clipLo, clipHi) + VERTICAL_PAD;
        double mainAscent = Math.max(0.0, obsY - ceiling);
        double mainDescent = Math.max(0.0, floor - obsY);

        double radius = accelScale * GHAST_INPUT_HORIZONTAL + HORIZONTAL_PAD;
        double centerX = vx + flowScratch.pushX();
        double centerZ = vz + flowScratch.pushZ();
        double mainHorizontal = OutwardResidual.excess(obsX, obsZ, centerX, centerZ, radius);
        double mainExcess = Math.max(Math.max(mainAscent, mainDescent), mainHorizontal);

        double impulseAscent = 0.0, impulseDescent = 0.0;
        double impulseHorizontal = Double.MAX_VALUE;
        double impulseExcess = Double.MAX_VALUE;
        double impulseBandLo = 0.0, impulseClipValue = 0.0;
        double impulseCenterX = 0.0, impulseCenterZ = 0.0;
        if (vehicle.impulseActive()) {
            impulseBandLo = vehicle.getImpulseY() + flowScratch.pushY();
            impulseClipValue = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                    minX, startY, minZ, maxX, maxY, maxZ, impulseBandLo, true);
            impulseAscent = Math.max(0.0, obsY - (impulseClipValue + up + VERTICAL_PAD));
            impulseDescent = Math.max(0.0, (impulseClipValue - down - VERTICAL_PAD) - obsY);
            impulseCenterX = vehicle.getImpulseX() + flowScratch.pushX();
            impulseCenterZ = vehicle.getImpulseZ() + flowScratch.pushZ();
            impulseHorizontal = OutwardResidual.excess(obsX, obsZ, impulseCenterX, impulseCenterZ, radius);
            impulseExcess = Math.max(Math.max(impulseAscent, impulseDescent), impulseHorizontal);
        }

        boolean useImpulse = impulseExcess < mainExcess;
        double ascentExcess = useImpulse ? impulseAscent : mainAscent;
        double descentExcess = useImpulse ? impulseDescent : mainDescent;
        double horizontalExcess = useImpulse ? impulseHorizontal : mainHorizontal;
        double chosenFloor = useImpulse ? impulseClipValue - down - VERTICAL_PAD : floor;
        double chosenCeiling = useImpulse ? impulseClipValue + up + VERTICAL_PAD : ceiling;
        double chosenBandLo = useImpulse ? impulseBandLo : bandLo;
        double chosenClipLo = useImpulse ? impulseClipValue : clipLo;
        double chosenCenterX = useImpulse ? impulseCenterX : centerX;
        double chosenCenterZ = useImpulse ? impulseCenterZ : centerZ;
        if (useImpulse) vehicle.consumeImpulse();

        boolean landed = chosenClipLo != chosenBandLo && obsY <= chosenClipLo + CLIP_EPS;
        if (landed) {
            vyLo = 0.0;
            vyHi = 0.0;
        } else {
            double clamped = Math.min(Math.max(obsY, chosenFloor), chosenCeiling);
            vyLo = clamped * drag;
            vyHi = clamped * drag;
        }
        lastDy = vyLo;
        collapseHorizontal(obsX, obsZ, chosenCenterX, chosenCenterZ, radius);
        vx *= drag;
        vz *= drag;

        emitVerdict("ghast", obsY, chosenFloor, chosenCeiling, ascentExcess, descentExcess,
                obsX, obsZ, chosenCenterX, chosenCenterZ, horizontalExcess);
        if (traceActive()) {
            String detail = " v0=(" + fmt(preVx) + "," + fmt(preVz) + "|" + fmt(preLo) + ".." + fmt(preHi) + ")"
                    + " drag=" + fmt(drag) + " acc=" + fmt(accelScale)
                    + " push=(" + fmt(flowScratch.pushX()) + "," + fmt(flowScratch.pushY())
                    + "," + fmt(flowScratch.pushZ()) + ")"
                    + " v1=(" + fmt(vx) + "," + fmt(vz) + "|" + fmt(vyLo) + ".." + fmt(vyHi) + ")"
                    + (useImpulse ? " chose=imp" : "");
            emitTrace("GHAST", obsX, obsY, obsZ, centerX, centerZ, radius,
                    chosenFloor, chosenCeiling, false, 0.0, detail);
        }
    }

    private double livingResidual(boolean steerable, double obsX, double obsZ,
                                  double centerX, double centerZ, double dirX, double dirZ,
                                  double reach, double diskRadius) {
        if (steerable) {
            return controlResidual(obsX, obsZ, centerX, centerZ, dirX, dirZ, 0.0, reach);
        }
        segClosestX = centerX;
        segClosestZ = centerZ;
        return OutwardResidual.excess(obsX, obsZ, centerX, centerZ, diskRadius);
    }

    private void emitVerdict(String kind, double obsY, double floor, double ceiling,
                             double ascentExcess, double descentExcess,
                             double obsX, double obsZ, double segX, double segZ,
                             double horizontalExcess) {
        if (descentExcess > 0.0) {
            verdict = new VehicleVerdict(true, kind + "-fly", obsY, floor, descentExcess);
        } else if (ascentExcess > 0.0) {
            verdict = new VehicleVerdict(true, kind + "-ascend", obsY, ceiling, ascentExcess);
        } else if (horizontalExcess > 0.0) {
            verdict = new VehicleVerdict(true, kind + "-speed", ClientMath.horizontalDistance(obsX, obsZ),
                    ClientMath.horizontalDistance(segX, segZ) + HORIZONTAL_PAD, horizontalExcess);
        }
    }

    private void settleBoatVertical(double obsY, double advLo, double advHi, double clipLo,
                                    double floor, double ceiling,
                                    double startY, double minX, double minZ, double maxX, double maxZ) {
        boolean landed = clipLo != advLo && obsY <= clipLo + CLIP_EPS;
        if (landed) {
            boolean unclippedPossible = obsY >= advLo - CLIP_EPS && obsY <= advHi + CLIP_EPS;
            double bounce = supportBounce(startY + clipLo, minX, minZ, maxX, maxZ);
            vyLo = unclippedPossible ? Math.min(obsY, 0.0) : 0.0;
            vyHi = bounce > 0.0 ? Math.max(0.0, -advLo * bounce * NON_LIVING_BOUNCE) : 0.0;
            lastDy = 0.0;
        } else {
            double clamped = Math.min(Math.max(obsY, floor), ceiling);
            vyLo = clamped;
            vyHi = clamped;
            lastDy = clamped;
        }
    }

    private double controlResidual(double obsX, double obsZ,
                                   double centerX, double centerZ, double dirX, double dirZ,
                                   double reachMin, double reachMax) {
        double along = (obsX - centerX) * dirX + (obsZ - centerZ) * dirZ;
        along = Math.max(reachMin, Math.min(reachMax, along));
        segClosestX = centerX + dirX * along;
        segClosestZ = centerZ + dirZ * along;
        return Math.max(0.0, ClientMath.horizontalDistance(obsX - segClosestX, obsZ - segClosestZ)
                - HORIZONTAL_PAD);
    }

    private void rememberSafe(VehicleData vehicle) {
        safeX = vehicle.getCurX();
        safeY = vehicle.getCurY();
        safeZ = vehicle.getCurZ();
        safeKnown = true;
    }

    private void collapseHorizontal(double obsX, double obsZ,
                                    double centerX, double centerZ, double radius) {
        double deviation = OutwardResidual.deviation(obsX, obsZ, centerX, centerZ);
        if (deviation <= radius || deviation <= 0.0) {
            vx = obsX;
            vz = obsZ;
            return;
        }
        double s = radius / deviation;
        vx = OutwardResidual.collapseAxis(obsX, centerX, s);
        vz = OutwardResidual.collapseAxis(obsZ, centerZ, s);
    }

    private double supportBounce(double topY, double minX, double minZ, double maxX, double maxZ) {
        double bounce = 0.0;
        int count = colliders.count();
        for (int i = 0; i < count; i++) {
            long tag = colliders.tagOf(i);
            if (!ColliderBuffer.clipEligible(tag)) continue;
            if (Math.abs(colliders.maxY(i) - topY) > CLIP_EPS) continue;
            if (colliders.minX(i) >= maxX || colliders.maxX(i) <= minX) continue;
            if (colliders.minZ(i) >= maxZ || colliders.maxZ(i) <= minZ) continue;
            bounce = Math.max(bounce, StateFacts.bounceFactor(tag));
        }
        return bounce;
    }

    private boolean groundedProbe(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double clipped = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, minY, minZ, maxX, maxY, maxZ, -GROUND_PROBE, true);
        return clipped != -GROUND_PROBE;
    }

    private void resolveSupportSlip(double feetY, double minX, double minZ, double maxX, double maxZ) {
        double top = Double.NEGATIVE_INFINITY;
        int count = colliders.count();
        for (int i = 0; i < count; i++) {
            if (!ColliderBuffer.clipEligible(colliders.tagOf(i))) continue;
            if (colliders.minX(i) >= maxX || colliders.maxX(i) <= minX) continue;
            if (colliders.minZ(i) >= maxZ || colliders.maxZ(i) <= minZ) continue;
            double candidate = colliders.maxY(i);
            if (candidate > feetY + GROUND_PROBE) continue;
            if (candidate > top) top = candidate;
        }
        if (top == Double.NEGATIVE_INFINITY) {
            supportSlipMin = DEFAULT_GROUND_SLIP;
            supportSlipMax = DEFAULT_GROUND_SLIP;
            return;
        }
        double lo = Double.MAX_VALUE;
        double hi = 0.0;
        for (int i = 0; i < count; i++) {
            long tag = colliders.tagOf(i);
            if (!ColliderBuffer.clipEligible(tag)) continue;
            if (Math.abs(colliders.maxY(i) - top) > CLIP_EPS) continue;
            if (colliders.minX(i) >= maxX || colliders.maxX(i) <= minX) continue;
            if (colliders.minZ(i) >= maxZ || colliders.maxZ(i) <= minZ) continue;
            double s = StateFacts.slipperiness(tag);
            lo = Math.min(lo, s);
            hi = Math.max(hi, s);
        }
        if (lo == Double.MAX_VALUE) {
            supportSlipMin = DEFAULT_GROUND_SLIP;
            supportSlipMax = DEFAULT_GROUND_SLIP;
        } else {
            supportSlipMin = lo;
            supportSlipMax = hi;
        }
    }

    private int fluidFlags(BlockReader reader,
                           double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ) {
        double boxMinY = minY + BOX_DEFLATE;
        int x0 = floor(minX + BOX_DEFLATE), x1 = ceil(maxX - BOX_DEFLATE) - 1;
        int y0 = floor(boxMinY), y1 = ceil(maxY - BOX_DEFLATE) - 1;
        int z0 = floor(minZ + BOX_DEFLATE), z1 = ceil(maxZ - BOX_DEFLATE) - 1;
        int flags = 0;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    long facts = reader.facts(x, y, z);
                    if (!StateFacts.is(facts, StateFacts.ANY_FLUID)) continue;
                    boolean lava = StateFacts.is(facts, StateFacts.LAVA);
                    long fluidKind = lava ? StateFacts.LAVA : StateFacts.WATER;
                    double height = StateFacts.is(reader.facts(x, y + 1, z), fluidKind)
                            ? 1.0
                            : StateFacts.fluidAmount(facts) / 9.0F;
                    if (y + height >= boxMinY) {
                        flags |= lava ? FLUID_LAVA : FLUID_WATER;
                        if (flags == (FLUID_WATER | FLUID_LAVA)) return flags;
                    }
                }
            }
        }
        return flags;
    }

    private void harvest(BlockReader reader, ShapeQuery query,
                         double minX, double minY, double minZ,
                         double maxX, double maxY, double maxZ,
                         double obsX, double obsZ, double dyMin, double dyMax) {
        ColliderCollector.fill(colliders, reader, world.entities(), query, ExemptCells.NONE,
                data.getPistonData(),
                minX + Math.min(0.0, obsX) - HARVEST_MARGIN,
                minY + Math.min(0.0, dyMin) - HARVEST_MARGIN,
                minZ + Math.min(0.0, obsZ) - HARVEST_MARGIN,
                maxX + Math.max(0.0, obsX) + HARVEST_MARGIN,
                maxY + Math.max(0.0, dyMax) + HARVEST_MARGIN,
                maxZ + Math.max(0.0, obsZ) + HARVEST_MARGIN);
    }

    private void solveFlow(BlockReader reader,
                           double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ) {
        flowScratch.reset();
        boolean lavaFast = world.dimension().dimensionType() != null
                && world.dimension().dimensionType().isUltraWarm();
        boolean modern = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_1);
        FlowSolver.solve(reader, flowScratch, lavaFast, true, modern,
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    private boolean traceActive() {
        ConfigView view = TGPlatform.getInstance().getConfigRepository().configView();
        return view.physicsDebugLevel().recording()
                && view.physicsDebugContexts().contains(PhysicsDebugContext.VEHICLE);
    }

    private void emitTrace(String label, double obsX, double obsY, double obsZ,
                           double centerX, double centerZ, double radius,
                           double floor, double ceiling, boolean snapVertical, double snapDy,
                           String detail) {
        PhysicsDebugLevel level = TGPlatform.getInstance().getConfigRepository().configView().physicsDebugLevel();
        boolean breach = verdict.breach();

        String outcome = breach ? verdict.label() + " over=" + fmt(verdict.excess()) : "ok";
        String line = "VEHICLE " + label
                + " obs=(" + fmt(obsX) + "," + fmt(obsY) + "," + fmt(obsZ) + ")"
                + " c=(" + fmt(centerX) + "," + fmt(centerZ) + ")"
                + " r=" + fmt(radius)
                + " vy=[" + fmt(floor) + "," + fmt(ceiling) + "]"
                + (snapVertical ? " snap=" + fmt(snapDy) : "")
                + (detail == null ? "" : detail)
                + " -> " + outcome;

        traceLog.record(line);
        if (level == PhysicsDebugLevel.TRACE) {
            TGPlatform.getInstance().getLogger().info("[PhysicsTrace] " + player.getUser().getName() + " " + line);
        }
        if (breach) {
            traceLog.dump(player, verdict.label());
        }
    }

    private static String fmt(double value) {
        return String.format("%.4f", value);
    }

    private void seedFrom(VehicleData vehicle) {
        seeded = true;
        vx = clamp(vehicle.deltaX(), MOUNT_PAD);
        vz = clamp(vehicle.deltaZ(), MOUNT_PAD);
        double vy = clamp(vehicle.deltaY(), MOUNT_PAD);
        vyLo = vy;
        vyHi = vy;
        lastDy = vy;
        prevGrounded = false;
        rememberSafe(vehicle);
    }

    private void disengage() {
        seeded = false;
        vx = vz = 0.0;
        vyLo = vyHi = 0.0;
        lastDy = 0.0;
        prevGrounded = false;
        safeKnown = false;
        boat.reset();
        traceLog.clear();
    }

    private static double clamp(double value, double bound) {
        return Math.max(-bound, Math.min(bound, value));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }
}
