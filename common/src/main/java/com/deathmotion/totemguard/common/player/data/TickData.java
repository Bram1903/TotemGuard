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

package com.deathmotion.totemguard.common.player.data;

import lombok.Getter;
import lombok.Setter;

/*
    This data is only for the current tick
 */
@Getter
@Setter
public class TickData {
    private boolean swapping;
    private boolean dropping;
    private boolean interacting;
    private boolean attacking;
    private boolean releasing;
    private boolean digging;
    private boolean sprinting;
    private boolean sneaking;
    private boolean placing;
    private boolean using;
    private boolean picking;
    private boolean clickingInInventory;
    private boolean closingInventory;
    private boolean quickMoveClicking;
    private boolean pickUpClicking;
    private boolean leavingBed;
    private boolean startingToGlide;
    private boolean jumpingWithMount;

    public void reset() {
        swapping = false;
        dropping = false;
        interacting = false;
        attacking = false;
        releasing = false;
        digging = false;
        sprinting = false;
        sneaking = false;
        placing = false;
        using = false;
        picking = false;
        clickingInInventory = false;
        closingInventory = false;
        quickMoveClicking = false;
        pickUpClicking = false;
        leavingBed = false;
        startingToGlide = false;
        jumpingWithMount = false;
    }

    public boolean isInvalidLeftClick() {

        if (isDigging()) return true;
        if (isPlacing()) return true;

        return false;
    }
}
