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

package com.deathmotion.totemguard.common.check.impl.inventory;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.HeuristicCheck;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.CombatTracker;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.util.MathUtil;
import com.deathmotion.totemguard.common.util.datastructure.EvictingList;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;

@CheckData(description = "Inventory close-spam", type = CheckType.INVENTORY, experimental = true)
public class InventoryE extends HeuristicCheck implements PacketCheck {

    private static final int SAMPLE_SIZE = 16;
    private static final long STALE_SESSION_MS = 5_000L;

    private static final long FAST_SESSION_MS = 100L;

    private static final long METRONOME_STDDEV_MS = 15L;
    private static final long METRONOME_MEAN_CAP_MS = 200L;

    private static final int CONSECUTIVE_STRICT_BIG = 6;
    private static final int CONSECUTIVE_STRICT_HUGE = 10;
    private static final int CONSECUTIVE_FAST_BIG = 8;
    private static final int CONSECUTIVE_FAST_HUGE = 12;

    private final CombatTracker combatTracker;
    private final EvictingList<Long> recentSessionDurationsMs = new EvictingList<>(SAMPLE_SIZE);

    private long sessionOpenedAtMs = -1L;
    private int tickEndsObservedThisSession;
    private long lastEventAtMs = -1L;
    private int consecutiveFast;

    public InventoryE(TGPlayer player) {
        super(player);
        this.combatTracker = player.getCombatTracker();
    }

    private static double mean(EvictingList<Long> values) {
        if (values.isEmpty()) return 0.0;
        long total = 0L;
        for (Long v : values) total += v;
        return (double) total / values.size();
    }

    @Override
    protected double flagThreshold() {
        return 4.0;
    }

    @Override
    protected double decayPerSecond() {
        return 0.3;
    }

    @Override
    protected long maxInventoryCampMs() {
        return Long.MAX_VALUE;
    }

    @Override
    protected boolean requiresCombat() {
        return true;
    }

    @Override
    protected boolean inCombat() {
        return combatTracker.inActiveCombat();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (sessionOpenedAtMs >= 0
                && player.supportsEndTick()
                && type == PacketType.Play.Client.CLIENT_TICK_END) {
            tickEndsObservedThisSession++;
        }

        if (type != PacketType.Play.Client.CLICK_WINDOW && type != PacketType.Play.Client.CLOSE_WINDOW) {
            return;
        }

        if (data.isInventoryMitigatedThisTick()) return;
        if (data.isInNetherPortal()) return;

        final long timestamp = event.getTimestamp();
        if (type == PacketType.Play.Client.CLICK_WINDOW) {
            int windowId = new WrapperPlayClientClickWindow(event).getWindowId();
            if (windowId != InventoryConstants.PLAYER_WINDOW_ID) return;
            onClick(timestamp);
        } else {
            int windowId = new WrapperPlayClientCloseWindow(event).getWindowId();
            if (windowId != InventoryConstants.PLAYER_WINDOW_ID) return;
            onClose(timestamp);
        }
    }

    private void onClick(long timestamp) {
        boolean sessionStale = lastEventAtMs > 0 && timestamp - lastEventAtMs > STALE_SESSION_MS;
        if (sessionOpenedAtMs < 0 || sessionStale) {
            sessionOpenedAtMs = timestamp;
            tickEndsObservedThisSession = 0;
            if (sessionStale) {
                consecutiveFast = 0;
                recentSessionDurationsMs.clear();
            }
        }
        lastEventAtMs = timestamp;
    }

    private void onClose(long timestamp) {
        long openedAt = sessionOpenedAtMs;
        int tickEnds = tickEndsObservedThisSession;
        sessionOpenedAtMs = -1L;
        tickEndsObservedThisSession = 0;
        lastEventAtMs = timestamp;

        if (openedAt < 0) return;

        long durationMs = timestamp - openedAt;
        if (durationMs < 0 || durationMs > STALE_SESSION_MS) {
            consecutiveFast = 0;
            return;
        }

        boolean strictSameTick = isStrictSameTick(tickEnds);
        boolean fast = strictSameTick || durationMs < FAST_SESSION_MS;

        if (fast) {
            consecutiveFast++;
        } else {
            consecutiveFast = 0;
        }

        recentSessionDurationsMs.add(durationMs);

        double weight = 0.0;
        if (strictSameTick) {
            if (consecutiveFast >= CONSECUTIVE_STRICT_BIG) weight += 1.5;
            if (consecutiveFast >= CONSECUTIVE_STRICT_HUGE) weight += 1.5;
        } else if (fast) {
            if (consecutiveFast >= CONSECUTIVE_FAST_BIG) weight += 1.0;
            if (consecutiveFast >= CONSECUTIVE_FAST_HUGE) weight += 1.5;
        }

        String stddevText = "n/a";
        if (recentSessionDurationsMs.size() == SAMPLE_SIZE) {
            double mean = mean(recentSessionDurationsMs);
            double stddev = MathUtil.getStandardDeviation(recentSessionDurationsMs);
            stddevText = String.format("%.2f", stddev);
            if (mean < METRONOME_MEAN_CAP_MS && stddev < METRONOME_STDDEV_MS) {
                weight += 2.0;
            }
        }

        if (weight <= 0.0) return;

        punish(weight, "dur={0},ticks={1},same={2},consec={3},stddev={4}",
                durationMs, tickEnds, strictSameTick, consecutiveFast, stddevText);
    }

    private boolean isStrictSameTick(int tickEndsObserved) {
        return player.supportsEndTick() && tickEndsObserved == 0;
    }

    public void resetSession() {
        sessionOpenedAtMs = -1L;
        tickEndsObservedThisSession = 0;
        lastEventAtMs = -1L;
        consecutiveFast = 0;
        recentSessionDurationsMs.clear();
    }
}
