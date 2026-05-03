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

package com.deathmotion.totemguard.common.alert;

import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.util.Scheduler;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class PlayerChatBuffer {

    private final Scheduler scheduler;
    private final FlagBroadcaster broadcaster;
    private final long bufferWindowSeconds;
    private final ConcurrentHashMap<String, ChatBuffer> checkBuffers = new ConcurrentHashMap<>();

    PlayerChatBuffer(Scheduler scheduler, FlagBroadcaster broadcaster, long bufferWindowSeconds) {
        this.scheduler = scheduler;
        this.broadcaster = broadcaster;
        this.bufferWindowSeconds = bufferWindowSeconds;
    }

    void buffer(CheckImpl check, int violations, @Nullable String debug) {
        String checkName = check.getName();

        ChatBuffer existing = checkBuffers.get(checkName);
        if (existing != null) {
            existing.update(violations, debug);
            return;
        }

        ChatBuffer fresh = new ChatBuffer(check);
        fresh.update(violations, debug);

        ChatBuffer prior = checkBuffers.putIfAbsent(checkName, fresh);
        if (prior != null) {
            // Lost the race — merge into the winner.
            prior.update(violations, debug);
            return;
        }

        scheduler.runAsyncTaskDelayed(() -> flush(checkName, fresh), bufferWindowSeconds, TimeUnit.SECONDS);
    }

    void clear() {
        checkBuffers.clear();
    }

    private void flush(String checkName, ChatBuffer chatBuffer) {
        if (!checkBuffers.remove(checkName, chatBuffer)) return;

        CheckImpl check = chatBuffer.getCheck();
        Component alertMessage = AlertBuilder.build(
                check,
                chatBuffer.getViolations(),
                chatBuffer.getDebug()
        );
        broadcaster.broadcast(check.player.getUuid(), check.player.getName(), alertMessage);
    }
}
