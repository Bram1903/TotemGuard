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

package com.deathmotion.totemguard.common.physics.input;

import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.EffectData;
import com.deathmotion.totemguard.common.player.data.InputData;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.data.PlayerAttributeData;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class InputResolver {

    private static final double RISE_EPS = 0.001;
    private static final double COYOTE_TAKEOFF_EPS = 0.05;

    private static final int SNEAK_CONFIRM = 3;
    private static final double SNEAK_DIAGONAL_FACTOR = Math.sqrt(2.0);

    private static final double SPRINT_MIN_SPEED = 0.15;
    private static final double SPRINT_BACKWARD_COS = -0.6;
    private static final int SPRINT_TURN_TOLERANCE = 8;
    private static final double SPRINT_GROUND_SLIPPERINESS = 0.61;

    private static final double SPRINT_SPEED_MULTIPLIER = 1.3;
    private static final double WATER_FRICTION = 0.8;
    private static final double WATER_SPRINT_FRICTION = 0.9;
    private static final double WATER_DOLPHIN_FRICTION = 0.96;
    private static final double WATER_ACCEL = 0.02;
    private static final double WATER_EFFICIENCY_FRICTION_TARGET = 0.54600006;
    private static final double WATER_CURRENT_PUSH = 0.014;

    private final Data data;

    private int sneakStreak;
    private int backwardSprintStreak;
    @Getter
    @Setter
    private boolean improperSprint;
    private boolean lastClampedJump;
    private boolean lastWallContact = true;

    public InputResolver(Data data) {
        this.data = data;
    }

    public PlayerInput build(MovementData movement, ContactReport contact, GroundFacts ground,
                              boolean fluidNow, double observedX, double observedY, double observedZ) {
        InputData.State state = data.getInputData().current();
        boolean inventoryOpen = data.isOpenInventory();
        boolean horizontalInput = !inventoryOpen
                && (state == null || state.forward() || state.backward() || state.left() || state.right());
        boolean jumpHeld = state == null || state.jumping();

        EffectData effects = data.getEffectData();
        PlayerAttributeData attr = data.getAttributeData();
        int jumpBoostAmplifier = effects.hasJumpBoost() ? effects.jumpBoostAmplifier() : -1;
        double jumpBoost = jumpBoostAmplifier >= 0 ? 0.1 * (jumpBoostAmplifier + 1) : 0.0;
        double takeoffMin = attr.jumpStrength() * ground.startJumpMin() + jumpBoost;
        double takeoffMax = attr.jumpStrength() * ground.startJumpMax() + jumpBoost;

        boolean freshJump = observedY >= takeoffMin - COYOTE_TAKEOFF_EPS;
        boolean landingJump = ground.landingSupport()
                && observedY > RISE_EPS && observedY <= takeoffMax + COYOTE_TAKEOFF_EPS;
        boolean coyoteJump = !ground.groundedStart()
                && jumpHeld && !inventoryOpen && !ground.coyoteBlocked()
                && (((ground.recentlyGrounded() || ground.startAmbiguous()) && freshJump) || landingJump);
        boolean effectiveGroundedStart = ground.groundedStart() || coyoteJump;
        boolean jumpPossible = effectiveGroundedStart && jumpHeld && !inventoryOpen;

        double observedSpeed = Math.hypot(observedX, observedZ);
        boolean sprinting = !inventoryOpen && data.isSprinting() && data.getFoodData().canSprint()
                && sprintForward(movement, contact, state, observedX, observedZ, observedSpeed, ground.groundedStart());
        improperSprint = data.isSprinting() && !sprinting && !inventoryOpen;

        boolean ceilingBlocked = contact.ceilingClearance() < takeoffMax;
        boolean sprintJump = effectiveGroundedStart && sprinting
                && (freshJump || (ceilingBlocked && jumpHeld));
        boolean clampedJumpNow = jumpPossible && ceilingBlocked;
        boolean ceilingClampedJump = clampedJumpNow || lastClampedJump;
        lastClampedJump = clampedJumpNow;

        sneakStreak = data.isSneaking() ? sneakStreak + 1 : 0;
        boolean sneaking = sneakStreak >= SNEAK_CONFIRM && !contact.startOverlapping();
        boolean diagonal = state == null
                || ((state.forward() ^ state.backward()) && (state.left() ^ state.right()));

        boolean fluidExitHop = ground.wasFluid() && !fluidNow && observedY > RISE_EPS
                && lastWallContact;
        lastWallContact = contact.collidedX() || contact.collidedZ() || contact.wallNear();

        return new PlayerInput(inventoryOpen, horizontalInput, sneaking, sprinting, sprintJump,
                jumpPossible, ceilingClampedJump, fluidExitHop,
                effectiveSpeed(sprinting, sneaking, diagonal),
                attr.jumpStrength() * ground.startJumpMax(), attr.gravity(), attr.stepHeight(),
                jumpBoostAmplifier,
                effects.hasLevitation(), effects.levitationAmplifier(), effects.hasSlowFalling(),
                fluidFriction(data.isSprinting(), effectiveGroundedStart, effects),
                fluidAccel(data.isSprinting(), effectiveGroundedStart));
    }

    private double waterEfficiency(boolean groundedStart) {
        double wme = observesWaterEfficiency() ? data.getAttributeData().waterMovementEfficiency() : 1.0;
        return groundedStart ? wme : wme * 0.5;
    }

    private boolean observesWaterEfficiency() {
        return data.getPlayer().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21);
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
        double accel = WATER_ACCEL + (getSpeed - WATER_ACCEL) * waterEfficiency(groundedStart);
        return accel + WATER_CURRENT_PUSH;
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
            double yaw = Math.toRadians(movement.getCurrent().getYaw());
            double forwardComponent = observedX * -Math.sin(yaw) + observedZ * Math.cos(yaw);
            backward = forwardComponent / observedSpeed < SPRINT_BACKWARD_COS;
        }
        backwardSprintStreak = backward ? backwardSprintStreak + 1 : 0;
        return backwardSprintStreak <= SPRINT_TURN_TOLERANCE;
    }

    private double effectiveSpeed(boolean sprinting, boolean sneaking, boolean diagonal) {
        double speed = data.getAttributeData().movementSpeed();
        if (sprinting) speed *= SPRINT_SPEED_MULTIPLIER;
        if (sneaking) {
            double sneak = data.getAttributeData().sneakingSpeed();
            if (diagonal) sneak = Math.min(1.0, sneak * SNEAK_DIAGONAL_FACTOR);
            speed *= sneak;
        }
        return speed;
    }

    public void onDecline() {
        backwardSprintStreak = 0;
        improperSprint = false;
        lastWallContact = true;
    }

    public void clear() {
        sneakStreak = 0;
        backwardSprintStreak = 0;
        improperSprint = false;
        lastClampedJump = false;
        lastWallContact = true;
    }
}
