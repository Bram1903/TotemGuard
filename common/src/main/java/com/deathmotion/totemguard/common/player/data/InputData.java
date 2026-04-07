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

@Getter
@Setter
public class InputData {

    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean jumping;
    private boolean sneaking;
    private boolean sprinting;

    public void setState(boolean forward, boolean backward, boolean left, boolean right, boolean jumping, boolean sneaking, boolean sprinting) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.jumping = jumping;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
    }

    public void reset() {
        setState(false, false, false, false, false, false, false);
    }

    public boolean isInput() {
        return forward
                || backward
                || left
                || right
                || jumping
                || sneaking
                || sprinting;
    }
}
