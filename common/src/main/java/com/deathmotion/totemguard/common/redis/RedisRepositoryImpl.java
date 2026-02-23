package com.deathmotion.totemguard.common.redis;

import com.deathmotion.totemguard.api3.redis.RedisRepository;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

public final class RedisRepositoryImpl implements RedisRepository {

    private final RedisConnectionManager manager = new RedisConnectionManager();
    private final RedisOptions options = new RedisOptions();

    @Override
    public boolean isEnabled() {
        return options.isEnabled();
    }

    @Override
    public boolean isConnected() {
        return options.isEnabled() && manager.isConnected();
    }

    public @Nullable RedisAsyncCommands<byte[], byte[]> async() {
        if (!options.isEnabled()) return null;
        StatefulRedisConnection<byte[], byte[]> conn = manager.connection();
        return (conn != null && conn.isOpen()) ? conn.async() : null;
    }

    public @Nullable StatefulRedisConnection<byte[], byte[]> connection() {
        return manager.connection();
    }

    public @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection() {
        return manager.pubSubConnection();
    }

    @Blocking
    public void start() {
        if (!options.isEnabled()) return;
        manager.start();
    }

    @Blocking
    public void stop() {
        manager.stop();
    }

    @Blocking
    public void restart() {
        if (!options.isEnabled()) {
            manager.stop();
            return;
        }
        manager.restart();
    }
}