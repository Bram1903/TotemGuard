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
import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.area.Range;
import com.deathmotion.totemguard.common.physics.fall.FallTracker;
import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.ground.GroundTracker;
import com.deathmotion.totemguard.common.physics.input.InputResolver;
import com.deathmotion.totemguard.common.physics.mitigation.MovementMitigation;
import com.deathmotion.totemguard.common.physics.phase.PhaseTracker;
import com.deathmotion.totemguard.common.physics.prescan.FastDetector;
import com.deathmotion.totemguard.common.physics.prescan.GroundSpoofDetector;
import com.deathmotion.totemguard.common.physics.sim.MovementInput;
import com.deathmotion.totemguard.common.physics.sim.MovementSimulator;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.ExternalVelocityData;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.data.WorldBorderData;
import com.deathmotion.totemguard.common.world.entity.EntityPush;
import com.deathmotion.totemguard.common.world.scan.BlockEnvironment;
import com.deathmotion.totemguard.common.world.scan.BlockEnvironmentScanner;
import com.deathmotion.totemguard.common.world.scan.WallGaps;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;

public class MovementEstimator {

    private static final double HIT_EPSILON = 0.002;
    private static final double VERTICAL_HIT_EPSILON = 0.003;
    private static final double STRONG_SINGLE_EXCESS = 0.40;

    private static final double HORIZONTAL_PAD = 0.003;
    private static final double VERTICAL_PAD = 0.002;

    private static final double AIR_DISK_PAD = 0.003;
    private static final double AIR_DIRECTION_MIN_SPEED = 0.04;

    private static final int WINDOW = 20;
    private static final long WINDOW_MASK = (1L << WINDOW) - 1;
    private static final int HITS_FOR_MOVED = 5;

    private static final double BORDER_MARGIN = 2.0;

    private static final double LANDING_REACH = 1.1;

    private static final double KNOCKBACK_PAD = 0.05;
    private static final double ENTITY_PUSH_PER = 0.08;
    private static final double MAX_ENTITY_PUSH = 0.30;
    private static final double EMBEDDED_PUSH = 0.1;
    private static final double WEDGE_CARRY = 0.25;
    private static final double WINDOW_ACCEL = 0.2;

    private static final int HOVER_TOLERANCE = 4;
    private static final double HOVER_EXCESS = MovementConstants.GRAVITY;
    private static final double HOVER_MIN_GAP = 0.9;
    private static final int HOVER_SETBACK_LIMIT = 2;
    private static final double JUMP_LIKE_ASCENT = 0.3;

    private static final int DOUBLE_MOVE_TOLERANCE = 3;
    private static final int TELEPORT_CARRY_GRACE = 3;
    private static final double CARRY_SLACK = 0.015;

    private static final int STEP_UNCERTAINTY_TICKS = 4;
    private static final double STEP_HORIZONTAL_SLACK = 0.15;

    private static final int BUBBLE_LAUNCH_TICKS = 5;

    private static final int FLUID_ENTRY_TICKS = 4;
    private static final double FLUID_ENTRY_ASCENT = 0.75;
    private static final double FLUID_DESCENT_SLACK = 0.02;
    private static final int FLUID_WALL_TICKS = 5;
    private static final double WATER_BUMP_ASCENT = 0.34;
    private static final int STUCK_ARREST_TICKS = 2;

    private final Data data;
    private final GroundTracker groundTracker = new GroundTracker();
    private final InputResolver inputResolver;
    private final MovementMitigation mitigation;
    private final FallTracker fallTracker;
    private final PhaseTracker phaseTracker = new PhaseTracker();
    private final FastDetector fastDetector = new FastDetector();
    private final GroundSpoofDetector groundSpoofDetector = new GroundSpoofDetector();
    private final ResidualTracker residualTracker = new ResidualTracker();

    private boolean initialized;
    private MotionArea carried = MotionArea.resting();
    private SimulationTolerance tolerance = SimulationTolerance.STRICT;

    private long hitWindow;
    private boolean movedSticky;
    private GameMode lastGameMode;
    private int flyingSinceTickEnd;
    private int doubleMoveStreak;
    private int airborneWithholdStreak;
    private int hoverSetbackStreak;
    private int teleportCarryGrace;
    private int stepMoveTicks;
    private int bubbleTicks;
    private double bubbleAscentCap;
    private int fluidEntryTicks;
    private int fluidWallTicks;
    private int stuckArrestTicks;
    private boolean airborneLastTick;
    private double airVelocityX;
    private double airVelocityZ;

    @Getter
    private MovementResult result = MovementResult.INITIAL;

    public MovementEstimator(Data data) {
        this.data = data;
        this.inputResolver = new InputResolver(data);
        this.mitigation = new MovementMitigation(data);
        this.fallTracker = new FallTracker(data);
    }

    // A tick-end without any flying packet means the client ran a full tick and asserts, by
    // omission, that it did not move. An airborne vanilla client always moves (gravity alone
    // exceeds the send threshold), so judging these silent ticks is what catches a cheat that
    // freezes in midair without sending anything. Keepalive-driven flying (once per second)
    // is far too slow for that, and only the tick-end packet proves a client tick actually
    // ran (a lagging client sends neither movement nor tick-ends, so it is never misjudged).
    public void onTickEnd() {
        boolean sawFlying = flyingSinceTickEnd > 0;
        flyingSinceTickEnd = 0;
        mitigation.clearTickFlags();
        if (!sawFlying) onSilentTick();
    }

    private void onSilentTick() {
        if (!initialized) return;
        if (data.isDead()) return;
        ConfigView view = TGPlatform.getInstance().getConfigRepository().configView();
        if (!view.physicsEngineEnabled()) return;
        if (data.getTeleportData().hasPendingTeleport()) return;

        MovementData movement = data.getMovementData();
        if (!movement.isCameraIsSelf()) return;
        Location current = movement.getCurrent();
        if (preScanBail(current) != null) return;

        BlockEnvironment env = BlockEnvironmentScanner.scan(
                data.getClientWorld(), data.getWorldEntityData(), current, current,
                data.getAttributeData().width(), poseHeight(), data.getAttributeData().stepHeight(), data.isSneaking(),
                phaseTracker.exemptCells());
        if (!env.feetLoaded()) return;

        GroundState ground = groundTracker.resolve(
                0.0, env, data.getAttributeData().stepHeight(), carried.vertical().min(), data.isSneaking());
        double bubbleAscent = bubbleTicks > 0 ? bubbleAscentCap : 0.0;
        MovementInput input = inputResolver.build(movement, env, Vector3d.zero(), ground, bubbleAscent);

        coastCarried(input, env);
        shiftWindow(false);
        movedSticky = Long.bitCount(hitWindow) >= HITS_FOR_MOVED;

        boolean landMedium = !env.fluid() && !env.stuck() && !env.climbable();
        boolean airborne = landMedium && !ground.groundedEnd()
                && env.groundGap() > HOVER_MIN_GAP && !env.startOverlapping();
        airborneWithholdStreak = airborne ? airborneWithholdStreak + 1 : 0;
        if (airborneWithholdStreak > HOVER_TOLERANCE && hoverSetbackStreak < HOVER_SETBACK_LIMIT) {
            result = new MovementResult(MovementCause.HOVER, Vector3d.zero(), MotionArea.resting(),
                    0.0, HOVER_EXCESS, false, true, false);
        } else {
            result = MovementResult.unpredictable(MovementCause.WITHHELD, Vector3d.zero());
        }
        if (airborne) {
            MovementDebug.log(data.getPlayer(), "coast:silent", Vector3d.zero(), input, env, null, 0.0,
                    airborneWithholdStreak > HOVER_TOLERANCE ? HOVER_EXCESS : 0.0);
        }

        mitigation.observe(result, view);
        if (result.cause() == MovementCause.HOVER && mitigation.setbackIssuedThisTick()) {
            hoverSetbackStreak++;
        }
        data.getExternalVelocityData().tick();
        data.getPistonData().tick();
        data.getEffectData().tick();
    }

    private void disengage() {
        mitigation.reset();
        fallTracker.reset();
        if (!initialized) return;
        initialized = false;
        carried = MotionArea.resting();
        clearExcess();
        result = MovementResult.INITIAL;
    }

    public void onFlying() {
        ConfigView view = TGPlatform.getInstance().getConfigRepository().configView();
        if (!view.physicsEngineEnabled()) {
            disengage();
            return;
        }
        tolerance = view.physicsEngineTolerance();

        final MovementData movement = data.getMovementData();
        data.getWorldEntityData().advanceInterpolation();

        final Location current = movement.getCurrent();
        final Location previous = movement.getPrevious();
        final Vector3d observed = new Vector3d(
                current.getX() - previous.getX(),
                current.getY() - previous.getY(),
                current.getZ() - previous.getZ());
        if (!movement.isLastFlyingWasTeleportResync() && flyingSinceTickEnd < 1000) flyingSinceTickEnd++;
        phaseTracker.prune(current, data.getAttributeData().width() / 2.0, poseHeight());

        GameMode gameMode = data.getGameMode();
        if (gameMode != lastGameMode) {
            lastGameMode = gameMode;
            if (initialized) {
                phaseTracker.exemptEmbeddedCells(data.getClientWorld(), current, data.getAttributeData().width() / 2.0, poseHeight(), data.isSneaking());
                phaseTracker.seedGrace();
                phaseTracker.invalidateEmbed();
            }
        }

        BlockEnvironment env = null;
        GroundState ground = null;
        try {
            if (!initialized) {
                initialized = true;
                groundTracker.seed(movement.isOnGround());
                seedCarried(observed);
                result = MovementResult.INITIAL;
                clearExcess();
                phaseTracker.seedGrace();
                phaseTracker.exemptEmbeddedCells(data.getClientWorld(), current, data.getAttributeData().width() / 2.0, poseHeight(), data.isSneaking());
                return;
            }

            if (movement.isLastFlyingWasResync()) {
                if (movement.isLastFlyingWasTeleportResync()) {
                    if (movement.isLastFlyingTeleportVelocityReset()) {
                        declineRested(MovementCause.RESYNC, observed);
                    } else {
                        coastFrozenMomentum();
                        teleportCarryGrace = TELEPORT_CARRY_GRACE;
                        declineCarried(MovementCause.RESYNC, observed);
                    }
                } else {
                    decline(MovementCause.RESYNC, observed);
                }
                return;
            }

            if (data.getTeleportData().hasPendingTeleport()) {
                if (movement.hasPendingVelocityPreservingTeleport()) {
                    coastFrozenMomentum();
                    declineCarried(MovementCause.TELEPORT, observed);
                } else {
                    decline(MovementCause.TELEPORT, observed);
                }
                return;
            }

            MovementCause bail = preScanBail(current);
            if (bail != null) {
                decline(bail, observed);
                return;
            }

            if (handleGroundSpoof(observed, movement)) return;

            if (handleFast(observed)) return;

            env = BlockEnvironmentScanner.scan(
                    data.getClientWorld(), data.getWorldEntityData(), current, previous,
                    data.getAttributeData().width(), poseHeight(), data.getAttributeData().stepHeight(), data.isSneaking(),
                    phaseTracker.exemptCells());
            if (!env.feetLoaded()) {
                decline(MovementCause.UNLOADED, observed);
                return;
            }

            ground = groundTracker.resolve(
                    observed.getY(), env, data.getAttributeData().stepHeight(), carried.vertical().min(), data.isSneaking());

            if (env.bubbleAscent() > 0.0) {
                bubbleTicks = BUBBLE_LAUNCH_TICKS;
                bubbleAscentCap = env.bubbleAscent();
            } else if (bubbleTicks > 0) {
                bubbleTicks--;
            }
            double bubbleAscent = bubbleTicks > 0 ? bubbleAscentCap : 0.0;

            if (env.fluid()) {
                fluidEntryTicks = ground.wasFluid() ? Math.max(0, fluidEntryTicks - 1) : FLUID_ENTRY_TICKS;
                fluidWallTicks = env.horizontalObstacle() ? FLUID_WALL_TICKS : Math.max(0, fluidWallTicks - 1);
            } else {
                fluidEntryTicks = 0;
                fluidWallTicks = 0;
            }
            stuckArrestTicks = env.stuckSwept() ? STUCK_ARREST_TICKS : Math.max(0, stuckArrestTicks - 1);

            MovementInput input = inputResolver.build(movement, env, observed, ground, bubbleAscent);

            if (trusted(movement)) {
                doubleMoveStreak = 0;
                judge(MovementSimulator.predictMove(carried, input, env), input, env, observed);
            } else if (!movement.isLastFlyingPositionChanged() || ++doubleMoveStreak <= DOUBLE_MOVE_TOLERANCE) {
                coastCarried(input, env);
                boolean withheld = !movement.isLastFlyingPositionChanged();
                boolean landMedium = !env.fluid() && !env.stuck() && !env.climbable();
                boolean airborne = landMedium && !ground.groundedEnd()
                        && env.groundGap() > HOVER_MIN_GAP && !env.startOverlapping();
                airborneWithholdStreak = (withheld && airborne) ? airborneWithholdStreak + 1 : 0;
                double wallExcess = 0.0;
                if (!withheld && landMedium) {
                    wallExcess = phaseTracker.excess(new WallGaps(0.0, env.wallGaps().embedded()),
                            Math.hypot(observed.getX(), observed.getZ()), false, tolerance.padScale());
                }
                shiftWindow(wallExcess > HIT_EPSILON);
                movedSticky = Long.bitCount(hitWindow) >= HITS_FOR_MOVED;
                if (wallExcess > HIT_EPSILON) {
                    result = new MovementResult(MovementCause.PHASE, observed, null,
                            wallExcess, 0.0, true, false, false);
                } else if (airborneWithholdStreak > HOVER_TOLERANCE && hoverSetbackStreak < HOVER_SETBACK_LIMIT) {
                    result = new MovementResult(MovementCause.HOVER, observed, MotionArea.resting(),
                            0.0, HOVER_EXCESS, false, true, false);
                } else {
                    result = MovementResult.unpredictable(
                            withheld ? MovementCause.WITHHELD : MovementCause.DOUBLE_MOVE, observed);
                }
                MovementDebug.log(data.getPlayer(), withheld ? "coast:withheld" : "coast:double",
                        observed, input, env, null, wallExcess, 0.0);
            } else {
                judge(MovementSimulator.predictMove(carried, input, env), input, env, observed);
            }

            if (ground.bounced()) {
                carried = MovementSimulator.advanceBounce(carried, ground.bounceFloor(), env.bounceFactor(), input);
            }
        } finally {
            fallTracker.observe(result, movement.isOnGround(), env, ground, view);
            mitigation.observe(result, view);
            if (result.cause() == MovementCause.HOVER && mitigation.setbackIssuedThisTick()) {
                hoverSetbackStreak++;
            }
            data.getExternalVelocityData().tick();
            data.getPistonData().tick();
            data.getEffectData().tick();
        }
    }

    private void judge(MotionArea move, MovementInput input, BlockEnvironment env, Vector3d observed) {
        airborneWithholdStreak = 0;
        hoverSetbackStreak = 0;
        fastDetector.reset();
        double observedSpeed = Math.hypot(observed.getX(), observed.getZ());
        double observedVy = observed.getY();

        boolean steppedUp = observedVy > VERTICAL_HIT_EPSILON && (input.groundedEnd() || input.supportWithinStep());
        if (steppedUp) stepMoveTicks = STEP_UNCERTAINTY_TICKS;

        boolean landMedium = !env.fluid() && !env.climbable() && !env.stuck();

        boolean fluidEntry = env.fluid() && fluidEntryTicks > 0;
        if (fluidEntry) {
            move = new MotionArea(move.horizontalSpeed(), move.vertical().raiseCeiling(FLUID_ENTRY_ASCENT));
        } else if (env.fluid() && fluidWallTicks > 0) {
            move = new MotionArea(move.horizontalSpeed(), move.vertical().raiseCeiling(WATER_BUMP_ASCENT));
        }
        if (landMedium && stuckArrestTicks > 0) {
            move = new MotionArea(move.horizontalSpeed(), move.vertical().raiseCeiling(0.0));
        }

        double horizontalPad = HORIZONTAL_PAD * tolerance.padScale();
        if (env.startEmbedded()) horizontalPad += EMBEDDED_PUSH;
        double verticalPad = VERTICAL_PAD * tolerance.padScale();
        MotionArea allowed = move.expand(horizontalPad, verticalPad);
        double horizontalExcess = allowed.horizontalExcess(observedSpeed);
        double verticalExcess = allowed.ascentExcess(observedVy);
        double wallExcess = 0.0;
        if (landMedium) {
            wallExcess = phaseTracker.excess(env.wallGaps(), observedSpeed, true, tolerance.padScale());
        } else {
            phaseTracker.invalidateEmbed();
        }

        boolean knockbackConsumed = false;
        ExternalVelocityData external = data.getExternalVelocityData();
        if (external.isActive() && (horizontalExcess > HIT_EPSILON || verticalExcess > VERTICAL_HIT_EPSILON)) {
            double knockbackAlong = observedSpeed > HIT_EPSILON
                    ? Math.max(0.0, (external.x() * observed.getX() + external.z() * observed.getZ()) / observedSpeed)
                    : Math.hypot(external.x(), external.z());
            MotionArea widened = allowed.expand(
                    knockbackAlong + KNOCKBACK_PAD,
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
            double slack = verticalPad;
            if (external.isActive()) slack += Math.max(0.0, -external.y()) + KNOCKBACK_PAD;
            if (input.groundedEnd()) slack += input.stepHeight();
            double descentExcess = (descentFloor - slack) - observedVy;
            if (descentExcess > verticalExcess) {
                verticalExcess = descentExcess;
                fellTooFast = true;
            }
        } else if (env.fluid() && !fluidEntry) {
            double slack = verticalPad + FLUID_DESCENT_SLACK;
            if (external.isActive()) slack += Math.max(0.0, -external.y()) + KNOCKBACK_PAD;
            double descentExcess = (move.vertical().min() - slack) - observedVy;
            if (descentExcess > verticalExcess) verticalExcess = descentExcess;
        }

        if (!fellTooFast && verticalExcess > VERTICAL_HIT_EPSILON && observedVy <= -VERTICAL_HIT_EPSILON
                && landMedium && env.groundGap() <= LANDING_REACH) {
            verticalExcess = 0.0;
        }

        boolean phased = wallExcess > HIT_EPSILON;
        if (phased && wallExcess > horizontalExcess) horizontalExcess = wallExcess;

        boolean pureAir = landMedium && !input.groundedStart() && !input.groundedEnd()
                && !input.groundedStartAmbiguous() && !env.startOverlapping();
        boolean airStrafed = false;
        if (airborneLastTick && pureAir && !steppedUp && teleportCarryGrace == 0
                && !external.isActive() && bubbleTicks == 0 && observedSpeed >= AIR_DIRECTION_MIN_SPEED) {
            double centerX = airVelocityX;
            double centerZ = airVelocityZ;
            double reach = MovementConstants.AIR_ACCEL_SPRINTING + AIR_DISK_PAD * tolerance.padScale();
            double deviation = Math.hypot(outwardResidual(observed.getX(), centerX), outwardResidual(observed.getZ(), centerZ));
            double directionExcess = deviation - reach;
            if (directionExcess > HIT_EPSILON) {
                directionExcess -= nearbyEntityPush(observed);
                if (directionExcess > HIT_EPSILON && directionExcess > horizontalExcess) {
                    horizontalExcess = directionExcess;
                    airStrafed = true;
                }
            }
        }

        boolean residualClean = !external.isActive() && !steppedUp && teleportCarryGrace == 0
                && bubbleTicks == 0 && !env.startOverlapping() && !env.startEmbedded()
                && !fluidEntry && fluidWallTicks == 0 && stuckArrestTicks == 0;

        boolean movedThisTick = horizontalExcess > HIT_EPSILON;
        boolean ascendingThisTick = verticalExcess > VERTICAL_HIT_EPSILON;
        shiftWindow(movedThisTick);
        movedSticky = horizontalExcess >= STRONG_SINGLE_EXCESS || Long.bitCount(hitWindow) >= HITS_FOR_MOVED;

        if (knockbackConsumed) external.consume();
        if (stepMoveTicks > 0) stepMoveTicks--;

        boolean sneakEdgeClamp = data.isSneaking() && input.groundedStart() && landMedium
                && observedVy <= 0.0
                && observedSpeed < move.horizontalSpeed().max() - HIT_EPSILON
                && (env.groundGap() > HIT_EPSILON || ledgeInMoveDirection(observed, observedSpeed, move));
        boolean carryPredicted = steppedUp || teleportCarryGrace > 0;
        if (teleportCarryGrace > 0) teleportCarryGrace--;
        double legalSpeed = carryPredicted || sneakEdgeClamp
                ? move.horizontalSpeed().max()
                : Math.min(observedSpeed, allowed.horizontalSpeed().max() + CARRY_SLACK * tolerance.padScale());
        if (env.startOverlapping()) legalSpeed = Math.max(legalSpeed, WEDGE_CARRY);
        double legalVy = Math.min(observedVy, allowed.vertical().max());
        carried = MovementSimulator.advance(legalSpeed, legalVy, input, env);

        if (pureAir && !carryPredicted && observedSpeed >= AIR_DIRECTION_MIN_SPEED) {
            double advancedScale = carried.horizontalSpeed().max() / observedSpeed;
            airVelocityX = observed.getX() * advancedScale;
            airVelocityZ = observed.getZ() * advancedScale;
            airborneLastTick = true;
        } else {
            airborneLastTick = false;
        }

        MovementCause cause;
        if (phased && horizontalExcess >= verticalExcess) {
            cause = MovementCause.PHASE;
        } else if (fellTooFast) {
            cause = MovementCause.FAST_FALL;
        } else if (data.isOpenInventory()) {
            cause = MovementCause.INVENTORY_MOVE;
        } else if (airStrafed && horizontalExcess >= verticalExcess) {
            cause = MovementCause.AIR_STRAFE;
        } else {
            cause = describeCause(env, input, observedVy);
        }
        result = new MovementResult(cause, observed, move,
                horizontalExcess, verticalExcess, movedThisTick, ascendingThisTick, knockbackConsumed);
        MovementDebug.log(data.getPlayer(), "judge:" + cause, observed, input, env, move, horizontalExcess, verticalExcess);

        if (residualClean) {
            double residFloor = env.fluid() ? move.vertical().min() : descentFloor;
            residualTracker.observe(data.getPlayer(), cause, env, input, observed,
                    observedVy - move.vertical().max(), residFloor - observedVy,
                    observedSpeed - move.horizontalSpeed().max());
        }
    }

    private boolean trusted(MovementData movement) {
        if (!movement.isLastFlyingPositionChanged()) return movement.isLastFlyingWasDuplicate();
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
        if (!groundSpoofDetector.provoked(movement.isOnGround(), observed.getY())) return false;
        flagGroundSpoof(observed);
        return true;
    }

    private void flagGroundSpoof(Vector3d observed) {
        airborneWithholdStreak = 0;
        inputResolver.improperSprint(false);
        double excess = Math.abs(observed.getY()) - GroundSpoofDetector.VERTICAL_EPS;
        result = new MovementResult(MovementCause.GROUNDSPOOF, observed, MotionArea.resting(),
                0.0, excess, false, true, false);
        seedCarried(observed);
    }

    private boolean handleFast(Vector3d observed) {
        double horizontal = Math.hypot(observed.getX(), observed.getZ());
        FastDetector.Outcome outcome = fastDetector.evaluate(horizontal, data.getExternalVelocityData().isActive());
        if (outcome == FastDetector.Outcome.NONE) return false;
        if (outcome == FastDetector.Outcome.DECLINE) {
            decline(MovementCause.FAST, observed);
        } else {
            flagFast(observed, horizontal);
        }
        return true;
    }

    private void flagFast(Vector3d observed, double horizontal) {
        airborneWithholdStreak = 0;
        inputResolver.improperSprint(false);
        double excess = horizontal - FastDetector.HORIZONTAL_CAP;
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
        int count = EntityPush.countPushableNear(data.getWorldEntityData().tracked(),
                Math.min(prevX, current.getX()), Math.min(prevY, current.getY()), Math.min(prevZ, current.getZ()),
                Math.max(prevX, current.getX()), Math.max(prevY, current.getY()), Math.max(prevZ, current.getZ()),
                data.getAttributeData().width() / 2.0, poseHeight());
        return Math.min(MAX_ENTITY_PUSH, count * ENTITY_PUSH_PER);
    }

    private boolean ledgeInMoveDirection(Vector3d observed, double observedSpeed, MotionArea move) {
        if (observedSpeed <= HIT_EPSILON) return false;
        double width = data.getAttributeData().width();
        double reach = Math.max(move.horizontalSpeed().max(), width);
        double scale = reach / observedSpeed;
        return BlockEnvironmentScanner.ledgeInDirection(
                data.getClientWorld(), data.getMovementData().getCurrent(),
                width, data.getAttributeData().stepHeight(), data.isSneaking(),
                observed.getX() * scale, observed.getZ() * scale);
    }

    private static double outwardResidual(double observed, double center) {
        if (center > 0.0) return observed < 0.0 ? -observed : Math.max(0.0, observed - center);
        if (center < 0.0) return observed > 0.0 ? observed : Math.max(0.0, center - observed);
        return Math.abs(observed);
    }

    private double poseHeight() {
        double base = data.isSneaking() ? MovementConstants.SNEAKING_HEIGHT : MovementConstants.STANDING_HEIGHT;
        return base * data.getAttributeData().scale();
    }

    public void onBlockChangeApplied(int x, int y, int z, int blockId) {
        if (!initialized) return;
        phaseTracker.onBlockChangeApplied(data.getClientWorld(), data.getMovementData().getCurrent(),
                data.getAttributeData().width() / 2.0, poseHeight(), data.isSneaking(), x, y, z, blockId);
    }

    private void coastCarried(MovementInput input, BlockEnvironment env) {
        MotionArea grown = MovementSimulator.predictMove(carried, input, env);
        double coastHorizontal = grown.horizontalSpeed().max();
        MotionArea ceilingAdvance = MovementSimulator.advance(coastHorizontal, grown.vertical().max(), input, env);
        MotionArea floorAdvance = MovementSimulator.advance(coastHorizontal, carried.vertical().min(), input, env);
        carried = new MotionArea(ceilingAdvance.horizontalSpeed(),
                new Range(floorAdvance.vertical().min(), ceilingAdvance.vertical().max()));
        airborneLastTick = false;
    }

    private void coastFrozenMomentum() {
        double gravity = data.getAttributeData().gravity();
        Range horizontal = carried.horizontalSpeed().grow(0.0, WINDOW_ACCEL);
        Range vertical = carried.vertical();
        double floor = gravity > 0.0
                ? (vertical.min() - gravity) * MovementConstants.VERTICAL_DRAG
                : vertical.min();
        double jumpCeiling = data.getAttributeData().jumpStrength()
                + (data.getEffectData().hasJumpBoost()
                ? 0.1 * (data.getEffectData().jumpBoostAmplifier() + 1) : 0.0);
        carried = new MotionArea(horizontal, new Range(floor, Math.max(vertical.max(), jumpCeiling)));
    }

    private void decline(MovementCause cause, Vector3d observed) {
        seedCarried(observed);
        declineCarried(cause, observed);
    }

    private void declineRested(MovementCause cause, Vector3d observed) {
        double jumpCeiling = data.getAttributeData().jumpStrength()
                + (data.getEffectData().hasJumpBoost()
                ? 0.1 * (data.getEffectData().jumpBoostAmplifier() + 1) : 0.0);
        carried = new MotionArea(Range.ZERO, new Range(0.0, jumpCeiling));
        declineCarried(cause, observed);
    }

    private void declineCarried(MovementCause cause, Vector3d observed) {
        shiftWindow(false);
        movedSticky = Long.bitCount(hitWindow) >= HITS_FOR_MOVED;
        airborneWithholdStreak = 0;
        airborneLastTick = false;
        doubleMoveStreak = 0;
        inputResolver.onDecline();
        if (cause != MovementCause.FAST) {
            phaseTracker.exemptEmbeddedCells(data.getClientWorld(), data.getMovementData().getCurrent(),
                    data.getAttributeData().width() / 2.0, poseHeight(), data.isSneaking());
        }
        if (cause == MovementCause.RESYNC || cause == MovementCause.TELEPORT
                || cause == MovementCause.PISTON || cause == MovementCause.UNLOADED) {
            phaseTracker.seedGrace();
            phaseTracker.invalidateEmbed();
        }
        if (cause == MovementCause.RESYNC || cause == MovementCause.TELEPORT) {
            groundTracker.displaced();
        }
        result = MovementResult.unpredictable(cause, observed);
        MovementDebug.log(data.getPlayer(), "decline:" + cause, observed, null, null, null, 0.0, 0.0);
    }

    private void seedCarried(Vector3d observed) {
        carried = MotionArea.of(Math.hypot(observed.getX(), observed.getZ()), observed.getY());
        airborneLastTick = false;
    }

    private void shiftWindow(boolean hit) {
        hitWindow = ((hitWindow << 1) | (hit ? 1L : 0L)) & WINDOW_MASK;
    }

    private void clearExcess() {
        hitWindow = 0;
        movedSticky = false;
        airborneWithholdStreak = 0;
        hoverSetbackStreak = 0;
        teleportCarryGrace = 0;
        doubleMoveStreak = 0;
        bubbleTicks = 0;
        fluidEntryTicks = 0;
        fluidWallTicks = 0;
        stuckArrestTicks = 0;
        airborneLastTick = false;
        groundTracker.clearWindows();
        inputResolver.clear();
        fastDetector.reset();
        groundSpoofDetector.reset();
        phaseTracker.clear();
    }

    public boolean isImproperSprint() {
        return inputResolver.improperSprint();
    }

    public boolean mitigationTriggeredThisTick() {
        return mitigation.triggeredThisTick();
    }

    public boolean setbackIssuedThisTick() {
        return mitigation.setbackIssuedThisTick();
    }

    public boolean setbackSkippedThisTick() {
        return mitigation.setbackSkippedThisTick();
    }

    public boolean fallViolationThisTick() {
        return fallTracker.violationThisTick();
    }

    public double fallAvoidedDamage() {
        return fallTracker.avoidedDamage();
    }

    public double fallDistance() {
        return fallTracker.fallDistance();
    }

    public boolean fallDamageApplied() {
        return fallTracker.damageApplied();
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
        mitigation.reset();
        fallTracker.reset();
        residualTracker.reset();
        data.getExternalVelocityData().reset();
        data.getPistonData().reset();
        data.getEffectData().reset();
    }

    public void clearHistory() {
        clearExcess();
        result = MovementResult.INITIAL;
    }
}
