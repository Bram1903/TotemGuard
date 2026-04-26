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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.RedisOptions;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import org.jetbrains.annotations.Nullable;
import reactor.core.Disposable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

// Lettuce's auto-reconnect only covers drops on established channels — initial-connect retry is on us.
final class RedisConnectionManager {

    private static final ByteArrayCodec CODEC = new ByteArrayCodec();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(30);

    private final Object lock = new Object();
    private final List<ConnectionStateListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean lettuceUp = new AtomicBoolean(false);
    private final Logger logger;

    private @Nullable ScheduledExecutorService executor;
    private @Nullable ClientResources resources;
    private @Nullable RedisClient client;
    private @Nullable RedisConnectionEventLogger eventLogger;
    private @Nullable Disposable eventSubscription;
    private @Nullable RedisOptions options;
    private boolean started;

    private volatile @Nullable RedisConnection current;

    RedisConnectionManager() {
        this.logger = TGPlatform.getInstance().getLogger();
    }

    void addListener(ConnectionStateListener listener) {
        listeners.add(listener);

        ScheduledExecutorService exec;
        RedisConnection snapshot;
        boolean up;
        synchronized (lock) {
            exec = executor;
            snapshot = current;
            up = lettuceUp.get();
        }
        if (!up || exec == null || snapshot == null) {
            return;
        }
        exec.execute(() -> dispatchConnected(listener, snapshot));
    }

    void removeListener(ConnectionStateListener listener) {
        listeners.remove(listener);
    }

    boolean isConnected() {
        RedisConnection snapshot = current;
        return lettuceUp.get() && snapshot != null && snapshot.isOpen();
    }

    @Nullable RedisConnection connection() {
        return current;
    }

    void start(RedisOptions newOptions) {
        start(newOptions, false);
    }

    // blocking=true runs the initial connect on the calling thread so the
    // connection is established before this method returns. Retries on failure
    // still go through the executor.
    void start(RedisOptions newOptions, boolean blocking) {
        synchronized (lock) {
            if (started) return;
            started = true;
            options = newOptions;
            executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "TotemGuard-Redis");
                thread.setDaemon(true);
                return thread;
            });
        }
        if (blocking) {
            connectAttempt();
        } else {
            scheduleConnect(0L);
        }
    }

    void stop() {
        ScheduledExecutorService executorRef;
        ClientResources resourcesRef;
        RedisClient clientRef;
        Disposable subRef;
        RedisConnectionEventLogger loggerRef;
        RedisConnection connRef;

        synchronized (lock) {
            if (!started && executor == null) return;
            started = false;
            options = null;
            executorRef = executor;
            executor = null;
            resourcesRef = resources;
            resources = null;
            clientRef = client;
            client = null;
            subRef = eventSubscription;
            eventSubscription = null;
            loggerRef = eventLogger;
            eventLogger = null;
            connRef = current;
            current = null;
        }

        boolean wasUp = lettuceUp.compareAndSet(true, false);
        if (wasUp) {
            notifyDisconnected();
        }

        if (subRef != null) {
            try {
                subRef.dispose();
            } catch (Exception ignored) {
            }
        }
        if (loggerRef != null) {
            try {
                loggerRef.close();
            } catch (Exception ignored) {
            }
        }
        if (connRef != null) {
            closeConnectionQuietly(connRef);
        }
        if (clientRef != null) {
            try {
                clientRef.shutdown();
            } catch (Exception ignored) {
            }
        }
        if (resourcesRef != null) {
            try {
                resourcesRef.shutdown();
            } catch (Exception ignored) {
            }
        }
        if (executorRef != null) {
            executorRef.shutdownNow();
            try {
                executorRef.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void restart(RedisOptions newOptions) {
        stop();
        start(newOptions);
    }

    private void scheduleConnect(long delaySeconds) {
        ScheduledExecutorService exec;
        synchronized (lock) {
            if (!started) return;
            exec = executor;
        }
        if (exec == null || exec.isShutdown()) return;
        try {
            exec.schedule(this::connectAttempt, delaySeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private void connectAttempt() {
        RedisOptions opts;
        synchronized (lock) {
            if (!started) return;
            if (current != null) return;
            opts = options;
        }
        if (opts == null) return;

        ClientResources newResources = ClientResources.create();
        RedisClient newClient = RedisClient.create(newResources, buildUri(opts));
        newClient.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build())
                .build());

        RedisConnectionEventLogger newEventLogger = new RedisConnectionEventLogger(logger);
        newEventLogger.attach(newResources);

        StatefulRedisConnection<byte[], byte[]> commands = null;
        StatefulRedisPubSubConnection<byte[], byte[]> pubSub = null;
        Disposable transitionSub = null;

        try {
            commands = newClient.connect(CODEC);
            pubSub = newClient.connectPubSub(CODEC);
            transitionSub = newResources.eventBus().get().subscribe(this::onLettuceEvent);

            RedisConnection conn = new RedisConnection(commands, pubSub);

            synchronized (lock) {
                if (!started) {
                    closeConnectionQuietly(conn);
                    transitionSub.dispose();
                    newEventLogger.close();
                    safeShutdown(newClient);
                    safeShutdown(newResources);
                    return;
                }
                resources = newResources;
                client = newClient;
                eventLogger = newEventLogger;
                eventSubscription = transitionSub;
                current = conn;
            }

            lettuceUp.set(true);
            notifyConnected(conn);
        } catch (Exception ex) {
            if (transitionSub != null) transitionSub.dispose();
            newEventLogger.close();
            closeConnectionQuietly(commands, pubSub);
            safeShutdown(newClient);
            safeShutdown(newResources);

            logger.warning(
                    "Failed to connect to Redis (" + ex.getClass().getSimpleName() + ": "
                            + ex.getMessage() + ") — retrying in " + RETRY_INTERVAL.toSeconds() + "s");
            scheduleConnect(RETRY_INTERVAL.toSeconds());
        }
    }

    private void onLettuceEvent(Event event) {
        if (event instanceof DisconnectedEvent) {
            handleLettuceDown();
        } else if (event instanceof ConnectionActivatedEvent) {
            handleLettuceUp();
        }
    }

    private void handleLettuceDown() {
        if (lettuceUp.compareAndSet(true, false)) {
            notifyDisconnected();
        }
    }

    private void handleLettuceUp() {
        RedisConnection snapshot = current;
        if (snapshot == null) return;
        if (!lettuceUp.compareAndSet(false, true)) return;

        ScheduledExecutorService exec;
        synchronized (lock) {
            exec = executor;
        }
        if (exec == null || exec.isShutdown()) {
            notifyConnected(snapshot);
            return;
        }
        try {
            exec.execute(() -> notifyConnected(snapshot));
        } catch (Exception ignored) {
            notifyConnected(snapshot);
        }
    }

    private void notifyConnected(RedisConnection conn) {
        for (ConnectionStateListener listener : listeners) {
            dispatchConnected(listener, conn);
        }
    }

    private void notifyDisconnected() {
        for (ConnectionStateListener listener : listeners) {
            try {
                listener.onDisconnected();
            } catch (Exception ex) {
                logger.log(Level.WARNING,
                        "Redis state listener threw on disconnect (" + listener.getClass().getSimpleName() + ")", ex);
            }
        }
    }

    private void dispatchConnected(ConnectionStateListener listener, RedisConnection conn) {
        try {
            listener.onConnected(conn);
        } catch (Exception ex) {
            logger.log(Level.WARNING,
                    "Redis state listener threw on connect (" + listener.getClass().getSimpleName() + ")", ex);
        }
    }

    private RedisURI buildUri(RedisOptions opts) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(opts.host())
                .withPort(opts.port())
                .withTimeout(COMMAND_TIMEOUT);

        String username = opts.username();
        String password = opts.password();

        if (!password.isBlank()) {
            if (!username.isBlank()) {
                builder.withAuthentication(username, password);
            } else {
                builder.withPassword(password.toCharArray());
            }
        }

        return builder.build();
    }

    private void closeConnectionQuietly(RedisConnection conn) {
        closeConnectionQuietly(conn.commands(), conn.pubSub());
    }

    private void closeConnectionQuietly(
            @Nullable StatefulRedisConnection<byte[], byte[]> commands,
            @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSub
    ) {
        if (pubSub != null) {
            try {
                pubSub.close();
            } catch (Exception ignored) {
            }
        }
        if (commands != null) {
            try {
                commands.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void safeShutdown(RedisClient client) {
        try {
            client.shutdown();
        } catch (Exception ignored) {
        }
    }

    private void safeShutdown(ClientResources resources) {
        try {
            resources.shutdown();
        } catch (Exception ignored) {
        }
    }
}
