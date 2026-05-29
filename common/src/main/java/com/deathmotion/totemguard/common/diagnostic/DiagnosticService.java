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

package com.deathmotion.totemguard.common.diagnostic;

import com.deathmotion.totemguard.api.event.events.TGDiagnosticEvent.Severity;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.EventBusImpl;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncDiagnosticPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DiagnosticService {

    private final TGPlatform platform;
    private final Map<String, Boolean> health = new ConcurrentHashMap<>();

    public DiagnosticService(@NotNull TGPlatform platform) {
        this.platform = platform;
    }

    private static String render(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public void report(@NotNull Severity severity, @NotNull String subsystem, @NotNull String message,
                       @Nullable Throwable error) {
        String stackTrace = error == null ? null : render(error);
        String serverName = localServerName();
        long timestamp = System.currentTimeMillis();

        EventBusImpl eventBus = platform.getEventBus();
        if (eventBus != null) {
            eventBus.getDiagnostic().fire(severity, subsystem, message, stackTrace, serverName, false, timestamp);
        }

        RedisRepositoryImpl redis = platform.getRedisRepository();
        if (redis != null && redis.isConnected() && redis.shouldSend(MessagingTopic.EVENTS)) {
            platform.getScheduler().runAsyncTask(() -> redis.publish(
                    Packets.SYNC_DIAGNOSTIC.packet(),
                    new SyncDiagnosticPacket.Payload(severity.name(), subsystem, message, stackTrace, serverName, timestamp)));
        }
    }

    public void unhealthy(@NotNull Severity severity, @NotNull String subsystem, @NotNull String message,
                          @Nullable Throwable error) {
        Boolean previous = health.put(subsystem, Boolean.FALSE);
        if (!Boolean.FALSE.equals(previous)) {
            report(severity, subsystem, message, error);
        }
    }

    public void healthy(@NotNull String subsystem, @NotNull String recoveryMessage) {
        Boolean previous = health.put(subsystem, Boolean.TRUE);
        if (Boolean.FALSE.equals(previous)) {
            report(Severity.INFO, subsystem, recoveryMessage, null);
        }
    }

    private String localServerName() {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        return presence != null ? presence.getLocalServerName() : "";
    }
}
