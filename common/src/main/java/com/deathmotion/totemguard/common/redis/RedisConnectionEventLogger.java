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

package com.deathmotion.totemguard.common.redis;

import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.event.connection.ReconnectAttemptEvent;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.resource.ClientResources;
import org.jetbrains.annotations.Nullable;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

// Dedup so a flapping connection doesn't spam the console.
final class RedisConnectionEventLogger implements AutoCloseable {

    private final Logger logger;
    private final AtomicBoolean up = new AtomicBoolean(false);
    private final AtomicLong lastReconnectAttempt = new AtomicLong(-1);
    private final AtomicLong lastReconnectFailure = new AtomicLong(-1);
    private volatile @Nullable Disposable subscription;

    RedisConnectionEventLogger(Logger logger) {
        this.logger = logger;
    }

    void attach(ClientResources resources) {
        close();

        this.subscription = resources.eventBus().get().subscribe(event -> {
            if (event instanceof ConnectionActivatedEvent) {
                lastReconnectAttempt.set(-1);
                lastReconnectFailure.set(-1);
                if (up.compareAndSet(false, true)) {
                    logger.info("Successfully connected to Redis.");
                }
            } else if (event instanceof DisconnectedEvent) {
                if (up.compareAndSet(true, false)) {
                    logger.severe("Redis connection was lost.");
                    lastReconnectAttempt.set(-1);
                    lastReconnectFailure.set(-1);
                }
            } else if (event instanceof ReconnectAttemptEvent reconnectAttempt) {
                long attempt = reconnectAttempt.getAttempt();
                if (lastReconnectAttempt.getAndSet(attempt) != attempt) {
                    logger.warning("Attempting to reconnect to Redis (attempt " + attempt + ").");
                }
            } else if (event instanceof ReconnectFailedEvent reconnectFailed) {
                long attempt = reconnectFailed.getAttempt();
                if (lastReconnectFailure.getAndSet(attempt) != attempt) {
                    Throwable cause = reconnectFailed.getCause();
                    logger.warning("Failed to reconnect to Redis (attempt " + attempt + "): "
                            + (cause == null ? "unknown error" : cause.getMessage()));
                }
            }
        });
    }

    @Override
    public void close() {
        Disposable sub = this.subscription;
        this.subscription = null;
        if (sub == null || sub.isDisposed()) return;
        try {
            sub.dispose();
        } catch (Exception ignored) {
        }
    }
}
