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

package com.deathmotion.totemguard.common.replay.capture;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayFrame;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayProvider;
import com.deathmotion.totemguard.common.replay.ReplayText;
import net.kyori.adventure.text.Component;

import java.util.Map;

public final class RecordingOverlayProvider implements DebugOverlayProvider {

    private final RecordingSession session;

    public RecordingOverlayProvider(RecordingSession session) {
        this.session = session;
    }

    @Override
    public String getKey() {
        return "recording";
    }

    @Override
    public DebugOverlayFrame buildFrame(TGPlayer player) {
        var messages = TGPlatform.getInstance().getMessageService();
        long flags = session.flagCount();
        Component line = messages.getComponent(MessagesKeys.REPLAY_ACTION_BAR, Map.of(
                "tg_label", session.getLabel().id(),
                "tg_scenario", session.getScenario(),
                "tg_elapsed", ReplayText.elapsed(session.elapsedNanos(player.getClock().nanos())),
                "tg_frames", session.frameCount(),
                "tg_size", ReplayText.size(session.byteCount()),
                "tg_judged", session.judgedCount()
        )).append(messages.getComponent(
                flags > 0 ? MessagesKeys.REPLAY_ACTION_BAR_FLAGGED : MessagesKeys.REPLAY_ACTION_BAR_FLAGS,
                Map.of("tg_flags", flags)));

        if (!session.degraded()) return DebugOverlayFrame.of(line);

        return DebugOverlayFrame.of(line.append(messages.getComponent(
                MessagesKeys.REPLAY_ACTION_BAR_DROPPED,
                Map.of("tg_dropped", session.droppedCount()))));
    }
}
