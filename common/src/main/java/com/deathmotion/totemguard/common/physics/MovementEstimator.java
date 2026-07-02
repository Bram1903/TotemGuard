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

package com.deathmotion.totemguard.common.physics;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.sim.MovementInput;
import com.deathmotion.totemguard.common.physics.sim.MovementSimulator;
import com.deathmotion.totemguard.common.physics.world.BlockEnvironment;
import com.deathmotion.totemguard.common.physics.world.BlockEnvironmentScanner;
import com.deathmotion.totemguard.common.physics.world.WallGaps;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.ExternalVelocityData;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.data.WorldBorderData;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;

public class MovementEstimator {

    private static final double HIT_EPSILON = 0.002;
    private static final double VERTICAL_HIT_EPSILON = 0.003;
    private static final double STRONG_SINGLE_EXCESS = 0.40;

    private static final double HORIZONTAL_PAD = 0.010;
    private static final double VERTICAL_PAD = 0.025;
    private static final double PHASE_ENTRY_TOLERANCE = 0.05;
    private static final double PHASE_EMBED_TOLERANCE = 0.05;
    private static final int PHASE_WINDOW = 6;
    private static final double PHASE_MOVE_EPS = 0.015;

    private static final int WINDOW = 20;
    private static final long WINDOW_MASK = (1L << WINDOW) - 1;
    private static final int HITS_FOR_MOVED = 5;

    private static final double FAST_HORIZONTAL_CAP = 10.0;
    private static final int FAST_TOLERANCE = 1;
    private static final double BORDER_MARGIN = 2.0;

    private static final double GROUNDSPOOF_VERTICAL_EPS = 0.1;
    private static final int GROUNDSPOOF_TOLERANCE = 4;

    private static final double LANDING_REACH = 1.1;

    private static final double KNOCKBACK_PAD = 0.05;
    private static final double ENTITY_PUSH_PER = 0.08;
    private static final double MAX_ENTITY_PUSH = 0.30;

    private static final int HOVER_TOLERANCE = 4;
    private static final double HOVER_EXCESS = MovementConstants.GRAVITY;
    private static final double JUMP_LIKE_ASCENT = 0.3;

    private static final int STEP_UNCERTAINTY_TICKS = 4;
    private static final double STEP_HORIZONTAL_SLACK = 0.15;

    private static final int BUBBLE_LAUNCH_TICKS = 5;

    private final Data data;
    private final GroundTracker groundTracker = new GroundTracker();
    private final InputResolver inputResolver;

    private boolean initialized;
    private MotionArea carried = MotionArea.resting();

    private long hitWindow;
    private boolean movedSticky;
    private int flyingSinceTickEnd;
    private int airborneWithholdStreak;
    private int fastStreak;
    private int groundMoveStreak;
    private int stepMoveTicks;
    private int bubbleTicks;
    private double bubbleAscentCap;
    private int phaseWindow;

    @Getter
    private MovementResult result = MovementResult.INITIAL;

    public MovementEstimator(Data data) {
        this.data = data;
        this.inputResolver = new InputResolver(data);
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
        if (flyingSinceTickEnd < 1000) flyingSinceTickEnd++;

        try {
            if (!initialized) {
                initialized = true;
                groundTracker.seed(movement.isOnGround());
                seedCarried(observed);
                result = MovementResult.INITIAL;
                clearExcess();
                return;
            }

            if (movement.isLastFlyingWasResync()) {
                decline(MovementCause.RESYNC, observed);
                return;
            }

            MovementCause bail = preScanBail(current);
            if (bail != null) {
                decline(bail, observed);
                return;
            }

            if (handleGroundSpoof(observed, movement)) return;

            if (handleFast(observed)) return;

            BlockEnvironment env = BlockEnvironmentScanner.scan(
                    data.getClientWorld(), data.getWorldEntityData(), current, previous,
                    data.getAttributeData().width(), poseHeight(), data.getAttributeData().stepHeight(), data.isSneaking());
            if (!env.feetLoaded()) {
                decline(MovementCause.UNLOADED, observed);
                return;
            }

            GroundState ground = groundTracker.resolve(
                    observed.getY(), env, data.getAttributeData().stepHeight(), carried.vertical().min(), data.isSneaking());

            if (env.bubbleAscent() > 0.0) {
                bubbleTicks = BUBBLE_LAUNCH_TICKS;
                bubbleAscentCap = env.bubbleAscent();
            } else if (bubbleTicks > 0) {
                bubbleTicks--;
            }
            double bubbleAscent = bubbleTicks > 0 ? bubbleAscentCap : 0.0;

            MovementInput input = inputResolver.build(movement, env, observed, ground, bubbleAscent);

            if (trusted(movement)) {
                judge(MovementSimulator.predictMove(carried, input, env), input, env, observed);
            } else {
                double coastHorizontal = MovementSimulator.predictMove(carried, input, env).horizontalSpeed().max();
                carried = MovementSimulator.advance(coastHorizontal, carried.vertical().max(), input, env);
                boolean withheld = !movement.isLastFlyingPositionChanged();
                boolean airborne = !ground.groundedEnd()
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
                MovementDebug.log(data.getPlayer(), withheld ? "coast:withheld" : "coast:double",
                        observed, input, env, null, 0.0, 0.0);
            }

            if (ground.bounced()) {
                carried = MovementSimulator.advanceBounce(carried, ground.bounceFloor(), env.bounceFactor(), input);
            }
        } finally {
            data.getExternalVelocityData().tick();
            data.getPistonData().tick();
            data.getEffectData().tick();
        }
    }

    private void judge(MotionArea move, MovementInput input, BlockEnvironment env, Vector3d observed) {
        airborneWithholdStreak = 0;
        fastStreak = 0;
        double observedSpeed = Math.hypot(observed.getX(), observed.getZ());
        double observedVy = observed.getY();

        boolean steppedUp = input.groundedEnd() && observedVy > VERTICAL_HIT_EPSILON;
        if (steppedUp) stepMoveTicks = STEP_UNCERTAINTY_TICKS;

        boolean landMedium = !env.fluid() && !env.climbable() && !env.stuck();

        MotionArea allowed = move.expand(HORIZONTAL_PAD, VERTICAL_PAD);
        double horizontalExcess = allowed.horizontalExcess(observedSpeed);
        double verticalExcess = allowed.ascentExcess(observedVy);
        double wallExcess = landMedium ? phaseExcess(env.wallGaps(), observedSpeed) : 0.0;

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

        if (stepMoveTicks > 0 && horizontalExcess > 0.0) {
            horizontalExcess = Math.max(0.0, horizontalExcess - STEP_HORIZONTAL_SLACK);
        }

        double descentFloor = Math.min(carried.vertical().min(), MovementSimulator.restFallVelocity(input));
        boolean fellTooFast = false;
        if (landMedium && descentFloor <= 0.0) {
            double slack = VERTICAL_PAD;
            if (external.isActive()) slack += Math.max(0.0, -external.y()) + KNOCKBACK_PAD;
            if (input.groundedEnd()) slack += input.stepHeight();
            double descentExcess = (descentFloor - slack) - observedVy;
            if (descentExcess > verticalExcess) {
                verticalExcess = descentExcess;
                fellTooFast = true;
            }
        }

        if (!fellTooFast && verticalExcess > VERTICAL_HIT_EPSILON && observedVy <= -VERTICAL_HIT_EPSILON
                && landMedium && env.groundGap() <= LANDING_REACH) {
            verticalExcess = 0.0;
        }

        boolean phased = wallExcess > HIT_EPSILON;
        if (phased && wallExcess > horizontalExcess) horizontalExcess = wallExcess;

        boolean movedThisTick = horizontalExcess > HIT_EPSILON;
        boolean ascendingThisTick = verticalExcess > VERTICAL_HIT_EPSILON;
        shiftWindow(movedThisTick);
        movedSticky = horizontalExcess >= STRONG_SINGLE_EXCESS || Long.bitCount(hitWindow) >= HITS_FOR_MOVED;

        if (knockbackConsumed) external.consume();
        if (stepMoveTicks > 0) stepMoveTicks--;

        double legalSpeed = steppedUp
                ? move.horizontalSpeed().max()
                : Math.min(observedSpeed, allowed.horizontalSpeed().max());
        double legalVy = Math.min(observedVy, allowed.vertical().max());
        carried = MovementSimulator.advance(legalSpeed, legalVy, input, env);

        MovementCause cause;
        if (phased && horizontalExcess >= verticalExcess) {
            cause = MovementCause.PHASE;
        } else if (fellTooFast) {
            cause = MovementCause.FAST_FALL;
        } else if (data.isOpenInventory()) {
            cause = MovementCause.INVENTORY_MOVE;
        } else {
            cause = describeCause(env, input, observedVy);
        }
        result = new MovementResult(cause, observed, move,
                horizontalExcess, verticalExcess, movedThisTick, ascendingThisTick, knockbackConsumed);
        MovementDebug.log(data.getPlayer(), "judge:" + cause, observed, input, env, move, horizontalExcess, verticalExcess);
    }

    private double phaseExcess(WallGaps gaps, double observedSpeed) {
        double entry = gaps.crossing() - PHASE_ENTRY_TOLERANCE;
        boolean entering = entry > HIT_EPSILON;
        if (entering) phaseWindow = PHASE_WINDOW;

        double excess = Math.max(0.0, entry);
        boolean embeddedWhileMoving = phaseWindow > 0 && observedSpeed > PHASE_MOVE_EPS
                && gaps.embedded() > PHASE_EMBED_TOLERANCE;
        if (embeddedWhileMoving) {
            phaseWindow = PHASE_WINDOW;
            excess = Math.max(excess, gaps.embedded() - PHASE_EMBED_TOLERANCE);
        } else if (phaseWindow > 0) {
            phaseWindow--;
        }
        return excess;
    }

    private boolean trusted(MovementData movement) {
        if (!movement.isLastFlyingPositionChanged()) return false;
        boolean doubleMove = data.getPlayer().supportsEndTick()
                && flyingSinceTickEnd > 1
                && !data.getTeleportData().lastPacketWasTeleport();
        return !doubleMove;
    }

    private MovementCause preScanBail(Location current) {
        if (data.isCanFly()) return MovementCause.FLY;
        if (data.isInVehicle()) return MovementCause.VEHICLE;
        if (data.isSleeping()) return MovementCause.SLEEPING;
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
        inputResolver.suppressImproperSprint();
        double excess = Math.abs(observed.getY()) - GROUNDSPOOF_VERTICAL_EPS;
        result = new MovementResult(MovementCause.GROUNDSPOOF, observed, MotionArea.resting(),
                0.0, excess, false, true, false);
        seedCarried(observed);
    }

    private boolean handleFast(Vector3d observed) {
        double horizontal = Math.hypot(observed.getX(), observed.getZ());
        if (horizontal <= FAST_HORIZONTAL_CAP) {
            fastStreak = 0;
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
        flagFast(observed, horizontal);
        return true;
    }

    private void flagFast(Vector3d observed, double horizontal) {
        airborneWithholdStreak = 0;
        inputResolver.suppressImproperSprint();
        double excess = horizontal - FAST_HORIZONTAL_CAP;
        result = new MovementResult(MovementCause.FAST, observed, MotionArea.resting(),
                excess, 0.0, true, false, false);
        seedCarried(observed);
    }

    private MovementCause describeCause(BlockEnvironment env, MovementInput input, double observedVy) {
        if (env.fluid()) return MovementCause.FLUID;
        if (env.stuck()) return MovementCause.STUCK;
        if (env.climbable()) return MovementCause.CLIMB;
        if (env.bounceFactor() > 0.0 && input.groundedEnd()) return MovementCause.BOUNCE;
        if (input.levitation()) return MovementCause.LEVITATION;
        if (input.groundedEnd()) return observedVy > VERTICAL_HIT_EPSILON ? MovementCause.STEP : MovementCause.GROUND;
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
        inputResolver.onDecline();
        result = MovementResult.unpredictable(cause, observed);
        MovementDebug.log(data.getPlayer(), "decline:" + cause, observed, null, null, null, 0.0, 0.0);
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
        airborneWithholdStreak = 0;
        fastStreak = 0;
        groundMoveStreak = 0;
        bubbleTicks = 0;
        phaseWindow = 0;
        groundTracker.clearWindows();
        inputResolver.clear();
    }

    public boolean isImproperSprint() {
        return inputResolver.improperSprint();
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
        groundTracker.reset();
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
