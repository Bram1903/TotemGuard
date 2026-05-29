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

package com.deathmotion.totemguard.common.features.alert;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.api.event.events.TGNetworkAlertEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.EventBusImpl;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncNetworkAlertPacket;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class NetworkAlertBroadcaster {

    private NetworkAlertBroadcaster() {
    }

    public static void broadcast(TGPlatform platform, UUID playerUuid, @Nullable String playerName,
                                 String checkName, CheckType checkType, int violations, @Nullable String debug,
                                 TGNetworkAlertEvent.Kind kind) {
        if (playerName == null) return;
        EventBusImpl eventBus = platform.getEventBus();
        if (eventBus == null) return;

        String serverName = localServerName(platform);
        long timestamp = System.currentTimeMillis();

        eventBus.getNetworkAlert().fire(playerUuid, playerName, checkName, checkType, violations, debug,
                serverName, kind, false, timestamp);

        RedisRepositoryImpl redis = platform.getRedisRepository();
        if (redis != null && redis.isConnected() && redis.shouldSend(MessagingTopic.EVENTS)) {
            boolean flag = kind == TGNetworkAlertEvent.Kind.FLAG;
            platform.getScheduler().runAsyncTask(() -> redis.publish(
                    Packets.SYNC_NETWORK_ALERT.packet(),
                    new SyncNetworkAlertPacket.Payload(playerUuid, playerName, checkName, checkType.name(),
                            violations, debug, serverName, flag, timestamp)));
        }
    }

    private static String localServerName(TGPlatform platform) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        return presence != null ? presence.getLocalServerName() : "";
    }
}
