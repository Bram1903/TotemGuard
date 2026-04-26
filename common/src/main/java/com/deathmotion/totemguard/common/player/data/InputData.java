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

public class InputData {

    private State current = State.empty();
    private State previous = State.empty();

    public void setState(boolean forward, boolean backward, boolean left, boolean right, boolean jumping, boolean sneaking, boolean sprinting) {
        this.previous = this.current;
        this.current = new State(forward, backward, left, right, jumping, sneaking, sprinting);
    }

    public void reset() {
        setState(false, false, false, false, false, false, false);
    }

    public boolean isInput() {
        return current.isInput();
    }

    public boolean isInput(boolean ignoreJumping) {
        return current.isInput(ignoreJumping);
    }

    public State current() {
        return current;
    }

    public State previous() {
        return previous;
    }

    public record State(
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jumping,
            boolean sneaking,
            boolean sprinting
    ) {
        public static State empty() {
            return new State(false, false, false, false, false, false, false);
        }

        public boolean isInput() {
            return forward || backward || left || right || jumping || sneaking || sprinting;
        }

        public boolean isInput(boolean ignoreJumping) {
            if (!ignoreJumping) {
                return isInput();
            }

            return forward || backward || left || right || sneaking || sprinting;
        }
    }
}