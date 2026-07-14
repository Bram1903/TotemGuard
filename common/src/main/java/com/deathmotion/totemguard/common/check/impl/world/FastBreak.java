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

package com.deathmotion.totemguard.common.check.impl.world;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.Buffer;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.DiggingData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(description = "Breaking blocks faster than vanilla allows", type = CheckType.WORLD)
public class FastBreak extends CheckImpl implements PacketCheck {

    private static final double BREAK_FLAG = 5;
    private static final double BREAK_CREDIT = 1.5;
    private static final double EGREGIOUS_FRACTION = 0.4;
    private static final long SPACING_JITTER_MILLIS = 100;
    private static final int START_RATE_LIMIT = 55;
    private static final double START_RATE_FLAG = 4;
    private static final double START_RATE_CREDIT = 0.25;
    private static final double WRONG_TARGET_FLAG = 2;
    private static final double WRONG_TARGET_CREDIT = 0.5;

    private final Buffer wrongTarget = new Buffer();
    private final Buffer startRate = new Buffer();

    public FastBreak(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;
        if (player.getData().getGameMode() == GameMode.CREATIVE) return;

        WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        DiggingData digging = player.getData().getDiggingData();

        switch (packet.getAction()) {
            case START_DIGGING -> checkStartRate(digging);
            case CANCELLED_DIGGING -> {
                if (digging.pollWrongAbort()) checkWrongTarget(event, packet, "abort");
                else wrongTarget.decrease(WRONG_TARGET_CREDIT);
            }
            case FINISHED_DIGGING -> {
                DiggingData.FinishJudgment judgment = digging.pollFinishJudgment();
                if (digging.pollWrongFinish()) checkWrongTarget(event, packet, "finish");
                else if (judgment != null) checkBreakSpeed(event, packet, judgment);
            }
            default -> {
            }
        }
    }

    private void checkStartRate(DiggingData digging) {
        if (digging.startsInWindow() <= START_RATE_LIMIT) {
            startRate.decrease(START_RATE_CREDIT);
            return;
        }
        if (startRate.increase() < START_RATE_FLAG) return;
        startRate.reset();
        fail("start rate " + digging.startsInWindow() + "/s");
    }

    private void checkWrongTarget(PacketReceiveEvent event, WrapperPlayClientPlayerDigging packet, String kind) {
        if (wrongTarget.increase() < WRONG_TARGET_FLAG) return;
        wrongTarget.reset();
        if (fail(kind + " for a block not being mined") && mitigate) {
            cancelAndResync(event, packet);
        }
    }

    private void checkBreakSpeed(PacketReceiveEvent event, WrapperPlayClientPlayerDigging packet,
                                 DiggingData.FinishJudgment judgment) {
        boolean tooFast = !judgment.mirrorEditable();
        boolean tooClose = judgment.sinceLastFinishMillis() != Long.MAX_VALUE
                && judgment.sinceLastFinishMillis() + SPACING_JITTER_MILLIS < judgment.requiredGapMillis();

        if (!tooFast && !tooClose) {
            buffer.decrease(BREAK_CREDIT);
            return;
        }

        double weight = tooFast && judgment.elapsedMillis() < judgment.expectedMillis() * EGREGIOUS_FRACTION ? 2.0 : 1.0;
        if (buffer.increase(weight) < BREAK_FLAG) return;
        buffer.reset();

        String detail = tooFast
                ? "broke in " + judgment.elapsedMillis() + "ms, server accepts at "
                  + (long) (judgment.expectedMillis() * DiggingData.SERVER_ACCEPT_FRACTION) + "ms"
                : "finish gap " + judgment.sinceLastFinishMillis() + "ms, needs " + judgment.requiredGapMillis() + "ms";
        if (fail(detail) && mitigate) {
            cancelAndResync(event, packet);
        }
    }

    private void cancelAndResync(PacketReceiveEvent event, WrapperPlayClientPlayerDigging packet) {
        event.setCancelled(true);
        Vector3i position = packet.getBlockPosition();
        player.getWorldMirror().predicted().drop(position.getX(), position.getY(), position.getZ());
        player.getData().getMitigationService().resyncBlock(position.getX(), position.getY(), position.getZ());
        player.getData().getMitigationService().acknowledgeBlockChanges(packet.getSequence());
    }
}
