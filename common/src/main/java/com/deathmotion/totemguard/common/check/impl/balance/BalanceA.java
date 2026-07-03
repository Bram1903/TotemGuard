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

package com.deathmotion.totemguard.common.check.impl.balance;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.mitigation.SetbackController;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(description = "Game clock running faster than real time", type = CheckType.TICK)
public class BalanceA extends CheckImpl implements PacketCheck {

    protected static final long TICK_NANOS = 50_000_000L;
    protected static final long CLOCK_DRIFT_NANOS = 120_000_000L;

    private static final long JOIN_GRACE_NANOS = 60_000_000_000L;
    private static final double BUFFER_GAIN = 1.0;
    private static final double BUFFER_DECAY = 0.0025;
    private static final double BUFFER_THRESHOLD = 2.0;
    private static final double BUFFER_RETAIN = 1.0;

    protected long anchorNanos;

    private final SetbackController setbackController;
    private long balance;
    private boolean flyingThisTick;
    private boolean overBudget;

    public BalanceA(TGPlayer player) {
        super(player);
        this.setbackController = player.getData().getSetbackController();
        long start = System.nanoTime() - JOIN_GRACE_NANOS;
        this.balance = start;
        this.anchorNanos = start;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!countsAsClientTick(event.getPacketType())) return;

        long gated = player.getPingData().getGatedTransactionAnchorNanos();
        if (gated != 0L) {
            anchorNanos = gated;
        }
        player.getPingData().markMovementForTransactionAnchor();

        balance += TICK_NANOS;

        long now = System.nanoTime();
        overBudget = balance > now;
        if (overBudget) {
            long aheadMillis = (balance - now) / 1_000_000L;
            balance -= TICK_NANOS;
            if (mitigate) setbackController.requestAnchorFreeze();
            if (shouldReport(now) && buffer.increase(BUFFER_GAIN) >= BUFFER_THRESHOLD) {
                buffer.set(BUFFER_RETAIN);
                fail("ahead={0}ms,ping={1}ms", aheadMillis, player.getPingData().getTransactionPing());
                if (mitigate) setbackController.requestSetback();
            }
        } else {
            buffer.decrease(BUFFER_DECAY);
        }

        long floor = floorNanos(now);
        if (balance < floor) {
            balance = floor;
        }
    }

    @Override
    public void onPreFlying(PacketReceiveEvent event) {
        if (!mitigate || !overBudget) return;
        if (data.getMitigationService().setbackPending()) return;
        if (data.getTeleportData().lastPacketWasTeleport()) return;
        if (!platform.getConfigRepository().configView().physicsEngineTimerPacketCancel()) return;
        event.setCancelled(true);
    }

    protected long floorNanos(long now) {
        return anchorNanos - CLOCK_DRIFT_NANOS;
    }

    protected boolean shouldReport(long now) {
        return true;
    }

    private boolean countsAsClientTick(PacketTypeCommon packetType) {
        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            boolean counted = !data.getTeleportData().lastPacketWasTeleport()
                    && !data.getMovementData().isLastFlyingWasDuplicate();
            flyingThisTick = true;
            return counted;
        }

        if (packetType == PacketType.Play.Client.CLIENT_TICK_END && player.supportsEndTick()) {
            boolean hadFlying = flyingThisTick;
            flyingThisTick = false;
            return !hadFlying;
        }

        return false;
    }
}
