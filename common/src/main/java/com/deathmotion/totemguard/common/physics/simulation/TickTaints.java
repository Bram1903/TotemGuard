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

import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.player.data.Data;

public final class TickTaints {

    private boolean landModel;
    private boolean landMedium;
    private boolean flying;
    private boolean doubleMove;
    private boolean stuck;
    private boolean pushed;
    private boolean bubble;
    private boolean externalVelocityActive;
    private boolean pistonDataActive;
    private boolean riptideActive;
    private boolean spinAttacking;
    private boolean setbackPending;
    private boolean collidedX;
    private boolean collidedZ;
    private boolean wallNear;
    private boolean startOverlapping;
    private boolean stepUsed;
    private boolean inputExact;
    private boolean horizontalInput;
    private boolean airborneStart;
    private boolean airAccelAmbiguous;
    private boolean sneakMismatch;
    private boolean slowdownAmbiguous;

    private boolean pistonInfluence;
    private boolean entityPush;
    private boolean stepped;
    private boolean stepFromFall;
    private boolean bounceAltOffered;
    private boolean knockbackChosen;
    private boolean altCenterUsed;

    public void computeBase(TickState state, Data data) {
        landModel = state.landModel();
        landMedium = state.landMedium;
        flying = data.isFlying();
        doubleMove = state.doubleMove;
        stuck = state.sample.stuck();
        pushed = state.sample.pushed();
        bubble = state.sample.bubbleAscent() > 0.0;
        externalVelocityActive = data.getExternalVelocityData().isActive();
        pistonDataActive = data.getPistonData().isActive();
        riptideActive = data.getGlideData().riptideActive();
        spinAttacking = data.isSpinAttacking();
        setbackPending = data.getMitigationService().setbackPending();
        collidedX = state.contact.collidedX();
        collidedZ = state.contact.collidedZ();
        wallNear = state.contact.wallNear();
        startOverlapping = state.contact.startOverlapping();
        stepUsed = state.contact.stepUsedHeight() > 0.0;
        inputExact = state.input != null && state.input.claimedInputExact();
        horizontalInput = state.input != null && state.input.horizontalInput();
        airborneStart = state.ground != null && state.ground.start() == GroundState.AIRBORNE;
        airAccelAmbiguous = state.input != null
                && state.input.airAccelBase() != state.input.airAccelBaseMin();
        sneakMismatch = state.input != null && state.input.sneaking() != data.isSneaking();
        slowdownAmbiguous = data.getUseItemData().slowdownAmbiguous();
        pistonInfluence = false;
        entityPush = false;
        stepped = false;
        stepFromFall = false;
        bounceAltOffered = false;
        knockbackChosen = false;
        altCenterUsed = false;
    }

    public void computeJudged(boolean pistonInfluence, boolean entityPush,
                              boolean stepped, boolean stepFromFall,
                              boolean bounceAltOffered, boolean knockbackChosen, boolean altCenterUsed) {
        this.pistonInfluence = pistonInfluence;
        this.entityPush = entityPush;
        this.stepped = stepped;
        this.stepFromFall = stepFromFall;
        this.bounceAltOffered = bounceAltOffered;
        this.knockbackChosen = knockbackChosen;
        this.altCenterUsed = altCenterUsed;
    }

    public boolean forbidsRiseObligation() {
        return !landModel || flying || doubleMove
                || stuck || pushed || bubble
                || externalVelocityActive || pistonDataActive
                || riptideActive || spinAttacking
                || setbackPending;
    }

    public boolean forbidsUnobstructedFall() {
        return !landModel || flying || doubleMove
                || pushed || bubble
                || externalVelocityActive || pistonDataActive
                || riptideActive || spinAttacking
                || setbackPending
                || collidedX || collidedZ || wallNear || startOverlapping || stepUsed;
    }

    public boolean forbidsKnockbackRequirement() {
        return collidedX || collidedZ || wallNear
                || startOverlapping || stepUsed || stepFromFall
                || stuck || !landMedium || flying
                || pistonInfluence
                || riptideActive || spinAttacking
                || bounceAltOffered
                || setbackPending;
    }

    public boolean forbidsAbsentKnockbackRequirement(int pushersNear) {
        return stuck || !landMedium || flying
                || wallNear || startOverlapping
                || pushersNear > 0
                || pistonDataActive
                || riptideActive || spinAttacking
                || setbackPending;
    }

    public boolean forbidsMomentum() {
        return !inputExact || !horizontalInput
                || !airborneStart
                || !landModel || !landMedium
                || stepped || stepFromFall
                || collidedX || collidedZ || wallNear
                || startOverlapping
                || stuck || pushed || bubble
                || externalVelocityActive || pistonDataActive
                || riptideActive || spinAttacking
                || setbackPending
                || pistonInfluence || entityPush || knockbackChosen || altCenterUsed
                || airAccelAmbiguous
                || sneakMismatch
                || slowdownAmbiguous
                || doubleMove;
    }

    public long bits() {
        long bits = 0L;
        if (landModel) bits |= 1L;
        if (landMedium) bits |= 1L << 1;
        if (flying) bits |= 1L << 2;
        if (doubleMove) bits |= 1L << 3;
        if (stuck) bits |= 1L << 4;
        if (pushed) bits |= 1L << 5;
        if (bubble) bits |= 1L << 6;
        if (externalVelocityActive) bits |= 1L << 7;
        if (pistonDataActive) bits |= 1L << 8;
        if (riptideActive) bits |= 1L << 9;
        if (spinAttacking) bits |= 1L << 10;
        if (setbackPending) bits |= 1L << 11;
        if (collidedX) bits |= 1L << 12;
        if (collidedZ) bits |= 1L << 13;
        if (wallNear) bits |= 1L << 14;
        if (startOverlapping) bits |= 1L << 15;
        if (stepUsed) bits |= 1L << 16;
        if (inputExact) bits |= 1L << 17;
        if (horizontalInput) bits |= 1L << 18;
        if (airborneStart) bits |= 1L << 19;
        if (airAccelAmbiguous) bits |= 1L << 20;
        if (sneakMismatch) bits |= 1L << 21;
        if (slowdownAmbiguous) bits |= 1L << 22;
        if (pistonInfluence) bits |= 1L << 23;
        if (stepped) bits |= 1L << 24;
        if (stepFromFall) bits |= 1L << 25;
        if (bounceAltOffered) bits |= 1L << 26;
        if (knockbackChosen) bits |= 1L << 27;
        if (altCenterUsed) bits |= 1L << 28;
        if (entityPush) bits |= 1L << 29;
        return bits;
    }
}
