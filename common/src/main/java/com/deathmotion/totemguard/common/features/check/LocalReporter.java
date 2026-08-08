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
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;

import java.util.Map;

final class LocalReporter implements ResultReporter {

    private final CheckService owner;
    private final Sender sender;

    LocalReporter(CheckService owner, Sender sender) {
        this.owner = owner;
        this.sender = sender;
    }

    @Override
    public void reportPassed(TGPlayer target, long elapsedMs, int durationMs) {
        sender.sendMessage(owner.platform.getMessageService().getComponent(MessagesKeys.CHECK_PASSED, target));
    }

    @Override
    public void reportFlagged(TGPlayer target, long elapsedMs, int durationMs, String staffName) {
        ManualTotemA detector = target.getCheckManager().getManualCheck(ManualTotemA.class);
        if (detector != null && detector.isEnabled()) {
            detector.handle(staffName, elapsedMs, durationMs);
        }
        sender.sendMessage(owner.platform.getMessageService().getComponent(
                MessagesKeys.CHECK_FLAGGED,
                target,
                Map.of("tg_elapsed_ms", elapsedMs, "tg_window_ms", (long) durationMs)
        ));
    }

    @Override
    public void reportDamageFailed(TGPlayer target) {
        sender.sendMessage(owner.platform.getMessageService().getComponent(MessagesKeys.CHECK_DAMAGE_FAILED, target));
    }
}
