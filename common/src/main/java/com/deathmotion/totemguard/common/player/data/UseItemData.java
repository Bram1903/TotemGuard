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

public final class UseItemData {

    private static final int PENDING_TIMEOUT_TICKS = 60;
    private static final int EDGE_UNION_TICKS = 2;

    private boolean pendingStart;
    private boolean active;
    private boolean mainHandUse;
    private int pendingTicks;
    private int edgeTicks;
    private double slowdownMultiplier = 1.0;

    public void onUseItem(boolean mainHand, double multiplier) {
        pendingStart = true;
        pendingTicks = 0;
        mainHandUse = mainHand;
        slowdownMultiplier = multiplier;
        edgeTicks = Math.max(edgeTicks, EDGE_UNION_TICKS);
    }

    public void onFlagsAck(boolean using) {
        if (using) {
            if (!active) edgeTicks = Math.max(edgeTicks, EDGE_UNION_TICKS);
            active = true;
            pendingStart = false;
        } else if (active || pendingStart) {
            endEdge();
        }
    }

    public void onRelease() {
        if (active || pendingStart) endEdge();
    }

    public void onSwap() {
        if (active || pendingStart) endEdge();
    }

    public void onHeldSlotChange() {
        if ((active || pendingStart) && mainHandUse) endEdge();
    }

    public void tick() {
        if (edgeTicks > 0) edgeTicks--;
        if (pendingStart && ++pendingTicks > PENDING_TIMEOUT_TICKS) pendingStart = false;
    }

    private void endEdge() {
        pendingStart = false;
        active = false;
        edgeTicks = EDGE_UNION_TICKS;
    }

    public boolean slowdownCertain() {
        return active && edgeTicks == 0;
    }

    public double slowdownMultiplier() {
        return slowdownMultiplier;
    }

    public void reset() {
        pendingStart = false;
        active = false;
        mainHandUse = false;
        pendingTicks = 0;
        edgeTicks = 0;
        slowdownMultiplier = 1.0;
    }
}
