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

// Tracks recent combat intent for use by heuristic checks that should only run while a player
// is actively fighting. Only inbound packets are sampled: we look at what the client claims to
// be doing (ATTACK / INTERACT_ENTITY(ATTACK)), not at server-side damage events, so this tracker
// adds zero outbound traffic (no extra transaction packets) and no latency-compensation cost.
//
// "Actively fighting" here means the client has sent an attack packet recently. That is a strict
// signal: the cheats InventoryE targets are auto-totem class bots, which by definition attack
// someone while clicking. A defender who is only taking damage but not swinging is excluded by
// design. That's fine for our threat model.
public class CombatTracker {

    public static final long ACTIVE_COMBAT_WINDOW_MS = 10_000L;

    @Getter
    private long lastOutgoingAttackMs;
    @Getter
    private int lastAttackedEntityId = -1;

    public void recordOutgoingAttack(int targetEntityId, long timestamp) {
        this.lastOutgoingAttackMs = timestamp;
        this.lastAttackedEntityId = targetEntityId;
    }

    public void recordOutgoingStab(long timestamp) {
        this.lastOutgoingAttackMs = timestamp;
    }

    public boolean inActiveCombat() {
        return inActiveCombat(ACTIVE_COMBAT_WINDOW_MS);
    }

    public boolean inActiveCombat(long withinMs) {
        if (lastOutgoingAttackMs <= 0) return false;
        return System.currentTimeMillis() - lastOutgoingAttackMs <= withinMs;
    }

    public long msSinceLastAttack() {
        return lastOutgoingAttackMs <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() - lastOutgoingAttackMs;
    }

    public void reset() {
        this.lastOutgoingAttackMs = 0L;
        this.lastAttackedEntityId = -1;
    }
}
