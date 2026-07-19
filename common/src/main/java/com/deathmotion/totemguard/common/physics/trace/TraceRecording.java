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

package com.deathmotion.totemguard.common.physics.trace;

import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.EngineContext;
import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugContext;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugLevel;
import com.deathmotion.totemguard.common.physics.verdict.MotionStream;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.FireworkData;
import com.deathmotion.totemguard.common.player.data.GlideData;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class TraceRecording {

    private final EngineActor actor;
    private final Data data;
    private final EngineContext context;
    private final TraceFrame frame = new TraceFrame();
    private final TraceDump dumper;
    private TrackedEntity stagedEntity;
    private double stagedPlyMinX, stagedPlyMaxX, stagedPlyMinZ, stagedPlyMaxZ;
    private int stagedAuthoritativeId = -1;
    private int stagedVehicleId = -1;
    private String stagedNearby = "";
    private long stagedContributors;
    private long stagedTaints;
    private byte stagedChosenSlot;
    private byte stagedLiveCount = 1;
    private TickRecorder recorder;
    private long tickCounter;
    private long lastEchoSeq;

    public TraceRecording(EngineActor actor, Data data, EngineContext context) {
        this.actor = actor;
        this.data = data;
        this.context = context;
        this.dumper = new TraceDump(context.logger());
    }

    private static int flags(@Nullable ContactReport contact, @Nullable MediumSample sample,
                             @Nullable GroundFacts ground, @Nullable ControlEnvelope input,
                             PhysicsVerdict verdict) {
        int flags = 0;
        if (input != null) {
            if (input.sprinting()) flags |= TraceFrame.FLAG_SPRINT;
            if (input.sneaking()) flags |= TraceFrame.FLAG_SNEAK;
            if (input.jumpPossible()) flags |= TraceFrame.FLAG_JUMP_POSSIBLE;
        }
        if (verdict.inventoryOpen()) flags |= TraceFrame.FLAG_INVENTORY_OPEN;
        if (contact != null) {
            if (contact.wallNear()) flags |= TraceFrame.FLAG_WALL_NEAR;
            if (contact.startOverlapping()) flags |= TraceFrame.FLAG_START_OVERLAP;
            if (contact.stepUsedHeight() > 0.0) flags |= TraceFrame.FLAG_STEP_USED;
        }
        if (sample != null) {
            if (sample.stuck()) flags |= TraceFrame.FLAG_STUCK;
            if (sample.bubbleAscent() > 0.0) flags |= TraceFrame.FLAG_BUBBLE;
        }
        if (ground != null) {
            if (ground.groundedEnd()) flags |= TraceFrame.FLAG_GROUNDED_END;
            if (ground.arrested()) flags |= TraceFrame.FLAG_ARRESTED;
        }
        if (verdict.knockbackConsumed()) flags |= TraceFrame.FLAG_ALT_CENTER;
        return flags;
    }

    public @Nullable TickRecorder recorder() {
        return recorder;
    }

    public void reset() {
        if (recorder != null) recorder.clear();
        tickCounter = 0;
        lastEchoSeq = data.getSharedFlagsEchoSeq();
    }

    public boolean dumpNow(String cause) {
        if (recorder == null) return false;
        Set<PhysicsDebugContext> contexts = context.view().physicsDebugContexts();
        return dumper.dump(actor, recorder, cause, contexts);
    }

    public void contributors(long bits) {
        this.stagedContributors = bits;
    }

    public void taints(long bits) {
        this.stagedTaints = bits;
    }

    public void stageNearestStandable(EntityTracker entities, double x, double y, double z, double half) {
        if (!context.view().physicsDebugLevel().recording()) {
            stagedEntity = null;
            stagedAuthoritativeId = -1;
            stagedVehicleId = -1;
            stagedNearby = "";
            return;
        }
        stagedEntity = entities.nearestStandable(x, y, z);
        stagedPlyMinX = x - half;
        stagedPlyMaxX = x + half;
        stagedPlyMinZ = z - half;
        stagedPlyMaxZ = z + half;
        stagedAuthoritativeId = entities.authoritativeId();
        stagedVehicleId = data.getVehicleId();
        stagedNearby = entities.describeStandablesNear(x, z, 2.5);
    }

    public void hypotheses(int chosenSlot, int liveCount) {
        this.stagedChosenSlot = (byte) chosenSlot;
        this.stagedLiveCount = (byte) liveCount;
    }

    public void record(ConfigView view,
                       @Nullable ContactReport contact, @Nullable MediumSample sample,
                       @Nullable GroundFacts ground, @Nullable ControlEnvelope input,
                       AreaBounds bounds, PhysicsVerdict verdict,
                       BlockReader reader, double buffer, double engineFall,
                       double preCarriedX, double preCarriedZ, double preCarriedFloor, double preCarriedCeil) {
        tickCounter++;
        long echoSeq = data.getSharedFlagsEchoSeq();
        boolean echoLanded = echoSeq != lastEchoSeq;
        lastEchoSeq = echoSeq;
        PhysicsDebugLevel level = view.physicsDebugLevel();
        if (!level.recording()) return;
        if (recorder == null) recorder = new TickRecorder();

        frame.tick = tickCounter;
        frame.stream = (byte) verdict.stream().ordinal();
        frame.body = (byte) verdict.body().ordinal();
        frame.obsX = verdict.observedX();
        frame.obsY = verdict.observedY();
        frame.obsZ = verdict.observedZ();
        if (input != null) {
            frame.yaw = Math.toDegrees(Math.atan2(-input.lookX(), input.lookZ()));
            frame.pitch = input.pitchDegrees();
            frame.prevYaw = Math.toDegrees(Math.atan2(-input.prevLookX(), input.prevLookZ()));
            frame.prevPitch = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, -input.prevLookY()))));
        } else {
            frame.yaw = 0.0;
            frame.pitch = 0.0;
            frame.prevYaw = 0.0;
            frame.prevPitch = 0.0;
        }
        frame.preCarriedX = preCarriedX;
        frame.preCarriedZ = preCarriedZ;
        frame.preCarriedFloor = preCarriedFloor;
        frame.preCarriedCeil = preCarriedCeil;
        frame.centerX = bounds.centerX();
        frame.centerZ = bounds.centerZ();
        frame.radius = bounds.radius();
        frame.ceiling = bounds.ceiling();
        frame.floor = bounds.judgedFloor();
        frame.altPresent = bounds.hasAltCenter();
        frame.altCenterX = frame.altPresent ? bounds.altCenterX() : 0.0;
        frame.altCenterZ = frame.altPresent ? bounds.altCenterZ() : 0.0;
        frame.horizontalExcess = verdict.horizontalExcess();
        frame.ascentExcess = verdict.ascentExcess();
        frame.descentExcess = verdict.descentExcess();
        frame.phaseExcess = verdict.phaseExcess();
        frame.outcome = (byte) verdict.outcome().ordinal();
        frame.reason = verdict.declineReason() == null ? -1 : (byte) verdict.declineReason().ordinal();
        frame.breach = verdict.breach() == null ? -1 : (byte) verdict.breach().ordinal();
        frame.medium = (byte) verdict.medium().ordinal();
        frame.ground = (byte) verdict.ground().ordinal();
        frame.flags = flags(contact, sample, ground, input, verdict);
        if (data.getMovementData().isOnGround()) frame.flags |= TraceFrame.FLAG_CLAIMED_GROUND;
        if (data.isSprinting()) frame.flags |= TraceFrame.FLAG_RAW_SPRINT;
        if (echoLanded) frame.flags |= TraceFrame.FLAG_ECHO_LANDED;
        if (data.isEchoedSprinting()) frame.flags |= TraceFrame.FLAG_ECHO_SPRINT;
        if (data.isEchoedSwimming()) frame.flags |= TraceFrame.FLAG_ECHO_SWIM;
        frame.contributors = stagedContributors;
        stagedContributors = 0L;
        frame.taints = stagedTaints;
        stagedTaints = 0L;
        frame.chosenSlot = stagedChosenSlot;
        frame.liveCount = stagedLiveCount;
        stagedChosenSlot = 0;
        stagedLiveCount = 1;
        populateContext(sample, input);
        frame.supportGap = contact != null ? Math.min(contact.nearestSupportGap(), 9.999) : 0.0;
        frame.ceilingClearance = contact != null ? contact.ceilingClearance() : 0.0;
        frame.supportTop = contact != null ? contact.supportTop() : Double.NEGATIVE_INFINITY;
        if (contact != null && contact.supportIsEntity()) frame.flags |= TraceFrame.FLAG_SUPPORT_ENTITY;
        populateEntity();
        frame.reads = reader.readsThisTick();
        frame.misses = reader.missesThisTick();
        frame.uncertainHits = reader.uncertainHitsThisTick();
        frame.buffer = buffer;
        frame.engineFall = engineFall;
        frame.mitigation = (byte) ((verdict.mitigation().triggered() ? 1 : 0)
                | (verdict.mitigation().setbackIssued() ? 2 : 0)
                | (verdict.mitigation().setbackSkipped() ? 4 : 0)
                | (verdict.mitigation().inventoryClosed() ? 8 : 0));
        recorder.record(frame);

        Set<PhysicsDebugContext> contexts = view.physicsDebugContexts();
        if (level == PhysicsDebugLevel.TRACE) {
            context.logger().info(
                    "[PhysicsTrace] " + actor.name() + " " + TraceFormatter.format(frame, contexts));
        }
        if (verdict.mitigation().triggered() || verdict.fall().violation()
                || (verdict.stream() == MotionStream.VEHICLE && verdict.breach() != null)) {
            dumper.dump(actor, recorder, verdict.breach() != null ? verdict.breach().name() : "fall", contexts);
        }
    }

    private void populateEntity() {
        TrackedEntity entity = stagedEntity;
        stagedEntity = null;
        frame.entPlyMinX = stagedPlyMinX;
        frame.entPlyMaxX = stagedPlyMaxX;
        frame.entPlyMinZ = stagedPlyMinZ;
        frame.entPlyMaxZ = stagedPlyMaxZ;
        frame.entAuthoritativeId = stagedAuthoritativeId;
        frame.entVehicleId = stagedVehicleId;
        frame.entNearby = stagedNearby;
        stagedAuthoritativeId = -1;
        stagedVehicleId = -1;
        stagedNearby = "";
        frame.entTracked = entity != null;
        if (entity == null) return;
        frame.entRenderX = entity.renderX();
        frame.entRenderY = entity.renderY();
        frame.entRenderZ = entity.renderZ();
        frame.entTargetX = entity.targetX();
        frame.entTargetY = entity.targetY();
        frame.entTargetZ = entity.targetZ();
        frame.entSpanMinX = entity.spanMinX();
        frame.entSpanMaxX = entity.spanMaxX();
        frame.entSpanMinZ = entity.spanMinZ();
        frame.entSpanMaxZ = entity.spanMaxZ();
        frame.entHalfWidth = entity.halfWidth();
        frame.entHeight = entity.height();
        frame.entSteps = entity.interpSteps();
    }

    private void populateContext(@Nullable MediumSample sample, @Nullable ControlEnvelope input) {
        frame.pushX = frame.pushY = frame.pushZ = 0.0;
        frame.bubbleAscent = 0.0;
        frame.stuckHorizontal = frame.stuckVertical = 1.0;
        frame.fluidFriction = frame.fluidAccel = 0.0;
        frame.boxMinX = frame.boxFeetY = frame.boxMinZ = 0.0;
        frame.boxMaxX = frame.boxHeadY = frame.boxMaxZ = 0.0;
        frame.eyeSampleY = 0.0;
        frame.wetCellFound = false;
        frame.wetCellX = frame.wetCellY = frame.wetCellZ = 0;
        frame.wetCellSurface = 0.0;
        frame.fluidCellX0 = frame.fluidCellX1 = 0;
        frame.fluidCellY0 = frame.fluidCellY1 = 0;
        frame.fluidCellZ0 = frame.fluidCellZ1 = 0;
        frame.moveSpeed = frame.jumpStrength = frame.stepHeight = 0.0;
        frame.riptideStrength = 0.0;
        frame.fireworkMin = frame.fireworkMax = 0;

        if (sample != null) {
            frame.pushX = sample.pushX();
            frame.pushY = sample.pushY();
            frame.pushZ = sample.pushZ();
            frame.bubbleAscent = sample.bubbleAscent();
            frame.stuckHorizontal = sample.stuckHorizontal();
            frame.stuckVertical = sample.stuckVertical();
            frame.boxMinX = sample.boxMinX();
            frame.boxFeetY = sample.boxFeetY();
            frame.boxMinZ = sample.boxMinZ();
            frame.boxMaxX = sample.boxMaxX();
            frame.boxHeadY = sample.boxHeadY();
            frame.boxMaxZ = sample.boxMaxZ();
            frame.eyeSampleY = sample.eyeSampleY();
            frame.wetCellFound = sample.wetCellFound();
            frame.wetCellX = sample.wetCellX();
            frame.wetCellY = sample.wetCellY();
            frame.wetCellZ = sample.wetCellZ();
            frame.wetCellSurface = sample.wetCellSurface();
            frame.fluidCellX0 = sample.fluidCellX0();
            frame.fluidCellX1 = sample.fluidCellX1();
            frame.fluidCellY0 = sample.fluidCellY0();
            frame.fluidCellY1 = sample.fluidCellY1();
            frame.fluidCellZ0 = sample.fluidCellZ0();
            frame.fluidCellZ1 = sample.fluidCellZ1();
            if (sample.eyeInWater()) frame.flags |= TraceFrame.FLAG_EYE_IN_WATER;
            if (sample.waterAtFeet()) frame.flags |= TraceFrame.FLAG_WATER_AT_FEET;
            if (sample.swimSteerWater()) frame.flags |= TraceFrame.FLAG_SWIM_STEER;
            if (sample.climbable()) frame.flags |= TraceFrame.FLAG_CLIMBABLE;
            if (sample.climbableUncertain()) frame.flags |= TraceFrame.FLAG_CLIMB_UNCERTAIN;
        }
        if (input != null) {
            frame.fluidFriction = input.fluidFriction();
            frame.fluidAccel = input.fluidAccel();
            frame.moveSpeed = input.moveSpeed();
            frame.jumpStrength = input.jumpStrength();
            frame.stepHeight = input.stepHeight();
            if (input.swimming()) frame.flags |= TraceFrame.FLAG_SWIMMING;
            if (input.fluidExitHop()) frame.flags |= TraceFrame.FLAG_FLUID_HOP;
        }

        GlideData glide = data.getGlideData();
        if (glide.claimActive()) frame.flags |= TraceFrame.FLAG_GLIDE_CLAIM;
        if (glide.riptideActive()) {
            frame.flags |= TraceFrame.FLAG_GLIDE_RIPTIDE;
            frame.riptideStrength = glide.riptideStrength();
        }
        if (glide.exitActive()) frame.flags |= TraceFrame.FLAG_GLIDE_EXIT;
        FireworkData firework = data.getFireworkData();
        frame.fireworkMin = firework.boostCountMin();
        frame.fireworkMax = firework.boostCountMax();
    }
}
