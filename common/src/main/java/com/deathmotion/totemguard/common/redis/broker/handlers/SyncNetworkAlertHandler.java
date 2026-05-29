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

package com.deathmotion.totemguard.common.redis.broker.handlers;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.api.event.events.TGNetworkAlertEvent;
import com.deathmotion.totemguard.api.reload.Reloadable;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.EventBusImpl;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketProcessor;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketRegistry;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncNetworkAlertPacket;

public final class SyncNetworkAlertHandler implements PacketProcessor<SyncNetworkAlertPacket.Payload>, Reloadable {

    private final TGPlatform platform;
    private final RedisRepositoryImpl redisRepository;
    private final PacketRegistry registry;
    private final Packet<SyncNetworkAlertPacket.Payload> packet;

    public SyncNetworkAlertHandler(TGPlatform platform, RedisRepositoryImpl redisRepository, PacketRegistry registry) {
        this.platform = platform;
        this.redisRepository = redisRepository;
        this.registry = registry;
        this.packet = Packets.SYNC_NETWORK_ALERT.packet();
    }

    private static CheckType parseType(String name) {
        try {
            return CheckType.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return CheckType.UNSPECIFIED;
        }
    }

    @Override
    public void handle(SyncNetworkAlertPacket.Payload payload) {
        EventBusImpl eventBus = platform.getEventBus();
        if (eventBus == null) return;
        TGNetworkAlertEvent.Kind kind = payload.flag()
                ? TGNetworkAlertEvent.Kind.FLAG
                : TGNetworkAlertEvent.Kind.PUNISHMENT;
        eventBus.getNetworkAlert().fire(payload.playerUuid(), payload.playerName(), payload.checkName(),
                parseType(payload.checkType()), payload.violations(), payload.debug(), payload.serverName(),
                kind, true, payload.timestamp());
    }

    @Override
    public void reload() {
        registry.unregister(packet, this);
        if (redisRepository.shouldReceive(MessagingTopic.EVENTS)) {
            registry.registerProcessor(packet, this);
        }
    }
}
