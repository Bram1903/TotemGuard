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

package com.deathmotion.totemguard.common.fleet;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.redis.ConnectionStateListener;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.host.LoaderController;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Glue that attaches/detaches the {@link RedisFleetCache} from the loader's
 * {@link LoaderController} based on Redis connection state. Lives on the TG side; the
 * loader has no knowledge of this class.
 *
 * <p>Idempotent: attach is a no-op if the cache is already attached; detach a no-op if
 * already detached.</p>
 */
public final class FleetCacheLifecycle implements ConnectionStateListener {

    private final TGPlatform platform;
    private final RedisRepositoryImpl redis;
    private final UUID instanceId;
    private final Logger logger;

    private volatile RedisFleetCache cache;

    public FleetCacheLifecycle(TGPlatform platform, RedisRepositoryImpl redis, UUID instanceId, Logger logger) {
        this.platform = platform;
        this.redis = redis;
        this.instanceId = instanceId;
        this.logger = logger;
    }

    public void register() {
        redis.addStateListener(this);
        // Catch the case where Redis was already connected before we registered.
        if (redis.isConnected()) {
            attach();
        }
    }

    public void unregister() {
        redis.removeStateListener(this);
        detach();
    }

    @Override
    public void onConnected(RedisConnection connection) {
        attach();
    }

    @Override
    public void onDisconnected() {
        detach();
    }

    private void attach() {
        if (cache != null) return;
        if (platform.getPluginHost() == null) return;
        var controllerOpt = platform.getPluginHost().loaderController();
        if (controllerOpt.isEmpty()) {
            // Standalone mode, no loader present.
            return;
        }
        RedisFleetCache built = new RedisFleetCache(redis, instanceId, logger);
        try {
            controllerOpt.get().attachFleetCache(built);
            this.cache = built;
            logger.fine("Fleet cache attached to loader (instance " + instanceId + ").");
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to attach fleet cache to loader", t);
        }
    }

    private void detach() {
        RedisFleetCache current = this.cache;
        if (current == null) return;
        this.cache = null;
        if (platform.getPluginHost() != null) {
            var controllerOpt = platform.getPluginHost().loaderController();
            controllerOpt.ifPresent(controller -> {
                try {
                    controller.attachFleetCache(null);
                } catch (Throwable t) {
                    logger.log(Level.FINE, "Loader rejected fleet-cache detach", t);
                }
            });
        }
        try {
            current.detach();
        } catch (Throwable ignored) {
        }
    }
}
