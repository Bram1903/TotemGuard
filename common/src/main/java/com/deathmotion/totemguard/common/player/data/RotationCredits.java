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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public final class RotationCredits {

    private static final int REPORTS_PER_WRITE = 2;
    private static final int TICKS_SPENDABLE = 2;
    private static final int ROOM = 8;

    private final TGPlayer player;

    private int inFlight;
    private int spendable;
    private long clientTick;
    private long grantedAt = Long.MIN_VALUE;

    public RotationCredits(TGPlayer player) {
        this.player = player;
    }

    public void serverWrote(PacketSendEvent event) {
        if (event.isCancelled()) return;
        inFlight++;
        player.getLatencyHandler().compensate(event, this::granted);
    }

    public void ticked() {
        clientTick++;
    }

    public boolean inFlight() {
        return inFlight > 0;
    }

    public boolean spend() {
        if (spendable() == 0) return false;
        spendable--;
        return true;
    }

    private void granted() {
        inFlight = Math.max(0, inFlight - 1);
        spendable = Math.min(spendable + REPORTS_PER_WRITE, ROOM);
        grantedAt = clientTick;
    }

    private int spendable() {
        if (clientTick - grantedAt > TICKS_SPENDABLE) {
            spendable = 0;
        }
        return spendable;
    }
}
