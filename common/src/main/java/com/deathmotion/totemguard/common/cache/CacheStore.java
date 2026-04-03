package com.deathmotion.totemguard.common.cache;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.logging.Level;

public final class CacheStore<K, V> {

    private final RedisRepositoryImpl redisRepository = TGPlatform.getInstance().getRedisRepository();

    private final String name;
    private final IntSupplier ttlSupplier;
    private final BooleanSupplier enabledSupplier;
    private final LongSupplier maxEntriesSupplier;

    private final Function<K, byte[]> redisKeyFactory;
    private final ThrowingEncoder<V> encoder;
    private final ThrowingDecoder<V> decoder;

    private volatile Cache<K, byte[]> localCache;

    public CacheStore(
            String name,
            IntSupplier ttlSupplier,
            BooleanSupplier enabledSupplier,
            LongSupplier maxEntriesSupplier,
            Function<K, byte[]> redisKeyFactory,
            ThrowingEncoder<V> encoder,
            ThrowingDecoder<V> decoder
    ) {
        this.name = name;
        this.ttlSupplier = ttlSupplier;
        this.enabledSupplier = enabledSupplier;
        this.maxEntriesSupplier = maxEntriesSupplier;
        this.redisKeyFactory = redisKeyFactory;
        this.encoder = encoder;
        this.decoder = decoder;

        this.localCache = buildLocalCache();
    }

    public void put(K key, V value) {
        if (!isActive()) return;

        byte[] encoded = encodeValue(key, value);
        if (encoded == null) return;

        localCache.put(key, encoded);

        RedisCommands<byte[], byte[]> redis = redisSync();
        if (redis == null) return;

        try {
            redis.setex(redisKeyFactory.apply(key), ttl(), encoded);
        } catch (Exception exception) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING, "Failed to set " + name + " for key " + key, exception);
        }
    }

    public boolean putIfAbsent(K key, V value) {
        if (!isActive()) {
            return true;
        }

        byte[] encoded = encodeValue(key, value);
        if (encoded == null) {
            return false;
        }

        RedisCommands<byte[], byte[]> redis = redisSync();
        if (redis != null) {
            try {
                String response = redis.set(redisKeyFactory.apply(key), encoded, SetArgs.Builder.nx().ex(ttl()));
                boolean stored = "OK".equalsIgnoreCase(response);

                if (stored) {
                    localCache.put(key, encoded);
                    return true;
                }

                byte[] existing = redis.get(redisKeyFactory.apply(key));
                if (existing != null) {
                    localCache.put(key, existing);
                }

                return false;
            } catch (Exception exception) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING, "Failed to claim " + name + " for key " + key, exception);
                return false;
            }
        }

        return localCache.asMap().putIfAbsent(key, encoded) == null;
    }

    public @Nullable V get(K key) {
        if (!isActive()) return null;

        RedisCommands<byte[], byte[]> redis = redisSync();
        if (redis != null) {
            try {
                byte[] raw = redis.getex(redisKeyFactory.apply(key), GetExArgs.Builder.ex(ttl()));
                if (raw == null) return null;

                localCache.put(key, raw);
                return decode(key, raw);
            } catch (Exception exception) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING, "Failed to get " + name + " for key " + key, exception);
            }
        }

        byte[] local = localCache.getIfPresent(key);
        if (local == null) return null;

        return decode(key, local);
    }

    public void remove(K key) {
        localCache.invalidate(key);

        RedisCommands<byte[], byte[]> redis = redisSync();
        if (redis == null) return;

        try {
            redis.del(redisKeyFactory.apply(key));
        } catch (Exception exception) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING, "Failed to remove " + name + " for key " + key, exception);
        }
    }

    public void reloadLocalCache() {
        Cache<K, byte[]> oldCache = this.localCache;
        Map<K, byte[]> entries = oldCache.asMap();

        Cache<K, byte[]> newCache = buildLocalCache();
        if (isActive()) {
            newCache.putAll(entries);
        }

        oldCache.invalidateAll();
        this.localCache = newCache;
    }

    public Cache<K, byte[]> buildLocalCache() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().maximumSize(Math.max(1L, maxEntriesSupplier.getAsLong()));

        if (ttl() > 0) {
            builder.expireAfterAccess(ttl(), TimeUnit.SECONDS);
        }

        return builder.build();
    }

    public @Nullable V decode(K key, byte[] payload) {
        try {
            return decoder.decode(payload);
        } catch (Exception exception) {
            localCache.invalidate(key);
            TGPlatform.getInstance().getLogger().log(Level.WARNING, "Failed to decode " + name + " for key " + key, exception);
            return null;
        }
    }

    private @Nullable byte[] encodeValue(K key, V value) {
        try {
            return encoder.encode(value);
        } catch (Exception exception) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING, "Failed to encode " + name + " for key " + key, exception);
            return null;
        }
    }

    private int ttl() {
        return Math.max(0, ttlSupplier.getAsInt());
    }

    private boolean isActive() {
        return enabledSupplier.getAsBoolean() && ttl() > 0;
    }

    private @Nullable RedisCommands<byte[], byte[]> redisSync() {
        if (!redisRepository.isConnected()) return null;
        return redisRepository.sync();
    }

    @FunctionalInterface
    public interface ThrowingEncoder<T> {
        byte[] encode(T value) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingDecoder<T> {
        T decode(byte[] value) throws Exception;
    }
}
