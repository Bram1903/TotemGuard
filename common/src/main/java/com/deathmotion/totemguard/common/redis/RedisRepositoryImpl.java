package com.deathmotion.totemguard.common.redis;

import com.deathmotion.totemguard.api3.redis.RedisRepository;
import com.deathmotion.totemguard.common.redis.options.RedisOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

public final class RedisRepositoryImpl implements RedisRepository {

    private final RedisConnectionManager manager = new RedisConnectionManager();
    private boolean isEnabled = false;

    public RedisRepositoryImpl() {
        start();
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isConnected() {
        return isEnabled && manager.isConnected();
    }

    public @Nullable StatefulRedisConnection<byte[], byte[]> connection() {
        return manager.connection();
    }

    public @Nullable RedisCommands<byte[], byte[]> sync() {
        StatefulRedisConnection<byte[], byte[]> connection = connection();
        return (connection != null && connection.isOpen()) ? connection.sync() : null;
    }

    public @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection() {
        return manager.pubSubConnection();
    }

    @Blocking
    public void start() {
        RedisOptions options = new RedisOptions();
        if (!options.isEnabled()) return;
        isEnabled = true;
        manager.start(options);
    }

    @Blocking
    public void stop() {
        isEnabled = false;
        manager.stop();
    }

    @Blocking
    public void restart() {
        if (!isEnabled()) {
            manager.stop();
            return;
        }

        RedisOptions options = new RedisOptions();
        if (!options.isEnabled()) return;
        isEnabled = true;
        manager.restart(options);
    }
}