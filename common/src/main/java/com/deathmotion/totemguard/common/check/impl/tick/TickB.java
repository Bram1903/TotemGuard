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

package com.deathmotion.totemguard.common.check.impl.tick;

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@RequiresTickEnd
@CheckData(description = "Invalid tick end timing", type = CheckType.TICK, experimental = true)
public class TickB extends CheckImpl implements PacketCheck {

    private static final long EXPECTED_INTERVAL = 50L;
    private static final long WINDOW_MILLIS = 2_000L;
    private static final long IDLE_RESET_MILLIS = 1_000L;
    private static final double BUFFER_THRESHOLD = 2.0;
    private static final double BUFFER_DECREASE = 0.5;

    private long windowStartTimestamp = -1L;
    private long lastTickEndTimestamp = -1L;
    private int tickEnds;

    public TickB(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLIENT_TICK_END) return;

        final long timestamp = event.getTimestamp();
        if (windowStartTimestamp == -1L) {
            resetWindow(timestamp);
            return;
        }

        final long delta = timestamp - lastTickEndTimestamp;
        lastTickEndTimestamp = timestamp;

        if (delta <= 0L) return;

        if (delta > IDLE_RESET_MILLIS) {
            log("reset delta=" + delta + "ms");
            buffer.decrease(BUFFER_DECREASE);
            resetWindow(timestamp);
            return;
        }

        tickEnds++;

        final long elapsed = timestamp - windowStartTimestamp;
        if (elapsed < WINDOW_MILLIS) return;

        checkWindow(elapsed);
        resetWindow(timestamp);
    }

    private void checkWindow(long elapsed) {
        final long expected = Math.round(elapsed / (double) EXPECTED_INTERVAL);
        final long difference = tickEnds - expected;

        if (difference == 0L) {
            final double bufferValue = buffer.decrease(BUFFER_DECREASE);
            log("ok tick_ends=" + tickEnds
                    + " expected=" + expected
                    + " difference=" + difference
                    + " elapsed=" + elapsed + "ms"
                    + " buffer=" + bufferValue);
            return;
        }

        final double bufferValue = buffer.increase();
        final String debug = (difference > 0L ? "fast" : "slow")
                + " tick_ends=" + tickEnds
                + " expected=" + expected
                + " difference=" + difference
                + " elapsed=" + elapsed + "ms"
                + " buffer=" + bufferValue;
        log(debug);

        if (bufferValue >= BUFFER_THRESHOLD) {
            fail(debug);
        }
    }

    private void resetWindow(long timestamp) {
        windowStartTimestamp = timestamp;
        lastTickEndTimestamp = timestamp;
        tickEnds = 0;
    }

    private void log(String msg) {
        //platform.getLogger().info("[TickB] " + player.getName() + " " + msg);
    }
}
