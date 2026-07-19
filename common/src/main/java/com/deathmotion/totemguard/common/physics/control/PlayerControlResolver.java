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

package com.deathmotion.totemguard.common.physics.control;

import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.player.data.*;
import com.deathmotion.totemguard.common.util.ClientMath;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class PlayerControlResolver {

    private static final double RISE_EPS = 0.001;
    private static final double COYOTE_TAKEOFF_EPS = 0.05;

    private static final int SNEAK_CONFIRM = 3;
    private static final double SNEAK_DIAGONAL_FACTOR = Math.sqrt(2.0);

    private static final double SPRINT_MIN_SPEED = 0.15;
    private static final double SPRINT_BACKWARD_COS = -0.6;
    private static final int SPRINT_TURN_TOLERANCE = 8;
    private static final double SPRINT_GROUND_SLIPPERINESS = 0.61;

    private static final double SPRINT_JUMP_BOOST = 0.2;

    private static final double SPRINT_SPEED_MULTIPLIER = 1.3;
    private static final double SWIFT_SNEAK_PER_LEVEL = 0.15;
    private static final double FROST_SPEED_PENALTY = 0.05;
    private static final int FROST_FULL_TICKS = 140;
    private static final double WATER_FRICTION = 0.8;
    private static final double WATER_SPRINT_FRICTION = 0.9;
    private static final double WATER_DOLPHIN_FRICTION = 0.96;
    private static final double WATER_ACCEL = 0.02;
    private static final double WATER_EFFICIENCY_FRICTION_TARGET = 0.54600006;

    private final Data data;
    private final EngineActor actor;
    private final VersionGates gates;

    private int sneakStreak;
    private int backwardSprintStreak;
    @Getter
    @Setter
    private boolean improperSprint;
    private boolean lastClampedJump;
    private boolean lastWallContact = true;
    private double lastCeilingClearance = Double.MAX_VALUE;
    private boolean previousAirborneSprint;

    public PlayerControlResolver(Data data, EngineActor actor, VersionGates gates) {
        this.data = data;
        this.actor = actor;
        this.gates = gates;
    }

    public PlayerControl build(MovementData movement, ContactReport contact, GroundFacts ground,
                               boolean fluidNow, double observedX, double observedY, double observedZ,
                               boolean doubleMove, double stuckVertical, boolean wasInPowderSnow) {
        InputData.State state = data.getInputData().current();
        boolean inventoryOpen = data.isOpenInventory();
        boolean immobile = inventoryOpen || data.isSleeping();
        boolean horizontalInput = !immobile
                && (state == null || state.forward() || state.backward() || state.left() || state.right());
        boolean jumpHeld = state == null || state.jumping();

        EffectData effects = data.getEffectData();
        PlayerAttributeData attr = data.getAttributeData();
        int jumpBoostAmplifier = effects.hasJumpBoost() ? effects.jumpBoostAmplifier() : -1;
        double jumpBoost = jumpBoostAmplifier >= 0 ? 0.1 * (jumpBoostAmplifier + 1) : 0.0;
        double takeoffMin = attr.jumpStrength() * ground.startJumpMin() + jumpBoost;
        double takeoffMax = attr.jumpStrength() * ground.startJumpMax() + jumpBoost;

        double takeoffScale = Math.min(1.0, stuckVertical);
        boolean freshJump = observedY >= takeoffMin * takeoffScale - COYOTE_TAKEOFF_EPS;
        boolean landingJump = ground.landingSupport()
                && observedY > RISE_EPS && observedY <= takeoffMax + COYOTE_TAKEOFF_EPS;
        boolean coyoteJump = !ground.groundedStart()
                && jumpHeld && !immobile && !ground.coyoteBlocked()
                && (((ground.recentlyGrounded() || ground.startAmbiguous()) && freshJump) || landingJump);
        boolean effectiveGroundedStart = ground.groundedStart() || coyoteJump;
        boolean jumpPossible = effectiveGroundedStart && jumpHeld && !immobile;

        double observedSpeed = ClientMath.horizontalDistance(observedX, observedZ);
        boolean sprinting = !immobile && data.isSprinting() && data.getFoodData().canSprint()
                && sprintForward(movement, contact, state, observedX, observedZ, observedSpeed, ground.groundedStart());
        improperSprint = data.isSprinting() && !sprinting && !immobile;

        boolean ceilingBlocked = contact.ceilingClearance() < takeoffMax;
        boolean ceilingBlockedAtTakeoff = ceilingBlocked || lastCeilingClearance < takeoffMax;
        boolean sprintJump = effectiveGroundedStart && sprinting
                && (freshJump || (ceilingBlockedAtTakeoff && jumpHeld));
        boolean clampedJumpNow = jumpPossible && ceilingBlockedAtTakeoff;
        boolean ceilingClampedJump = clampedJumpNow || lastClampedJump;
        lastClampedJump = clampedJumpNow;
        lastCeilingClearance = contact.ceilingClearance();

        sneakStreak = data.isSneaking() ? sneakStreak + 1 : 0;
        boolean sneaking = sneakStreak >= SNEAK_CONFIRM && !contact.startOverlapping();
        boolean diagonal = state == null
                || ((state.forward() ^ state.backward()) && (state.left() ^ state.right()));

        boolean fluidExitHop = ground.wasFluid() && !fluidNow && observedY > RISE_EPS
                && lastWallContact;
        boolean powderSnowClimb = wasInPowderSnow && leatherBoots();
        boolean priorWallContact = lastWallContact;
        boolean geometricWall = contact.collidedX() || contact.collidedZ() || contact.wallNear();
        lastWallContact = gates.endTick()
                ? geometricWall && movement.isHorizontalCollision()
                : geometricWall;

        float yaw = movement.getCurrent().getYaw();
        float pitch = movement.getCurrent().getPitch();
        float prevYaw = movement.getPrevious().getYaw();
        float prevPitch = movement.getPrevious().getPitch();
        boolean modernTrig = modernTrig();
        double useMultiplier = useSlowdownMultiplier();

        double boostDirX = 0.0;
        double boostDirZ = 0.0;
        double boostSpread = 0.0;
        if (sprintJump) {
            boostDirX = ClientMath.lookX(yaw, 0.0f, modernTrig);
            boostDirZ = ClientMath.lookZ(yaw, 0.0f, modernTrig);
            boostSpread = ClientMath.horizontalDistance(
                    boostDirX - ClientMath.lookXFast(yaw, 0.0f),
                    boostDirZ - ClientMath.lookZFast(yaw, 0.0f)) * SPRINT_JUMP_BOOST;
        }

        ClaimedVector claimed = resolveClaimedInput(state, movement, doubleMove, sneaking, useMultiplier, yaw);

        boolean rawAirborneSprint = data.isSprinting();
        boolean sprintTransition = rawAirborneSprint != previousAirborneSprint;
        boolean airSprint = rawAirborneSprint || sprintTransition;
        boolean airSprintFirm = rawAirborneSprint && !sprintTransition;
        previousAirborneSprint = rawAirborneSprint;

        return new PlayerControl(inventoryOpen, horizontalInput, sneaking, sprinting, sprintJump,
                jumpPossible, ceilingClampedJump, fluidExitHop, powderSnowClimb, priorWallContact,
                effectiveSpeed(sprinting, sneaking, diagonal, ground) * useMultiplier,
                attr.jumpStrength() * ground.startJumpMax(), attr.gravity(), attr.stepHeight(),
                jumpBoostAmplifier,
                effects.hasLevitation(), effects.levitationAmplifier(), effects.hasSlowFalling(),
                fluidFriction(data.isSprinting(), effectiveGroundedStart, effects),
                fluidAccel(data.isSprinting(), effectiveGroundedStart) * useMultiplier,
                ClientMath.lookX(yaw, pitch, modernTrig),
                ClientMath.lookY(pitch, modernTrig),
                ClientMath.lookZ(yaw, pitch, modernTrig),
                pitch,
                data.isSwimming(),
                ClientMath.lookX(prevYaw, prevPitch, modernTrig),
                ClientMath.lookY(prevPitch, modernTrig),
                ClientMath.lookZ(prevYaw, prevPitch, modernTrig),
                ClientMath.lookXFast(yaw, pitch),
                ClientMath.lookYFast(pitch),
                ClientMath.lookZFast(yaw, pitch),
                ClientMath.lookXFast(prevYaw, prevPitch),
                ClientMath.lookYFast(prevPitch),
                ClientMath.lookZFast(prevYaw, prevPitch),
                data.getAbilitiesFlyingSpeed() * (data.isSprinting() ? 2.0 : 1.0) * useMultiplier,
                data.getAbilitiesFlyingSpeed() * 3.0,
                attr.airDragModifier(),
                attr.frictionModifier(),
                useMultiplier,
                boostDirX,
                boostDirZ,
                boostSpread,
                claimed.exact(),
                claimed.x(),
                claimed.z(),
                claimed.spread(),
                effectiveSpeedBase(sprinting, ground),
                fluidAccel(data.isSprinting(), effectiveGroundedStart),
                data.getAbilitiesFlyingSpeed() * (data.isSprinting() ? 2.0 : 1.0),
                airSprint,
                airSprintFirm);
    }

    private ClaimedVector resolveClaimedInput(InputData.State state, MovementData movement,
                                              boolean doubleMove, boolean sneaking,
                                              double useMultiplier, float yaw) {
        if (!gates.claimedInput() || state == null || doubleMove
                || movement.isLastFlyingWasDuplicate()) {
            return ClaimedVector.NONE;
        }
        double impulseX = (state.left() ? 1.0 : 0.0) - (state.right() ? 1.0 : 0.0);
        double impulseZ = (state.forward() ? 1.0 : 0.0) - (state.backward() ? 1.0 : 0.0);
        if (impulseX == 0.0 && impulseZ == 0.0) return ClaimedVector.ZERO;
        double multiplier = MotionDefaults.INPUT_SCALE;
        if (sneaking && !data.isFlying() && !data.isSwimming()) multiplier *= sneakMultiplier();
        multiplier *= useMultiplier;
        double scaledX = impulseX * multiplier;
        double scaledZ = impulseZ * multiplier;
        double localX;
        double localZ;
        if (gates.squareInputRescale()) {
            float floatX = (float) scaledX;
            float floatZ = (float) scaledZ;
            float length = (float) Math.sqrt(floatX * floatX + floatZ * floatZ);
            if (length < 1.0e-4f) return ClaimedVector.ZERO;
            double directionX = floatX / length;
            double directionZ = floatZ / length;
            double absX = Math.abs(scaledX);
            double absZ = Math.abs(scaledZ);
            double ratio = Math.min(absX, absZ) / Math.max(absX, absZ);
            double magnitude = Math.min(ClientMath.horizontalDistance(scaledX, scaledZ)
                    * Math.sqrt(1.0 + ratio * ratio), 1.0);
            localX = directionX * magnitude;
            localZ = directionZ * magnitude;
        } else {
            double lengthSqr = scaledX * scaledX + scaledZ * scaledZ;
            if (lengthSqr < 1.0e-7) return ClaimedVector.ZERO;
            if (lengthSqr > 1.0) {
                double length = Math.sqrt(lengthSqr);
                localX = scaledX / length;
                localZ = scaledZ / length;
            } else {
                localX = scaledX;
                localZ = scaledZ;
            }
        }
        float radians = yaw * ClientMath.DEG_TO_RAD;
        boolean modern = modernTrig();
        double sin = ClientMath.sin(radians, modern);
        double cos = ClientMath.cos(radians, modern);
        double worldX = localX * cos - localZ * sin;
        double worldZ = localZ * cos + localX * sin;
        double sinFast = ClientMath.sinFast(radians);
        double cosFast = ClientMath.cosFast(radians);
        double spread = ClientMath.horizontalDistance(
                worldX - (localX * cosFast - localZ * sinFast),
                worldZ - (localZ * cosFast + localX * sinFast));
        return new ClaimedVector(true, worldX, worldZ, spread);
    }

    private double effectiveSpeedBase(boolean sprinting, GroundFacts ground) {
        double speed = data.getAttributeData().movementSpeed();
        speed = Math.max(0.0, speed - frostPenalty(ground, speed));
        if (sprinting) speed *= SPRINT_SPEED_MULTIPLIER;
        return speed;
    }

    private double useSlowdownMultiplier() {
        UseItemData use = data.getUseItemData();
        if (!use.slowdownCertain()) return 1.0;
        return Math.min(1.0, use.slowdownMultiplier());
    }

    private double waterEfficiency(boolean groundedStart) {
        double wme = observesWaterEfficiency()
                ? data.getAttributeData().waterMovementEfficiency()
                : depthStriderEfficiency();
        return groundedStart ? wme : wme * 0.5;
    }

    private double depthStriderEfficiency() {
        ItemStack boots = actor.bootsItem();
        if (boots == null) return 0.0;
        return Math.min(3, boots.getEnchantmentLevel(EnchantmentTypes.DEPTH_STRIDER)) / 3.0;
    }

    private boolean leatherBoots() {
        ItemStack boots = actor.bootsItem();
        return boots != null && boots.getType() == ItemTypes.LEATHER_BOOTS;
    }

    private boolean observesWaterEfficiency() {
        return gates.waterEfficiencyAttribute();
    }

    private double fluidFriction(boolean sprinting, boolean groundedStart, EffectData effects) {
        if (effects.hasDolphinsGrace()) return WATER_DOLPHIN_FRICTION;
        double friction = sprinting ? WATER_SPRINT_FRICTION : WATER_FRICTION;
        if (observesWaterEfficiency()) {
            friction += (WATER_EFFICIENCY_FRICTION_TARGET - friction) * waterEfficiency(groundedStart);
        }
        return friction;
    }

    private double fluidAccel(boolean sprinting, boolean groundedStart) {
        double getSpeed = data.getAttributeData().movementSpeed() * (sprinting ? SPRINT_SPEED_MULTIPLIER : 1.0);
        return WATER_ACCEL + (getSpeed - WATER_ACCEL) * waterEfficiency(groundedStart);
    }

    private boolean sprintForward(MovementData movement, ContactReport contact, InputData.State state,
                                  double observedX, double observedZ, double observedSpeed, boolean groundedStart) {
        if (state != null) {
            backwardSprintStreak = 0;
            return state.forward();
        }
        boolean onNormalGround = groundedStart && contact.supportSlipMax() <= SPRINT_GROUND_SLIPPERINESS;
        boolean backward = false;
        if (onNormalGround && observedSpeed > SPRINT_MIN_SPEED) {
            float yaw = movement.getCurrent().getYaw();
            boolean modern = modernTrig();
            double forwardComponent = observedX * ClientMath.lookX(yaw, 0.0f, modern)
                    + observedZ * ClientMath.lookZ(yaw, 0.0f, modern);
            backward = forwardComponent / observedSpeed < SPRINT_BACKWARD_COS;
        }
        backwardSprintStreak = backward ? backwardSprintStreak + 1 : 0;
        return backwardSprintStreak <= SPRINT_TURN_TOLERANCE;
    }

    private boolean modernTrig() {
        return gates.modernTrig();
    }

    private double effectiveSpeed(boolean sprinting, boolean sneaking, boolean diagonal, GroundFacts ground) {
        double speed = data.getAttributeData().movementSpeed();
        speed = Math.max(0.0, speed - frostPenalty(ground, speed));
        if (sprinting) speed *= SPRINT_SPEED_MULTIPLIER;
        if (sneaking) {
            double sneak = sneakMultiplier();
            if (diagonal) sneak = Math.min(1.0, sneak * SNEAK_DIAGONAL_FACTOR);
            speed *= sneak;
        }
        return speed;
    }

    private double frostPenalty(GroundFacts ground, double speed) {
        int frozen = data.getTicksFrozen();
        if (frozen <= 0 || !ground.supportedStart()) return 0.0;
        double percent = Math.min(1.0, frozen / (double) FROST_FULL_TICKS);
        return FROST_SPEED_PENALTY * percent
                * Math.min(1.0, speed / MotionDefaults.BASE_MOVEMENT_SPEED);
    }

    private double sneakMultiplier() {
        if (gates.sneakingSpeedAttribute()) return data.getAttributeData().sneakingSpeed();
        if (gates.swiftSneakInput()) {
            ItemStack leggings = actor.leggingsItem();
            int level = leggings == null ? 0
                    : leggings.getEnchantmentLevel(EnchantmentTypes.SWIFT_SNEAK);
            return Math.min(1.0, MotionDefaults.SNEAKING_SPEED + SWIFT_SNEAK_PER_LEVEL * level);
        }
        return MotionDefaults.SNEAKING_SPEED;
    }

    public void onDecline() {
        backwardSprintStreak = 0;
        improperSprint = false;
        lastWallContact = true;
        lastCeilingClearance = Double.MAX_VALUE;
    }

    public void clear() {
        sneakStreak = 0;
        backwardSprintStreak = 0;
        improperSprint = false;
        lastClampedJump = false;
        lastWallContact = true;
        lastCeilingClearance = Double.MAX_VALUE;
        previousAirborneSprint = false;
    }

    private record ClaimedVector(boolean exact, double x, double z, double spread) {
        static final ClaimedVector NONE = new ClaimedVector(false, 0.0, 0.0, 0.0);
        static final ClaimedVector ZERO = new ClaimedVector(true, 0.0, 0.0, 0.0);
    }
}
