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
import java.util.function.Consumer;

final class PlayerChatBuffer {

    private final Scheduler scheduler;
    private final Consumer<Component> broadcaster;
    private final long bufferWindow;
    private final TimeUnit bufferWindowUnit;
    private final ConcurrentHashMap<String, ChatBuffer> checkBuffers = new ConcurrentHashMap<>();

    PlayerChatBuffer(
            Scheduler scheduler,
            Consumer<Component> broadcaster,
            long bufferWindow,
            TimeUnit bufferWindowUnit
    ) {
        this.scheduler = scheduler;
        this.broadcaster = broadcaster;
        this.bufferWindow = bufferWindow;
        this.bufferWindowUnit = bufferWindowUnit;
    }

    void buffer(CheckImpl check, int violations, @Nullable String debug) {
        String checkName = check.getName();

        checkBuffers.compute(checkName, (ignored, existingBuffer) -> {
            if (existingBuffer == null) {
                ChatBuffer createdBuffer = createBuffer(checkName, check);
                createdBuffer.update(violations, debug);
                return createdBuffer;
            }

            existingBuffer.update(violations, debug);
            return existingBuffer;
        });
    }

    void clear() {
        checkBuffers.clear();
    }

    private ChatBuffer createBuffer(String checkName, CheckImpl check) {
        ChatBuffer chatBuffer = new ChatBuffer(check);

        scheduler.runAsyncTaskDelayed(
                () -> flush(checkName, chatBuffer),
                bufferWindow,
                bufferWindowUnit
        );

        return chatBuffer;
    }

    private void flush(String checkName, ChatBuffer chatBuffer) {
        if (!checkBuffers.remove(checkName, chatBuffer)) {
            return;
        }

        Component alertMessage = AlertBuilder.build(
                chatBuffer.getCheck(),
                chatBuffer.getViolations(),
                chatBuffer.getDebug()
        );
        broadcaster.accept(alertMessage);
    }
}
