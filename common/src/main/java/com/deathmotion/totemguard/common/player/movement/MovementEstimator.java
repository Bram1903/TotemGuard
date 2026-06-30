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

import com.deathmotion.totemguard.common.player.data.*;
import com.deathmotion.totemguard.common.util.BoundingBox;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.Getter;

public class MovementEstimator {

    private static final double FRICTION = MovementConstants.MAX_HORIZONTAL_FRICTION;
    private static final double ENVELOPE_PAD = 0.010;
    private static final double HIT_EPSILON = 0.002;
    private static final double FAST_MOVEMENT_CAP = 10.0;
    private static final double BORDER_MARGIN = 2.0;

    private static final double DEFAULT_SLIPPERINESS = 0.6;
    private static final double ICE_SLIPPERINESS = 0.98;
    private static final double BLUE_ICE_SLIPPERINESS = 0.989;
    private static final double SLIME_SLIPPERINESS = 0.8;
    private static final double FLUID_PUSH_PAD = 0.014;
    private static final double SUPPORT_BLOCK_OFFSET = 0.5000001;

    private static final int WINDOW = 20;
    private static final long WINDOW_MASK = (1L << WINDOW) - 1;
    private static final int HITS_FOR_MOVED = 5;
    private static final double STRONG_SINGLE_EXCESS = 0.40;

    private static final double ENTITY_PUSH_PER = 0.08;
    private static final double MAX_ENTITY_PUSH = 0.30;

    private static final double KNOCKBACK_PAD = 0.05;

    private static final double GRAVITY = MovementConstants.GRAVITY;
    private static final double SLOW_FALLING_GRAVITY = MovementConstants.SLOW_FALLING_GRAVITY;
    private static final double VERTICAL_DRAG = MovementConstants.VERTICAL_DRAG;
    private static final double STEP_HEIGHT = MovementConstants.STEP_HEIGHT;
    private static final double VERTICAL_PAD = 0.025;
    private static final double VERTICAL_HIT_EPSILON = 0.003;
    private static final double FAST_VERTICAL_CAP = 1.5;
    private static final double ONGROUND_RISE_TOLERANCE = 0.1;

    public enum VerticalCause {
        NONE,
        GROUND,
        STEP,
        AIR,
        LEVITATION,
        FLUID,
        CLIMB,
        STUCK,
        BOUNCE,
        PISTON,
        VEHICLE,
        FLY,
        FAST,
        UNLOADED
    }

    private final Data data;

    private boolean initialized;
    private double prevVelX;
    private double prevVelY;
    private double prevVelZ;

    private long hitWindow;

    @Getter
    private MovementResult result = MovementResult.UNKNOWN;
    @Getter
    private double lastExcess;
    @Getter
    private boolean movedThisTick;

    @Getter
    private boolean ascendingThisTick;
    private boolean supportedByGround;
    @Getter
    private double lastVerticalExcess;
    @Getter
    private VerticalCause verticalCause = VerticalCause.NONE;

    public MovementEstimator(Data data) {
        this.data = data;
    }

    private static double horizontalExcess(Interval areaX, Interval areaZ, double obsVelX, double obsVelZ) {
        return Math.hypot(areaX.distanceOutside(obsVelX), areaZ.distanceOutside(obsVelZ));
    }

    private static boolean isBouncy(StateType type) {
        return type == StateTypes.SLIME_BLOCK || type.getName().endsWith("_BED");
    }

    private static double slipperiness(StateType type) {
        if (type == StateTypes.ICE || type == StateTypes.PACKED_ICE || type == StateTypes.FROSTED_ICE) {
            return ICE_SLIPPERINESS;
        }
        if (type == StateTypes.BLUE_ICE) return BLUE_ICE_SLIPPERINESS;
        if (type == StateTypes.SLIME_BLOCK) return SLIME_SLIPPERINESS;
        return DEFAULT_SLIPPERINESS;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public boolean movedHorizontally() {
        return result == MovementResult.MOVED;
    }

    public int windowHits() {
        return Long.bitCount(hitWindow);
    }

    public int hitsForMoved() {
        return HITS_FOR_MOVED;
    }

    public void onFlying() {
        final MovementData movement = data.getMovementData();
        final ExternalVelocityData external = data.getExternalVelocityData();

        data.getWorldEntityData().advanceInterpolation();

        final Location current = movement.getCurrent();
        final Location previous = movement.getPrevious();

        final double obsVelX = current.getX() - previous.getX();
        final double obsVelY = current.getY() - previous.getY();
        final double obsVelZ = current.getZ() - previous.getZ();

        try {
            if (!initialized) {
                anchor(obsVelX, obsVelY, obsVelZ);
                initialized = true;
                movedThisTick = false;
                result = MovementResult.UNKNOWN;
                clearVertical();
                return;
            }

            if (shouldBail(current, obsVelX, obsVelZ)) {
                anchor(obsVelX, obsVelY, obsVelZ);
                shiftWindow(false);
                movedThisTick = false;
                result = MovementResult.UNKNOWN;
                clearVertical();
                return;
            }

            if (movement.isLastFlyingWasResync()) {
                anchor(0.0, 0.0, 0.0);
                shiftWindow(false);
                movedThisTick = false;
                result = MovementResult.EXTERNAL;
                clearVertical();
                return;
            }

            boolean inFluid = inFluidSwept(current, previous);
            double friction = (inFluid || !movement.isLastOnGround())
                    ? FRICTION
                    : Math.max(groundFriction(current), groundFriction(previous));

            Interval baseX = Interval.ZERO.hull(prevVelX * friction).expand(ENVELOPE_PAD);
            Interval baseZ = Interval.ZERO.hull(prevVelZ * friction).expand(ENVELOPE_PAD);
            if (inFluid) {
                baseX = baseX.expand(FLUID_PUSH_PAD);
                baseZ = baseZ.expand(FLUID_PUSH_PAD);
            }

            double excess = horizontalExcess(baseX, baseZ, obsVelX, obsVelZ);

            Interval envX = baseX;
            Interval envZ = baseZ;
            boolean kbConsumed = false;
            if (external.isActive() && excess > HIT_EPSILON) {
                Interval kbX = baseX.shift(external.x()).expand(KNOCKBACK_PAD);
                Interval kbZ = baseZ.shift(external.z()).expand(KNOCKBACK_PAD);
                double kbExcess = horizontalExcess(kbX, kbZ, obsVelX, obsVelZ);
                if (kbExcess < excess) {
                    excess = kbExcess;
                    envX = kbX;
                    envZ = kbZ;
                    if (kbExcess <= HIT_EPSILON) kbConsumed = true;
                }
            }

            if (excess > HIT_EPSILON) {
                double entityPush = nearbyEntityPush(previous, current);
                if (entityPush > 0.0) {
                    excess = Math.min(excess, horizontalExcess(envX.expand(entityPush), envZ.expand(entityPush), obsVelX, obsVelZ));
                }
            }

            lastExcess = excess;
            boolean groundTransition = movement.isOnGround() != movement.isLastOnGround();
            movedThisTick = !groundTransition && excess > HIT_EPSILON;
            shiftWindow(movedThisTick);

            int hits = Long.bitCount(hitWindow);
            if (excess >= STRONG_SINGLE_EXCESS || hits >= HITS_FOR_MOVED) {
                result = MovementResult.MOVED;
            } else if (movedThisTick) {
                result = MovementResult.UNKNOWN;
            } else {
                result = MovementResult.EXTERNAL;
            }

            estimateVertical(current, previous, obsVelY, inFluid);

            if (kbConsumed) external.consume();
            anchor(obsVelX, obsVelY, obsVelZ);
        } finally {
            external.tick();
            data.getPistonData().tick();
            data.getEffectData().tick();
        }
    }

    private void estimateVertical(Location current, Location previous, double obsVelY, boolean inFluid) {
        if (!feetChunkLoaded(current)) {
            supportedByGround = false;
            setVertical(false, 0.0, VerticalCause.UNLOADED);
            return;
        }

        supportedByGround = supportedByGround(current);

        VerticalCause bail = verticalBail(current, previous, obsVelY, inFluid);
        if (bail != null) {
            setVertical(false, 0.0, bail);
            return;
        }

        EffectData effects = data.getEffectData();
        boolean levitating = effects.hasLevitation();
        double predictedY = predictVerticalNoInput(levitating, effects);

        boolean groundClaim = data.getMovementData().isOnGround() && obsVelY <= ONGROUND_RISE_TOLERANCE;
        boolean grounded = supportedByGround || groundClaim;
        double base;
        VerticalCause cause;
        if (grounded) {
            base = STEP_HEIGHT;
            cause = obsVelY > VERTICAL_PAD ? VerticalCause.STEP : VerticalCause.GROUND;
        } else {
            base = predictedY;
            cause = levitating ? VerticalCause.LEVITATION : VerticalCause.AIR;
        }

        ExternalVelocityData external = data.getExternalVelocityData();
        double externalUp = external.isActive() ? Math.max(0.0, external.y()) + KNOCKBACK_PAD : 0.0;
        double upper = base + externalUp + VERTICAL_PAD;
        double excess = obsVelY - upper;

        boolean ascending = excess > VERTICAL_HIT_EPSILON;
        setVertical(ascending, Math.max(0.0, excess), cause);
    }

    private double predictVerticalNoInput(boolean levitating, EffectData effects) {
        if (levitating) {
            double target = MovementConstants.LEVITATION_PER_LEVEL * (effects.levitationAmplifier() + 1);
            return (prevVelY + (target - prevVelY) * MovementConstants.LEVITATION_RATE) * VERTICAL_DRAG;
        }
        double gravity = effects.hasSlowFalling() ? SLOW_FALLING_GRAVITY : GRAVITY;
        return (prevVelY - gravity) * VERTICAL_DRAG;
    }

    private VerticalCause verticalBail(Location current, Location previous, double obsVelY, boolean inFluid) {
        if (data.isCanFly()) return VerticalCause.FLY;
        if (data.isInVehicle()) return VerticalCause.VEHICLE;
        if (data.isSwimming() || data.isGliding() || data.isSpinAttacking()) return VerticalCause.VEHICLE;
        if (data.getPistonData().isActive()) return VerticalCause.PISTON;
        if (Math.abs(obsVelY) > FAST_VERTICAL_CAP) return VerticalCause.FAST;
        if (inFluid) return VerticalCause.FLUID;
        if (bouncyBelow(current)) return VerticalCause.BOUNCE;
        if (nearClimbable(current, previous)) return VerticalCause.CLIMB;
        if (inStuckBlock(current, previous)) return VerticalCause.STUCK;
        return null;
    }

    private boolean shouldBail(Location current, double obsVelX, double obsVelZ) {
        if (data.isInVehicle()) return true;
        if (data.isSwimming() || data.isGliding() || data.isSpinAttacking()) return true;
        if (data.getPistonData().isActive()) return true;
        if (Math.hypot(obsVelX, obsVelZ) > FAST_MOVEMENT_CAP) return true;
        WorldBorderData border = data.getWorldBorderData();
        return border.isActive() && border.distanceToEdge(current.getX(), current.getZ()) < BORDER_MARGIN;
    }

    private double poseHeight() {
        double base = data.isSneaking() ? MovementConstants.SNEAKING_HEIGHT : MovementConstants.STANDING_HEIGHT;
        return base * data.getAttributeData().scale();
    }

    private boolean inFluidSwept(Location current, Location previous) {
        BoundingBox box = BoundingBox.sweptPlayer(current, previous,
                data.getAttributeData().width(), poseHeight());
        return data.getClientWorld().hasBlock(box, MovementBlocks::isFluid);
    }

    private boolean nearClimbable(Location current, Location previous) {
        BoundingBox box = BoundingBox.sweptPlayer(current, previous,
                data.getAttributeData().width(), poseHeight());
        return data.getClientWorld().hasBlock(box, state -> MovementBlocks.isClimbable(state.getType()));
    }

    private boolean inStuckBlock(Location current, Location previous) {
        BoundingBox box = BoundingBox.sweptPlayer(current, previous,
                data.getAttributeData().width(), poseHeight());
        return data.getClientWorld().hasBlock(box, state -> MovementBlocks.isStuck(state.getType()));
    }

    private boolean feetChunkLoaded(Location feet) {
        return data.getClientWorld().isLoaded(floor(feet.getX()) >> 4, floor(feet.getZ()) >> 4);
    }

    private boolean supportedByGround(Location feet) {
        double half = data.getAttributeData().width() / 2.0;
        int y = floor(feet.getY() - SUPPORT_BLOCK_OFFSET);
        return isSupporting(feet.getX() - half, y, feet.getZ() - half)
                || isSupporting(feet.getX() + half, y, feet.getZ() - half)
                || isSupporting(feet.getX() - half, y, feet.getZ() + half)
                || isSupporting(feet.getX() + half, y, feet.getZ() + half)
                || isSupporting(feet.getX(), y, feet.getZ());
    }

    private boolean isSupporting(double x, int y, double z) {
        StateType type = data.getClientWorld().getBlockState(floor(x), y, floor(z)).getType();
        return MovementBlocks.isSolidSupport(type);
    }

    private boolean bouncyBelow(Location feet) {
        double half = data.getAttributeData().width() / 2.0;
        int y = floor(feet.getY() - SUPPORT_BLOCK_OFFSET);
        return isBouncyAt(feet.getX() - half, y, feet.getZ() - half)
                || isBouncyAt(feet.getX() + half, y, feet.getZ() - half)
                || isBouncyAt(feet.getX() - half, y, feet.getZ() + half)
                || isBouncyAt(feet.getX() + half, y, feet.getZ() + half)
                || isBouncyAt(feet.getX(), y, feet.getZ());
    }

    private boolean isBouncyAt(double x, int y, double z) {
        return isBouncy(data.getClientWorld().getBlockState(floor(x), y, floor(z)).getType());
    }

    private double groundFriction(Location feet) {
        int x = floor(feet.getX());
        int y = floor(feet.getY() - SUPPORT_BLOCK_OFFSET);
        int z = floor(feet.getZ());
        StateType type = data.getClientWorld().getBlockState(x, y, z).getType();
        if (type == StateTypes.AIR) return FRICTION;
        return slipperiness(type) * FRICTION;
    }

    private void shiftWindow(boolean hit) {
        hitWindow = ((hitWindow << 1) | (hit ? 1L : 0L)) & WINDOW_MASK;
    }

    private void anchor(double velX, double velY, double velZ) {
        prevVelX = velX;
        prevVelY = velY;
        prevVelZ = velZ;
    }

    private void setVertical(boolean ascending, double excess, VerticalCause cause) {
        ascendingThisTick = ascending;
        lastVerticalExcess = excess;
        verticalCause = cause;
    }

    private void clearVertical() {
        ascendingThisTick = false;
        supportedByGround = false;
        lastVerticalExcess = 0.0;
        verticalCause = VerticalCause.NONE;
    }

    private double nearbyEntityPush(Location previous, Location current) {
        PlayerAttributeData attributes = data.getAttributeData();
        double minX = Math.min(previous.getX(), current.getX());
        double maxX = Math.max(previous.getX(), current.getX());
        double minY = Math.min(previous.getY(), current.getY());
        double maxY = Math.max(previous.getY(), current.getY());
        double minZ = Math.min(previous.getZ(), current.getZ());
        double maxZ = Math.max(previous.getZ(), current.getZ());
        int count = data.getWorldEntityData().countPushableNear(
                minX, minY, minZ, maxX, maxY, maxZ,
                attributes.width() / 2.0, poseHeight());
        return Math.min(MAX_ENTITY_PUSH, count * ENTITY_PUSH_PER);
    }

    public void reset() {
        initialized = false;
        prevVelX = 0;
        prevVelY = 0;
        prevVelZ = 0;
        clearHistory();
        data.getExternalVelocityData().reset();
        data.getPistonData().reset();
        data.getEffectData().reset();
    }

    public void clearHistory() {
        hitWindow = 0;
        result = MovementResult.UNKNOWN;
        lastExcess = 0;
        movedThisTick = false;
        clearVertical();
    }
}
