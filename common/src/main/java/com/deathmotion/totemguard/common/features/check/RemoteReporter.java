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

package com.deathmotion.totemguard.common.features.check;

import com.deathmotion.totemguard.common.check.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncCheckRequestPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncCheckResultPacket;

final class RemoteReporter implements ResultReporter {

    private final CheckService owner;
    private final SyncCheckRequestPacket.Payload request;
    private final String targetName;

    RemoteReporter(CheckService owner, SyncCheckRequestPacket.Payload request, String targetName) {
        this.owner = owner;
        this.request = request;
        this.targetName = targetName;
    }

    @Override
    public void reportPassed(TGPlayer target, long elapsedMs, int durationMs) {
        owner.publishResult(request, targetName, SyncCheckResultPacket.STATUS_PASSED, elapsedMs, 0L);
    }

    @Override
    public void reportFlagged(TGPlayer target, long elapsedMs, int durationMs, String staffName) {
        ManualTotemA detector = target.getCheckManager().getManualCheck(ManualTotemA.class);
        if (detector != null && detector.isEnabled()) {
            detector.handle(staffName, elapsedMs, durationMs);
        }
        owner.publishResult(request, targetName, SyncCheckResultPacket.STATUS_FLAGGED, elapsedMs, 0L);
    }

    @Override
    public void reportDamageFailed(TGPlayer target) {
        owner.publishResult(request, targetName, SyncCheckResultPacket.STATUS_DAMAGE_FAILED, 0L, 0L);
    }
}
