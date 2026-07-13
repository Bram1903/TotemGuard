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

package com.deathmotion.totemguard.common.physics.simulation;

import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.EngineContext;
import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.physics.area.*;
import com.deathmotion.totemguard.common.physics.body.BoatBody;
import com.deathmotion.totemguard.common.physics.body.BodyKind;
import com.deathmotion.totemguard.common.physics.body.GhastBody;
import com.deathmotion.totemguard.common.physics.body.LivingVehicleBody;
import com.deathmotion.totemguard.common.physics.collision.*;
import com.deathmotion.totemguard.common.physics.control.*;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.ground.GroundResolver;
import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.medium.*;
import com.deathmotion.totemguard.common.physics.medium.model.*;
import com.deathmotion.totemguard.common.physics.mitigation.VehicleSetback;
import com.deathmotion.totemguard.common.physics.prescan.MountFilter;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import com.deathmotion.totemguard.common.physics.rules.BoatSnapRule;
import com.deathmotion.totemguard.common.physics.rules.BounceRule;
import com.deathmotion.totemguard.common.physics.rules.RiderClaimRule;
import com.deathmotion.totemguard.common.physics.trace.TraceRecording;
import com.deathmotion.totemguard.common.physics.verdict.*;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.VehicleData;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class VehicleSimulation {

    private static final double MOUNT_PAD = 1.5;
    private static final double HARVEST_MARGIN = 0.5;
    private static final double CLIP_EPS = 1.0E-7;
    private static final double NON_LIVING_BOUNCE = 0.8;

    private final EngineActor actor;
    private final Data data;
    private final WorldMirror world;
    private final BlockReader reader;
    private final EngineContext context;
    private final VersionGates gates;

    private final ColliderBuffer colliders = new ColliderBuffer();
    private final CollisionSweep sweep = new CollisionSweep();
    private final ContactReport contact = new ContactReport();
    private final MediumSample sample = new MediumSample();
    private final GroundResolver groundResolver = new GroundResolver();
    private final HypothesisSet hypotheses = new HypothesisSet();
    private final BandClip bandClip = new BandClip();
    private final MountFilter mounts = new MountFilter();
    private final VehicleSetback setback = new VehicleSetback();
    private final StuckFactor stuckFactor = new StuckFactor();
    private final LandModel land = new LandModel();

    private final LivingVehicleBody livingBody = new LivingVehicleBody();
    private final BoatBody boatBody = new BoatBody();
    private final GhastBody ghastBody = new GhastBody();

    private final TraceRecording trace;

    private MotionArea carried = MotionArea.rest();
    private double lastDy;
    private double preX, preZ, preFloor, preCeil;

    @Getter
    private PhysicsVerdict verdict = idle(BodyKind.HORSE);

    public VehicleSimulation(EngineActor actor, Data data, WorldMirror world,
                             EngineContext context, VersionGates gates, TraceRecording trace) {
        this.actor = actor;
        this.data = data;
        this.world = world;
        this.reader = world.reader();
        this.context = context;
        this.gates = gates;
        this.trace = trace;
    }

    private static PhysicsVerdict idle(BodyKind kind) {
        return new PhysicsVerdict(MotionStream.VEHICLE, kind,
                TickOutcome.DECLINED, DeclineReason.WITHHELD, null,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0,
                MediumKind.LAND, GroundState.AMBIGUOUS,
                false, false, false,
                MitigationOutcome.NONE, FallFinding.NONE);
    }

    private static double clamp(double value, double bound) {
        return Math.max(-bound, Math.min(bound, value));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public void onVehicleMove(double x, double y, double z, float yaw, float pitch) {
        ConfigView view = context.view();
        if (!view.physicsEngineEnabled()) {
            disengage();
            return;
        }
        PhysicsPreset preset = view.physicsPreset();
        reader.resetCounters();
        VehicleData vehicle = data.getVehicleData();
        int vehicleId = data.getVehicleId();

        if (data.getMitigationService().setbackPending()) {
            markBoostLag(vehicleId);
            return;
        }
        long now = System.currentTimeMillis();
        if (vehicleId >= 0 && mounts.reentryBlocked(vehicleId, now)
                && data.getMitigationService().bootRider(mounts.reentryX(), mounts.reentryY(), mounts.reentryZ(),
                gates.endTick())) {
            markBoostLag(vehicleId);
            return;
        }

        boolean seedRequested = mounts.needsSeed(vehicle);
        vehicle.handleMove(x, y, z, yaw, pitch);

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
        if (EntityRoles.steerableMob(type)) ridden.tickBoost();
        if (!world.readiness().ready() || seedRequested) {
            seedFrom(vehicle, BodyKind.HORSE);
            vehicle.tickImpulse();
            return;
        }
        if (!reader.columnLoaded(floor(vehicle.getCurX()) >> 4, floor(vehicle.getCurZ()) >> 4)) {
            seedFrom(vehicle, BodyKind.HORSE);
            vehicle.tickImpulse();
            return;
        }

        preX = carried.centerX();
        preZ = carried.centerZ();
        preFloor = carried.floorVy();
        preCeil = carried.ceilVy();
        BodyKind kind;
        if (EntityRoles.boat(type)) {
            boatBody.mount(ridden, type);
            kind = BodyKind.BOAT;
            judgeBoat(vehicle, preset);
        } else if (EntityRoles.happyGhast(type)) {
            ghastBody.mount(ridden, type);
            kind = BodyKind.GHAST;
            judgeGhast(vehicle, ridden, preset);
        } else if (EntityRoles.horseFamily(type) || EntityRoles.steerableMob(type)) {
            livingBody.mount(ridden, type);
            kind = livingBody.kind();
            judgeLiving(vehicle, ridden, type, preset);
        } else {
            seedFrom(vehicle, BodyKind.HORSE);
            vehicle.tickImpulse();
            return;
        }

        if (reader.missesThisTick() > 0) {
            verdict = idle(kind);
            seedFrom(vehicle, kind);
            vehicle.tickImpulse();
            return;
        }
        vehicle.tickImpulse();
        if (verdict.breach() == null) {
            setback.rememberSafe(vehicle.getCurX(), vehicle.getCurY(), vehicle.getCurZ());
        }
    }

    public boolean requestSetback() {
        return setback.requestSetback(data.getMitigationService(), mounts, data.getVehicleId(),
                gates.endTick(), System.currentTimeMillis());
    }

    public void reset() {
        disengage();
    }

    private void judgeLiving(VehicleData vehicle, TrackedEntity ridden, EntityType type, PhysicsPreset preset) {
        double speed = ridden.movementSpeed();
        if (Double.isNaN(speed)) {
            verdict = idle(livingBody.kind());
            seedFrom(vehicle, livingBody.kind());
            return;
        }
        double obsX = vehicle.deltaX();
        double obsY = vehicle.deltaY();
        double obsZ = vehicle.deltaZ();
        double startX = vehicle.getPrevX(), startY = vehicle.getPrevY(), startZ = vehicle.getPrevZ();
        double half = livingBody.halfWidth();
        double height = livingBody.height();
        ShapeQuery query = livingBody.shapeQuery(startY, false);

        double reachDown = Math.min(carried.floorVy() - HARVEST_MARGIN, obsY);
        double reachUp = Math.max(carried.ceilVy() + RiderControlResolver.STEP_HEIGHT_CAMEL, obsY);
        scan(query, startX, startY, startZ, half, height, obsX, obsY, obsZ, reachDown, reachUp,
                livingBody.stepHeight());
        if (reader.missesThisTick() > 0) return;

        boolean water = sample.water();
        boolean lava = sample.lava();
        boolean strider = type == EntityTypes.STRIDER;
        if (lava && !strider) {
            verdict = idle(livingBody.kind());
            seedFrom(vehicle, livingBody.kind());
            return;
        }

        GroundFacts ground = groundResolver.resolve(obsY, contact, sample.fluid(),
                livingBody.stepHeight(), carried.floorVy(), false, null);
        boolean grounded = ground.groundedStart() || (strider && lava);
        RiderControl control = RiderControlResolver.build(ridden, type, vehicle, data, gates,
                vehicle.getYaw(), grounded, water);
        MediumModel medium = water ? livingBody.water()
                : lava ? livingBody.lava()
                  : land;

        hypotheses.reset(carried);
        AreaBounds main = hypotheses.bounds(HypothesisSet.Slot.MAIN);
        AreaExpander.applyFluidPush(carried, sample, main);
        if (sample.stuck() && !sample.fluid()) stuckFactor.apply(main, sample);
        double accel = medium.accelBound(control, ground);
        if (control.steerable()) {
            main.controlSegment(control.lookX(), control.lookZ(), 0.0, accel + control.leapRadius());
        } else {
            main.expandRadius(accel);
            RiderClaimRule.expandLeap(main, control.leapRadius());
        }
        main.expandRadius(preset.vehicleHorizontalPad());

        double vPad = preset.vehicleVerticalPad();
        double bandLo = main.floor();
        double bandHi = main.ceiling() + control.dashVertical();
        bandClip.clipY(colliders, startX - half, startY, startZ - half,
                startX + half, startY + height, startZ + half, bandLo, bandHi);
        double clipLo = bandClip.floor();
        double clipHi = bandClip.ceiling();
        boolean landing = bandLo < 0.0 && clipLo != bandLo;
        boolean groundContact = grounded || landing;

        double extraCeiling = Double.NEGATIVE_INFINITY;
        if (control.jumpTakeoff() > 0.0) {
            double jumpClip = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                    startX - half, startY, startZ - half,
                    startX + half, startY + height, startZ + half, control.jumpTakeoff(), true);
            extraCeiling = jumpClip + vPad;
        }
        if (groundContact) {
            extraCeiling = Math.max(extraCeiling, control.stepHeight() + vPad);
        }
        main.floor(clipLo);
        main.addDescentSlack(vPad);
        main.enforceDescentFloor(true);
        main.ceiling(Math.max(clipHi + vPad, extraCeiling));

        double impulseBandLo = 0.0;
        double impulseClip = 0.0;
        if (vehicle.impulseActive()) {
            AreaBounds impulse = hypotheses.bounds(HypothesisSet.Slot.IMPULSE);
            impulse.reset(carried);
            impulse.centerX(vehicle.getImpulseX() + sample.pushX());
            impulse.centerZ(vehicle.getImpulseZ() + sample.pushZ());
            if (control.steerable()) {
                impulse.controlSegment(control.lookX(), control.lookZ(), 0.0, accel + control.leapRadius());
            } else {
                impulse.expandRadius(accel);
                RiderClaimRule.expandLeap(impulse, control.leapRadius());
            }
            impulse.expandRadius(preset.vehicleHorizontalPad());
            impulseBandLo = vehicle.getImpulseY() + sample.pushY();
            impulseClip = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                    startX - half, startY, startZ - half,
                    startX + half, startY + height, startZ + half, impulseBandLo, true);
            impulse.floor(impulseClip);
            impulse.addDescentSlack(vPad);
            impulse.enforceDescentFloor(true);
            impulse.ceiling(Math.max(impulseClip + vPad, extraCeiling));
            hypotheses.enable(HypothesisSet.Slot.IMPULSE);
        }

        HypothesisSet.Slot chosen = hypotheses.judge(obsX, obsY, obsZ, -1.0);
        if (chosen == HypothesisSet.Slot.IMPULSE) vehicle.consumeImpulse();
        AreaBounds chosenBounds = hypotheses.chosenBounds();
        JudgedExcess excess = hypotheses.chosenExcess();
        BoundBreach breach = classify(excess, preset);

        double chosenBandLo = chosen == HypothesisSet.Slot.IMPULSE ? impulseBandLo : bandLo;
        double chosenBandHi = chosen == HypothesisSet.Slot.IMPULSE ? impulseBandLo : bandHi;
        double chosenClipLo = chosen == HypothesisSet.Slot.IMPULSE ? impulseClip : clipLo;
        double gravity = control.gravity();
        boolean landed = chosenClipLo != chosenBandLo && obsY <= chosenClipLo + CLIP_EPS;
        double vyLo;
        double vyHi;
        if (landed) {
            boolean unclippedPossible = obsY >= chosenBandLo - CLIP_EPS && obsY <= chosenBandHi + CLIP_EPS;
            double restingCarry = -gravity * MotionDefaults.VERTICAL_DRAG;
            vyLo = Math.min(unclippedPossible ? Math.min(obsY, 0.0) : 0.0, restingCarry);
            vyHi = Math.max(0.0, BounceRule.reflect(gates.restitutionBounce(), contact,
                    chosenBandLo, gravity));
        } else {
            double clamped = Math.min(Math.max(obsY, chosenBounds.floor() - chosenBounds.descentSlack()),
                    chosenBounds.ceiling());
            if (water) {
                double advanced = livingBody.water().advanceVertical(clamped, control);
                vyLo = advanced;
                vyHi = advanced + RiderWaterModel.floatRise(control);
            } else {
                double advanced = land.advanceVertical(clamped, control);
                boolean stepPossible = groundContact && obsY > Math.max(chosenClipLo, 0.0) + CLIP_EPS;
                if (stepPossible) {
                    vyLo = Math.min(advanced, 0.0);
                    vyHi = Math.max(advanced, 0.0);
                } else if (strider && lava) {
                    double floated = StriderLavaModel.floated(advanced);
                    vyLo = Math.min(advanced, floated);
                    vyHi = Math.max(advanced, floated);
                } else {
                    vyLo = advanced;
                    vyHi = advanced;
                }
            }
        }
        lastDy = vyLo;
        AreaAdvancer.clampObserved(chosenBounds, obsX, obsY, obsZ, false, 0.0);
        double drag = water ? RiderControl.WATER_FRICTION : land.frictionMax(control, ground);
        carried = AreaAdvancer.zeroClamp(new MotionArea(chosenBounds.legalX() * drag,
                chosenBounds.legalZ() * drag, 0.0, vyLo, vyHi), false);

        verdict = judged(livingBody.kind(), medium.kind(), ground.start(),
                obsX, obsY, obsZ, excess, breach, chosenBounds);
        trace.record(context.view(), contact, sample, ground, control,
                chosenBounds, verdict, reader, 0.0, 0.0, preX, preZ, preFloor, preCeil);
    }

    private void judgeBoat(VehicleData vehicle, PhysicsPreset preset) {
        double obsX = vehicle.deltaX();
        double obsY = vehicle.deltaY();
        double obsZ = vehicle.deltaZ();
        double startX = vehicle.getPrevX(), startY = vehicle.getPrevY(), startZ = vehicle.getPrevZ();
        double half = boatBody.halfWidth();
        double height = boatBody.height();
        ShapeQuery query = boatBody.shapeQuery(startY, false);
        BoatFloatModel boat = boatBody.floatModel();

        boat.resolve(reader, query, startX - half, startY, startZ - half,
                startX + half, startY + height, startZ + half);
        double reachDown = Math.min(carried.floorVy() - HARVEST_MARGIN, obsY);
        double reachUp = Math.max(carried.ceilVy() + HARVEST_MARGIN, obsY);
        scan(query, startX, startY, startZ, half, height, obsX, obsY, obsZ, reachDown, reachUp, 0.0);
        if (reader.missesThisTick() > 0) return;

        BoatControl control = BoatControlResolver.build(gates, vehicle.getYaw());
        double friction = boat.horizontalFriction();
        double pushX = sample.pushX();
        double pushY = sample.pushY();
        double pushZ = sample.pushZ();
        double vPad = preset.vehicleVerticalPad();

        double advLo = boat.advanceVertical(carried.floorVy() + pushY, startY);
        double advHi = boat.advanceVertical(carried.ceilVy() + pushY, startY);
        if (advHi < advLo) {
            double swap = advLo;
            advLo = advHi;
            advHi = swap;
        }

        hypotheses.reset(carried);
        AreaBounds main = hypotheses.bounds(HypothesisSet.Slot.MAIN);
        main.centerX((carried.centerX() + pushX) * friction);
        main.centerZ((carried.centerZ() + pushZ) * friction);
        main.controlSegment(control.dirX(), control.dirZ(), control.reachMin(), control.reachMax());
        main.expandRadius(preset.vehicleHorizontalPad());
        bandClip.clipY(colliders, startX - half, startY, startZ - half,
                startX + half, startY + height, startZ + half, advLo, advHi);
        main.floor(bandClip.floor());
        main.addDescentSlack(vPad);
        main.enforceDescentFloor(true);
        main.ceiling(bandClip.ceiling() + vPad);
        double mainClipLo = bandClip.floor();

        double impulseAdv = 0.0;
        double impulseClip = 0.0;
        if (vehicle.impulseActive()) {
            AreaBounds impulse = hypotheses.bounds(HypothesisSet.Slot.IMPULSE);
            impulse.reset(carried);
            impulse.centerX((vehicle.getImpulseX() + pushX) * friction);
            impulse.centerZ((vehicle.getImpulseZ() + pushZ) * friction);
            impulse.controlSegment(control.dirX(), control.dirZ(), control.reachMin(), control.reachMax());
            impulse.expandRadius(preset.vehicleHorizontalPad());
            impulseAdv = boat.advanceVertical(vehicle.getImpulseY() + pushY, startY);
            impulseClip = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                    startX - half, startY, startZ - half,
                    startX + half, startY + height, startZ + half, impulseAdv, true);
            impulse.floor(impulseClip);
            impulse.addDescentSlack(vPad);
            impulse.enforceDescentFloor(true);
            impulse.ceiling(impulseClip + vPad);
            hypotheses.enable(HypothesisSet.Slot.IMPULSE);
        }

        double snapDy = 0.0;
        boolean snapOffered = false;
        if (BoatSnapRule.eligible(boat)) {
            snapDy = BoatSnapRule.snapDy(boat, reader, startX - half, startZ - half,
                    startX + half, startY + height, startZ + half, startY, lastDy);
            AreaBounds snap = hypotheses.bounds(HypothesisSet.Slot.SNAP);
            snap.reset(carried);
            snap.centerX(carried.centerX() + pushX);
            snap.centerZ(carried.centerZ() + pushZ);
            snap.controlSegment(control.dirX(), control.dirZ(), control.reachMin(), control.reachMax());
            snap.expandRadius(preset.vehicleHorizontalPad());
            snap.floor(snapDy);
            snap.addDescentSlack(vPad);
            snap.enforceDescentFloor(true);
            snap.ceiling(snapDy + vPad);
            hypotheses.enable(HypothesisSet.Slot.SNAP);
            snapOffered = true;
        }

        HypothesisSet.Slot chosen = hypotheses.judge(obsX, obsY, obsZ, -1.0);
        if (chosen == HypothesisSet.Slot.IMPULSE) vehicle.consumeImpulse();
        AreaBounds chosenBounds = hypotheses.chosenBounds();
        JudgedExcess excess = hypotheses.chosenExcess();
        BoundBreach breach = classify(excess, preset);

        double vyLo;
        double vyHi;
        if (snapOffered && chosen == HypothesisSet.Slot.SNAP) {
            vyLo = 0.0;
            vyHi = 0.0;
            lastDy = 0.0;
        } else {
            double chosenAdvLo = chosen == HypothesisSet.Slot.IMPULSE ? impulseAdv : advLo;
            double chosenAdvHi = chosen == HypothesisSet.Slot.IMPULSE ? impulseAdv : advHi;
            double chosenClipLo = chosen == HypothesisSet.Slot.IMPULSE ? impulseClip : mainClipLo;
            boolean landed = chosenClipLo != chosenAdvLo && obsY <= chosenClipLo + CLIP_EPS;
            if (landed) {
                boolean unclippedPossible = obsY >= chosenAdvLo - CLIP_EPS && obsY <= chosenAdvHi + CLIP_EPS;
                vyLo = unclippedPossible ? Math.min(obsY, 0.0) : 0.0;
                vyHi = Math.max(0.0, BounceRule.reflect(gates.restitutionBounce(), contact,
                        chosenAdvLo, BoatFloatModel.GRAVITY) * NON_LIVING_BOUNCE);
                lastDy = 0.0;
            } else {
                double clamped = Math.min(Math.max(obsY, chosenBounds.floor() - chosenBounds.descentSlack()),
                        chosenBounds.ceiling());
                vyLo = clamped;
                vyHi = clamped;
                lastDy = clamped;
            }
        }
        AreaAdvancer.clampObserved(chosenBounds, obsX, obsY, obsZ, false, 0.0);
        carried = AreaAdvancer.zeroClamp(new MotionArea(chosenBounds.legalX(),
                chosenBounds.legalZ(), 0.0, vyLo, vyHi), false);

        verdict = judged(BodyKind.BOAT, MediumKind.BOAT, GroundState.AMBIGUOUS,
                obsX, obsY, obsZ, excess, breach, chosenBounds);
        trace.record(context.view(), contact, sample, null, control,
                chosenBounds, verdict, reader, 0.0, 0.0, preX, preZ, preFloor, preCeil);
    }

    private void judgeGhast(VehicleData vehicle, TrackedEntity ridden, PhysicsPreset preset) {
        double obsX = vehicle.deltaX();
        double obsY = vehicle.deltaY();
        double obsZ = vehicle.deltaZ();
        double startX = vehicle.getPrevX(), startY = vehicle.getPrevY(), startZ = vehicle.getPrevZ();
        double half = ghastBody.halfWidth();
        double height = ghastBody.height();
        ShapeQuery query = ghastBody.shapeQuery(startY, false);

        GhastControl control = GhastControlResolver.build(ridden);
        double reachDown = Math.min(carried.floorVy() - control.down() - HARVEST_MARGIN, obsY);
        double reachUp = Math.max(carried.ceilVy() + control.up() + HARVEST_MARGIN, obsY);
        scan(query, startX, startY, startZ, half, height, obsX, obsY, obsZ, reachDown, reachUp, 0.0);
        if (reader.missesThisTick() > 0) return;

        FlyingModel flying = ghastBody.flying();
        flying.prepare(sample);
        double drag = flying.drag();
        double pushX = sample.pushX();
        double pushY = sample.pushY();
        double pushZ = sample.pushZ();
        double vPad = preset.vehicleVerticalPad();

        hypotheses.reset(carried);
        AreaBounds main = hypotheses.bounds(HypothesisSet.Slot.MAIN);
        main.centerX(carried.centerX() + pushX);
        main.centerZ(carried.centerZ() + pushZ);
        main.expandRadius(control.horizontalReach() + preset.vehicleHorizontalPad());
        double bandLo = carried.floorVy() + pushY - control.down();
        double bandHi = carried.ceilVy() + pushY + control.up();
        bandClip.clipY(colliders, startX - half, startY, startZ - half,
                startX + half, startY + height, startZ + half, bandLo, bandHi);
        main.floor(bandClip.floor());
        main.addDescentSlack(vPad);
        main.enforceDescentFloor(true);
        main.ceiling(bandClip.ceiling() + vPad);
        double mainClipLo = bandClip.floor();

        double impulseBandLo = 0.0;
        double impulseClip = 0.0;
        if (vehicle.impulseActive()) {
            AreaBounds impulse = hypotheses.bounds(HypothesisSet.Slot.IMPULSE);
            impulse.reset(carried);
            impulse.centerX(vehicle.getImpulseX() + pushX);
            impulse.centerZ(vehicle.getImpulseZ() + pushZ);
            impulse.expandRadius(control.horizontalReach() + preset.vehicleHorizontalPad());
            impulseBandLo = vehicle.getImpulseY() + pushY;
            impulseClip = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                    startX - half, startY, startZ - half,
                    startX + half, startY + height, startZ + half, impulseBandLo, true);
            impulse.floor(impulseClip - control.down());
            impulse.addDescentSlack(vPad);
            impulse.enforceDescentFloor(true);
            impulse.ceiling(impulseClip + control.up() + vPad);
            hypotheses.enable(HypothesisSet.Slot.IMPULSE);
        }

        HypothesisSet.Slot chosen = hypotheses.judge(obsX, obsY, obsZ, -1.0);
        if (chosen == HypothesisSet.Slot.IMPULSE) vehicle.consumeImpulse();
        AreaBounds chosenBounds = hypotheses.chosenBounds();
        JudgedExcess excess = hypotheses.chosenExcess();
        BoundBreach breach = classify(excess, preset);

        double chosenBandLo = chosen == HypothesisSet.Slot.IMPULSE ? impulseBandLo : bandLo;
        double chosenClipLo = chosen == HypothesisSet.Slot.IMPULSE ? impulseClip : mainClipLo;
        boolean landed = chosenClipLo != chosenBandLo && obsY <= chosenClipLo + CLIP_EPS;
        double vyLo;
        double vyHi;
        if (landed) {
            vyLo = 0.0;
            vyHi = 0.0;
        } else {
            double clamped = Math.min(Math.max(obsY, chosenBounds.floor() - chosenBounds.descentSlack()),
                    chosenBounds.ceiling());
            vyLo = clamped * drag;
            vyHi = clamped * drag;
        }
        lastDy = vyLo;
        AreaAdvancer.clampObserved(chosenBounds, obsX, obsY, obsZ, false, 0.0);
        carried = AreaAdvancer.zeroClamp(new MotionArea(chosenBounds.legalX() * drag,
                chosenBounds.legalZ() * drag, 0.0, vyLo, vyHi), false);

        verdict = judged(BodyKind.GHAST, MediumKind.FLYING, GroundState.AMBIGUOUS,
                obsX, obsY, obsZ, excess, breach, chosenBounds);
        trace.record(context.view(), contact, sample, null, control,
                chosenBounds, verdict, reader, 0.0, 0.0, preX, preZ, preFloor, preCeil);
    }

    private void markBoostLag(int vehicleId) {
        if (vehicleId < 0) return;
        TrackedEntity ridden = world.entities().resolve(vehicleId);
        if (ridden != null && EntityRoles.steerableMob(ridden.type())) ridden.addBoostLag();
    }

    private void scan(ShapeQuery query, double startX, double startY, double startZ,
                      double half, double height, double obsX, double obsY, double obsZ,
                      double reachDown, double reachUp, double stepHeight) {
        double minX = startX - half + Math.min(0.0, obsX) - HARVEST_MARGIN;
        double minY = startY + Math.min(0.0, reachDown) - HARVEST_MARGIN;
        double minZ = startZ - half + Math.min(0.0, obsZ) - HARVEST_MARGIN;
        double maxX = startX + half + Math.max(0.0, obsX) + HARVEST_MARGIN;
        double maxY = startY + height + Math.max(0.0, reachUp) + HARVEST_MARGIN;
        double maxZ = startZ + half + Math.max(0.0, obsZ) + HARVEST_MARGIN;
        ColliderCollector.fill(colliders, reader, world.entities(), query, ExemptCells.NONE,
                data.getPistonData(), minX, minY, minZ, maxX, maxY, maxZ);
        sweep.resolve(colliders, contact,
                startX, startY, startZ,
                half, height, obsX, obsY, obsZ,
                stepHeight, groundResolver.lastGroundedEnd());
        double endX = startX + obsX;
        double endY = startY + obsY;
        double endZ = startZ + obsZ;
        TraitSampler.sample(reader, contact, endX, endY, endZ, half);
        MediumScan.sample(reader, sample,
                true, world.dimension().dimensionType() != null
                        && world.dimension().dimensionType().isUltraWarm(),
                gates.modernFluidPush(), false, true,
                startX - half, startY, startZ - half,
                startX + half, startY + height, startZ + half,
                Math.min(startX, endX) - half, Math.min(startY, endY),
                Math.min(startZ, endZ) - half,
                Math.max(startX, endX) + half,
                Math.max(startY, endY) + height,
                Math.max(startZ, endZ) + half);
    }

    private BoundBreach classify(JudgedExcess excess, PhysicsPreset preset) {
        if (excess.descent() > preset.verticalFlagEpsilon()) return BoundBreach.DESCENT_FLOOR;
        if (excess.ascent() > preset.verticalFlagEpsilon()) return BoundBreach.ASCENT;
        if (excess.horizontal() > preset.horizontalFlagEpsilon()) return BoundBreach.HORIZONTAL_DISK;
        return null;
    }

    private void seedFrom(VehicleData vehicle, BodyKind kind) {
        mounts.markSeeded();
        double vx = clamp(vehicle.deltaX(), MOUNT_PAD);
        double vz = clamp(vehicle.deltaZ(), MOUNT_PAD);
        double vy = clamp(vehicle.deltaY(), MOUNT_PAD);
        carried = new MotionArea(vx, vz, 0.0, vy, vy);
        lastDy = vy;
        groundResolver.reset();
        groundResolver.seed(false);
        setback.rememberSafe(vehicle.getCurX(), vehicle.getCurY(), vehicle.getCurZ());
    }

    private void disengage() {
        mounts.reset();
        carried = MotionArea.rest();
        lastDy = 0.0;
        groundResolver.reset();
        setback.reset();
        boatBody.floatModel().reset();
        stuckFactor.reset();
        verdict = idle(BodyKind.HORSE);
    }

    private PhysicsVerdict judged(BodyKind kind, MediumKind medium, GroundState ground,
                                  double obsX, double obsY, double obsZ,
                                  JudgedExcess excess, BoundBreach breach, AreaBounds bounds) {
        return new PhysicsVerdict(MotionStream.VEHICLE, kind,
                TickOutcome.JUDGED, null, breach,
                obsX, obsY, obsZ,
                excess.horizontal(), excess.ascent(), excess.descent(), 0.0,
                bounds.centerX(), bounds.centerZ(), bounds.radius(),
                bounds.ceiling(), bounds.floor() - bounds.descentSlack(),
                medium, ground,
                false, false, false,
                MitigationOutcome.NONE, FallFinding.NONE);
    }
}
