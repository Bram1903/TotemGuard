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

package com.deathmotion.totemguard.common.player.movement;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.data.*;
import com.deathmotion.totemguard.common.player.movement.area.MotionArea;
import com.deathmotion.totemguard.common.player.movement.sim.MovementInput;
import com.deathmotion.totemguard.common.player.movement.sim.MovementSimulator;
import com.deathmotion.totemguard.common.player.movement.world.BlockEnvironment;
import com.deathmotion.totemguard.common.player.movement.world.BlockEnvironmentScanner;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;

public class MovementEstimator {

    private static final double HIT_EPSILON = 0.002;
    private static final double VERTICAL_HIT_EPSILON = 0.003;
    private static final double STRONG_SINGLE_EXCESS = 0.40;

    private static final double HORIZONTAL_PAD = 0.010;
    private static final double VERTICAL_PAD = 0.025;

    private static final int WINDOW = 20;
    private static final long WINDOW_MASK = (1L << WINDOW) - 1;
    private static final int HITS_FOR_MOVED = 5;

    private static final double FAST_HORIZONTAL_CAP = 10.0;
    private static final double FAST_RISE_CAP = 1.5;
    private static final int FAST_TOLERANCE = 1;
    private static final double BALLISTIC_DECAY = 0.04;
    private static final double BORDER_MARGIN = 2.0;

    private static final double GROUNDSPOOF_VERTICAL_EPS = 0.1;
    private static final int GROUNDSPOOF_TOLERANCE = 4;

    private static final double WATER_WALK_SINK_EPS = 0.02;
    private static final int WATER_WALK_TOLERANCE = 8;

    private static final double AIRWALK_GAP = 0.25;
    private static final double AIRWALK_MOVE_EPS = 0.05;
    private static final int AIRWALK_TOLERANCE = 8;

    private static final int SNEAK_CONFIRM = 3;
    private static final double SNEAK_DIAGONAL_FACTOR = Math.sqrt(2.0);

    private static final double KNOCKBACK_PAD = 0.05;
    private static final double ENTITY_PUSH_PER = 0.08;
    private static final double MAX_ENTITY_PUSH = 0.30;

    private static final double ONGROUND_RISE_TOLERANCE = 0.1;
    private static final int GROUND_RISE_FORGIVE_LIMIT = 2;

    private static final int HOVER_TOLERANCE = 4;
    private static final double HOVER_EXCESS = MovementConstants.GRAVITY;
    private static final double JUMP_LIKE_ASCENT = 0.3;

    private static final double SPRINT_MIN_SPEED = 0.15;
    private static final double SPRINT_BACKWARD_COS = -0.6;
    private static final int SPRINT_TURN_TOLERANCE = 8;
    private static final double SPRINT_GROUND_SLIPPERINESS = 0.61;

    private final Data data;

    private boolean initialized;
    private MotionArea carried = MotionArea.resting();

    private long hitWindow;
    private boolean movedSticky;
    private int flyingSinceTickEnd;
    private int groundRiseStreak;
    private int airborneWithholdStreak;
    private int backwardSprintStreak;
    private int fastStreak;
    private int groundMoveStreak;
    private int waterWalkStreak;
    private int airWalkStreak;
    private int sneakStreak;
    private double lastObservedVy;

    @Getter
    private boolean improperSprint;

    @Getter
    private MovementResult result = MovementResult.INITIAL;

    public MovementEstimator(Data data) {
        this.data = data;
    }

    private static MovementCause bailCause(BlockEnvironment env) {
        if (!env.feetLoaded()) return MovementCause.UNLOADED;
        if (env.bouncyBelow()) return MovementCause.BOUNCE;
        return MovementCause.STUCK;
    }

    public void onTickEnd() {
        flyingSinceTickEnd = 0;
    }

    private void disengage() {
        if (!initialized) return;
        initialized = false;
        carried = MotionArea.resting();
        clearExcess();
        result = MovementResult.INITIAL;
    }

    public void onFlying() {
        if (!TGPlatform.getInstance().getConfigRepository().configView().physicsEngineEnabled()) {
            disengage();
            return;
        }

        final MovementData movement = data.getMovementData();
        data.getWorldEntityData().advanceInterpolation();

        final Location current = movement.getCurrent();
        final Location previous = movement.getPrevious();
        final Vector3d observed = new Vector3d(
                current.getX() - previous.getX(),
                current.getY() - previous.getY(),
                current.getZ() - previous.getZ());
        final double previousObservedVy = lastObservedVy;
        lastObservedVy = observed.getY();
        if (flyingSinceTickEnd < 1000) flyingSinceTickEnd++;

        try {
            if (!initialized) {
                initialized = true;
                seedCarried(observed);
                result = MovementResult.INITIAL;
                clearExcess();
                return;
            }

            if (movement.isLastFlyingWasResync()) {
                decline(MovementCause.RESYNC, observed);
                return;
            }

            MovementCause bail = preScanBail(observed, current);
            if (bail != null) {
                decline(bail, observed);
                return;
            }

            if (handleGroundSpoof(observed, movement)) return;

            if (handleFast(observed, previousObservedVy)) return;

            BlockEnvironment env = BlockEnvironmentScanner.scan(
                    data.getClientWorld(), current, previous, data.getAttributeData().width(), poseHeight());
            if (!env.feetLoaded() || env.bouncyBelow()) {
                decline(bailCause(env), observed);
                return;
            }

            if (handleWaterWalk(observed, env)) return;

            if (handleAirWalk(observed, env, movement)) return;

            MovementInput input = buildInput(movement, env, observed);

            if (trusted(movement)) {
                judge(MovementSimulator.predictMove(carried, input, env), input, env, movement, observed);
            } else {
                double coastHorizontal = MovementSimulator.predictMove(carried, input, env).horizontalSpeed().max();
                carried = MovementSimulator.advance(coastHorizontal, carried.vertical().max(), input, env);
                boolean withheld = !movement.isLastFlyingPositionChanged();
                boolean airborne = !movement.isOnGround() && !env.supported()
                        && !env.fluid() && !env.stuck() && !env.climbable();
                airborneWithholdStreak = (withheld && airborne) ? airborneWithholdStreak + 1 : 0;
                shiftWindow(false);
                movedSticky = Long.bitCount(hitWindow) >= HITS_FOR_MOVED;
                if (airborneWithholdStreak > HOVER_TOLERANCE) {
                    result = new MovementResult(MovementCause.HOVER, observed, MotionArea.resting(),
                            0.0, HOVER_EXCESS, false, true, false);
                } else {
                    result = MovementResult.unpredictable(
                            withheld ? MovementCause.WITHHELD : MovementCause.DOUBLE_MOVE, observed);
                }
            }
        } finally {
            data.getExternalVelocityData().tick();
            data.getPistonData().tick();
            data.getEffectData().tick();
        }
    }

    private void judge(MotionArea move, MovementInput input, BlockEnvironment env, MovementData movement, Vector3d observed) {
        airborneWithholdStreak = 0;
        fastStreak = 0;
        double observedSpeed = Math.hypot(observed.getX(), observed.getZ());
        double observedVy = observed.getY();

        MotionArea allowed = move.expand(HORIZONTAL_PAD, VERTICAL_PAD);
        double horizontalExcess = allowed.horizontalExcess(observedSpeed);
        double verticalExcess = allowed.ascentExcess(observedVy);

        boolean knockbackConsumed = false;
        ExternalVelocityData external = data.getExternalVelocityData();
        if (external.isActive() && (horizontalExcess > HIT_EPSILON || verticalExcess > VERTICAL_HIT_EPSILON)) {
            MotionArea widened = allowed.expand(
                    Math.hypot(external.x(), external.z()) + KNOCKBACK_PAD,
                    Math.max(0.0, external.y()) + KNOCKBACK_PAD);
            double h = widened.horizontalExcess(observedSpeed);
            double v = widened.ascentExcess(observedVy);
            if (h < horizontalExcess || v < verticalExcess) {
                allowed = widened;
                horizontalExcess = h;
                verticalExcess = v;
                knockbackConsumed = h <= HIT_EPSILON && v <= VERTICAL_HIT_EPSILON;
            }
        }

        if (horizontalExcess > HIT_EPSILON) {
            double push = nearbyEntityPush(observed);
            if (push > 0.0) {
                allowed = allowed.expand(push, 0.0);
                horizontalExcess = allowed.horizontalExcess(observedSpeed);
            }
        }

        if (movement.isOnGround() && verticalExcess > VERTICAL_HIT_EPSILON && observedVy <= ONGROUND_RISE_TOLERANCE) {
            if (groundRiseStreak < GROUND_RISE_FORGIVE_LIMIT) verticalExcess = 0.0;
            groundRiseStreak++;
        } else {
            groundRiseStreak = 0;
        }

        boolean landMedium = !env.fluid() && !env.climbable() && !env.stuck();
        double descentFloor = carried.vertical().min();
        boolean fellTooFast = false;
        if (landMedium && descentFloor <= 0.0) {
            double slack = VERTICAL_PAD;
            if (external.isActive()) slack += Math.max(0.0, -external.y()) + KNOCKBACK_PAD;
            double descentExcess = (descentFloor - slack) - observedVy;
            if (descentExcess > verticalExcess) {
                verticalExcess = descentExcess;
                fellTooFast = true;
            }
        }

        boolean movedThisTick = horizontalExcess > HIT_EPSILON;
        boolean ascendingThisTick = verticalExcess > VERTICAL_HIT_EPSILON;
        shiftWindow(movedThisTick);
        movedSticky = horizontalExcess >= STRONG_SINGLE_EXCESS || Long.bitCount(hitWindow) >= HITS_FOR_MOVED;

        if (knockbackConsumed) external.consume();

        double legalSpeed = Math.min(observedSpeed, allowed.horizontalSpeed().max());
        double legalVy = Math.min(observedVy, allowed.vertical().max());
        carried = MovementSimulator.advance(legalSpeed, legalVy, input, env);

        MovementCause cause = fellTooFast ? MovementCause.FAST_FALL : describeCause(env, input, observedVy);
        result = new MovementResult(cause, observed, move,
                horizontalExcess, verticalExcess, movedThisTick, ascendingThisTick, knockbackConsumed);
    }

    private boolean trusted(MovementData movement) {
        if (!movement.isLastFlyingPositionChanged()) return false;
        boolean doubleMove = data.getPlayer().supportsEndTick()
                && flyingSinceTickEnd > 1
                && !data.getTeleportData().lastPacketWasTeleport();
        return !doubleMove;
    }

    private MovementInput buildInput(MovementData movement, BlockEnvironment env, Vector3d observed) {
        InputData.State state = data.getInputData().current();
        boolean inventoryOpen = data.isOpenInventory();
        boolean horizontalInput = !inventoryOpen
                && (state == null || state.forward() || state.backward() || state.left() || state.right());
        boolean jumpHeld = state == null || state.jumping();
        boolean jumpPossible = movement.isLastOnGround() && jumpHeld && !inventoryOpen;

        boolean sprinting = data.isSprinting() && sprintForward(movement, env, state, observed);
        improperSprint = data.isSprinting() && !sprinting && !inventoryOpen;

        sneakStreak = data.isSneaking() ? sneakStreak + 1 : 0;
        boolean sneaking = sneakStreak >= SNEAK_CONFIRM;
        boolean diagonal = state == null
                || ((state.forward() ^ state.backward()) && (state.left() ^ state.right()));

        EffectData effects = data.getEffectData();
        PlayerAttributeData attr = data.getAttributeData();
        return new MovementInput(movement.isLastOnGround(), movement.isOnGround(), horizontalInput, jumpPossible,
                sprinting, effectiveSpeed(sprinting, sneaking, diagonal),
                attr.jumpStrength(), attr.gravity(), attr.stepHeight(),
                effects.hasJumpBoost() ? effects.jumpBoostAmplifier() : -1,
                effects.hasLevitation(), effects.levitationAmplifier(), effects.hasSlowFalling());
    }

    private boolean sprintForward(MovementData movement, BlockEnvironment env, InputData.State state, Vector3d observed) {
        if (state != null) {
            backwardSprintStreak = 0;
            return state.forward();
        }
        double speed = Math.hypot(observed.getX(), observed.getZ());
        boolean onNormalGround = movement.isLastOnGround() && env.slipperiness() <= SPRINT_GROUND_SLIPPERINESS;
        boolean backward = false;
        if (onNormalGround && speed > SPRINT_MIN_SPEED) {
            double yaw = Math.toRadians(movement.getCurrent().getYaw());
            double forwardComponent = observed.getX() * -Math.sin(yaw) + observed.getZ() * Math.cos(yaw);
            backward = forwardComponent / speed < SPRINT_BACKWARD_COS;
        }
        backwardSprintStreak = backward ? backwardSprintStreak + 1 : 0;
        return backwardSprintStreak <= SPRINT_TURN_TOLERANCE;
    }

    private double effectiveSpeed(boolean sprinting, boolean sneaking, boolean diagonal) {
        double speed = data.getAttributeData().movementSpeed();
        if (sprinting) speed *= MovementConstants.SPRINT_SPEED_MULTIPLIER;
        if (sneaking) {
            double sneak = data.getAttributeData().sneakingSpeed();
            if (diagonal) sneak = Math.min(1.0, sneak * SNEAK_DIAGONAL_FACTOR);
            speed *= sneak;
        }
        return speed;
    }

    private MovementCause preScanBail(Vector3d observed, Location current) {
        if (data.isCanFly()) return MovementCause.FLY;
        if (data.isInVehicle()) return MovementCause.VEHICLE;
        if (data.isSwimming() || data.isGliding() || data.isSpinAttacking()) return MovementCause.GLIDE;
        if (data.getPistonData().isActive()) return MovementCause.PISTON;
        WorldBorderData border = data.getWorldBorderData();
        if (border.isActive() && border.distanceToEdge(current.getX(), current.getZ()) < BORDER_MARGIN) {
            return MovementCause.BORDER;
        }
        return null;
    }

    private boolean handleGroundSpoof(Vector3d observed, MovementData movement) {
        if (!movement.isOnGround() || Math.abs(observed.getY()) <= GROUNDSPOOF_VERTICAL_EPS) {
            groundMoveStreak = 0;
            return false;
        }
        groundMoveStreak++;
        if (groundMoveStreak <= GROUNDSPOOF_TOLERANCE) return false;
        flagGroundSpoof(observed);
        return true;
    }

    private void flagGroundSpoof(Vector3d observed) {
        airborneWithholdStreak = 0;
        improperSprint = false;
        double excess = Math.abs(observed.getY()) - GROUNDSPOOF_VERTICAL_EPS;
        result = new MovementResult(MovementCause.GROUNDSPOOF, observed, MotionArea.resting(),
                0.0, excess, false, true, false);
        seedCarried(observed);
    }

    private boolean handleWaterWalk(Vector3d observed, BlockEnvironment env) {
        boolean onSurface = env.fluidBelow() && !env.fluid() && !env.supported()
                && !env.climbable() && !env.stuck();
        if (!onSurface || observed.getY() < -WATER_WALK_SINK_EPS) {
            waterWalkStreak = 0;
            return false;
        }
        waterWalkStreak++;
        if (waterWalkStreak <= WATER_WALK_TOLERANCE) return false;
        flagWaterWalk(observed);
        return true;
    }

    private void flagWaterWalk(Vector3d observed) {
        airborneWithholdStreak = 0;
        improperSprint = false;
        result = new MovementResult(MovementCause.WATER_WALK, observed, MotionArea.resting(),
                0.0, HOVER_EXCESS, false, true, false);
        seedCarried(observed);
    }

    private boolean handleAirWalk(Vector3d observed, BlockEnvironment env, MovementData movement) {
        boolean moving = Math.hypot(observed.getX(), observed.getZ()) > AIRWALK_MOVE_EPS;
        boolean candidate = movement.isOnGround() && moving && env.groundGap() > AIRWALK_GAP
                && !env.fluid() && !env.fluidBelow() && !env.climbable() && !env.stuck();
        if (!candidate) {
            airWalkStreak = 0;
            return false;
        }
        airWalkStreak++;
        if (airWalkStreak <= AIRWALK_TOLERANCE) return false;
        flagAirWalk(observed, env.groundGap());
        return true;
    }

    private void flagAirWalk(Vector3d observed, double gap) {
        airborneWithholdStreak = 0;
        improperSprint = false;
        result = new MovementResult(MovementCause.AIRWALK, observed, MotionArea.resting(),
                0.0, gap, false, true, false);
        seedCarried(observed);
    }

    private boolean handleFast(Vector3d observed, double previousVy) {
        double horizontal = Math.hypot(observed.getX(), observed.getZ());
        double vy = observed.getY();
        boolean fastHorizontal = horizontal > FAST_HORIZONTAL_CAP;
        boolean fastUp = vy > FAST_RISE_CAP;
        boolean decelerating = previousVy - vy > BALLISTIC_DECAY;

        boolean poweredRise = fastUp && !decelerating && !data.getEffectData().hasLevitation();
        boolean suspicious = fastHorizontal || poweredRise;

        if (!suspicious) {
            fastStreak = 0;
            if (fastHorizontal || fastUp) {
                decline(MovementCause.FAST, observed);
                return true;
            }
            return false;
        }

        if (data.getExternalVelocityData().isActive()) {
            fastStreak = 0;
            decline(MovementCause.FAST, observed);
            return true;
        }

        fastStreak++;
        if (fastStreak <= FAST_TOLERANCE) {
            decline(MovementCause.FAST, observed);
            return true;
        }
        flagFast(observed, poweredRise && !fastHorizontal, horizontal);
        return true;
    }

    private void flagFast(Vector3d observed, boolean fastUp, double horizontal) {
        airborneWithholdStreak = 0;
        improperSprint = false;
        double excess = fastUp ? observed.getY() - FAST_RISE_CAP : horizontal - FAST_HORIZONTAL_CAP;
        result = new MovementResult(MovementCause.FAST, observed, MotionArea.resting(),
                fastUp ? 0.0 : excess, fastUp ? excess : 0.0, !fastUp, fastUp, false);
        seedCarried(observed);
    }

    private MovementCause describeCause(BlockEnvironment env, MovementInput input, double observedVy) {
        if (env.fluid()) return MovementCause.FLUID;
        if (env.stuck()) return MovementCause.STUCK;
        if (env.climbable()) return MovementCause.CLIMB;
        if (input.levitation()) return MovementCause.LEVITATION;
        if (env.supported()) return observedVy > VERTICAL_HIT_EPSILON ? MovementCause.STEP : MovementCause.GROUND;
        if (observedVy > JUMP_LIKE_ASCENT) return MovementCause.JUMP;
        return MovementCause.AIR;
    }

    private double nearbyEntityPush(Vector3d observed) {
        Location current = data.getMovementData().getCurrent();
        double prevX = current.getX() - observed.getX();
        double prevY = current.getY() - observed.getY();
        double prevZ = current.getZ() - observed.getZ();
        int count = data.getWorldEntityData().countPushableNear(
                Math.min(prevX, current.getX()), Math.min(prevY, current.getY()), Math.min(prevZ, current.getZ()),
                Math.max(prevX, current.getX()), Math.max(prevY, current.getY()), Math.max(prevZ, current.getZ()),
                data.getAttributeData().width() / 2.0, poseHeight());
        return Math.min(MAX_ENTITY_PUSH, count * ENTITY_PUSH_PER);
    }

    private double poseHeight() {
        double base = data.isSneaking() ? MovementConstants.SNEAKING_HEIGHT : MovementConstants.STANDING_HEIGHT;
        return base * data.getAttributeData().scale();
    }

    private void decline(MovementCause cause, Vector3d observed) {
        seedCarried(observed);
        shiftWindow(false);
        movedSticky = Long.bitCount(hitWindow) >= HITS_FOR_MOVED;
        airborneWithholdStreak = 0;
        backwardSprintStreak = 0;
        improperSprint = false;
        result = MovementResult.unpredictable(cause, observed);
    }

    private void seedCarried(Vector3d observed) {
        carried = MotionArea.of(Math.hypot(observed.getX(), observed.getZ()), observed.getY());
    }

    private void shiftWindow(boolean hit) {
        hitWindow = ((hitWindow << 1) | (hit ? 1L : 0L)) & WINDOW_MASK;
    }

    private void clearExcess() {
        hitWindow = 0;
        movedSticky = false;
        groundRiseStreak = 0;
        airborneWithholdStreak = 0;
        backwardSprintStreak = 0;
        improperSprint = false;
        fastStreak = 0;
        groundMoveStreak = 0;
        waterWalkStreak = 0;
        sneakStreak = 0;
    }

    public boolean movedHorizontally() {
        return movedSticky;
    }

    public boolean isMovedThisTick() {
        return result.movedThisTick();
    }

    public boolean isAscendingThisTick() {
        return result.ascendingThisTick();
    }

    public double getLastExcess() {
        return result.horizontalExcess();
    }

    public double getLastVerticalExcess() {
        return result.verticalExcess();
    }

    public MovementCause getCause() {
        return result.cause();
    }

    public int windowHits() {
        return Long.bitCount(hitWindow);
    }

    public int hitsForMoved() {
        return HITS_FOR_MOVED;
    }

    public void reset() {
        initialized = false;
        carried = MotionArea.resting();
        flyingSinceTickEnd = 0;
        clearHistory();
        data.getExternalVelocityData().reset();
        data.getPistonData().reset();
        data.getEffectData().reset();
    }

    public void clearHistory() {
        clearExcess();
        result = MovementResult.INITIAL;
    }
}
