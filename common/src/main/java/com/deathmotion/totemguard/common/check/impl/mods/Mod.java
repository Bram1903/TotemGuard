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

package com.deathmotion.totemguard.common.check.impl.mods;

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.punishment.PunishmentCommand;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientNameItem;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public final class Mod extends CheckImpl implements PacketCheck {

    private static final String REGISTER_CHANNEL = "minecraft:register";

    private final ModTranslationDetector translationDetector;
    private final Set<String> alreadyFlaggedModIds = ConcurrentHashMap.newKeySet();
    private final Deque<ModDefinition> pendingDetections = new ArrayDeque<>();
    private final Object pendingLock = new Object();

    private volatile ModRegistry.Snapshot snapshot = ModRegistry.snapshot();

    public Mod(TGPlayer player) {
        super(player);
        punishable = false;
        this.translationDetector = new ModTranslationDetector(player);
        this.translationDetector.rebuild(snapshot);
    }

    private static String normalizeChannel(String value) {
        if (value == null) return null;
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isBlank() ? null : trimmed;
    }

    @Override
    public void reload() {
        super.reload();
        punishable = false;
        snapshot = ModRegistry.snapshot();
        alreadyFlaggedModIds.removeIf(id -> !snapshot.definitions().containsKey(id));
        translationDetector.rebuild(snapshot);
    }

    public void handle() {
        flushPending();
        translationDetector.start();
    }

    public boolean isDetectionActive() {
        return translationDetector.isActive();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.NAME_ITEM) {
            ModDefinition mod = translationDetector.tryConsumeResponse(
                    new WrapperPlayClientNameItem(event),
                    () -> event.setCancelled(true)
            );
            if (mod != null) recordDetection(mod);
            return;
        }

        if (type == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
            return;
        }

        if (type == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        }
    }

    private void handlePluginMessage(String channelName, byte[] data) {
        String channel = normalizeChannel(channelName);
        if (channel == null) return;

        if (REGISTER_CHANNEL.equals(channel)) {
            String payload = new String(data, StandardCharsets.UTF_8);
            for (String registered : payload.split("\0")) {
                matchAndQueue(registered);
            }
            return;
        }
        matchAndQueue(channel);
    }

    private void matchAndQueue(String rawChannel) {
        String channel = normalizeChannel(rawChannel);
        if (channel == null) return;
        ModDefinition mod = snapshot.matchPayload(channel);
        if (mod != null) recordDetection(mod);
    }

    private void recordDetection(ModDefinition mod) {
        if (!alreadyFlaggedModIds.add(mod.id())) return;

        if (!player.isHasLoggedIn()) {
            synchronized (pendingLock) {
                pendingDetections.add(mod);
            }
            return;
        }
        process(mod);
    }

    private void flushPending() {
        if (!player.isHasLoggedIn()) return;
        ModDefinition next;
        while (true) {
            synchronized (pendingLock) {
                next = pendingDetections.pollFirst();
            }
            if (next == null) return;
            process(next);
        }
    }

    private void process(ModDefinition mod) {
        if (!fail(mod.id())) return;
        applyPunishment(mod);
    }

    private void applyPunishment(ModDefinition mod) {
        ModSeverity severity = mod.severity();
        if (severity == ModSeverity.LOG) return;

        ModRegistry.Snapshot active = snapshot;
        PunishmentCommand command = switch (severity) {
            case LOG -> null;
            case KICK -> active.kickCommand();
            case BAN -> active.banCommand();
            case KICK_THEN_BAN -> escalateKickThenBan(mod, active);
        };
        if (command == null) return;

        Map<String, Object> extras = Map.of("tg_mod", mod.id());
        platform.getPunishmentRepository().punishWith(this, List.of(command), mod.id(), extras);
    }

    private PunishmentCommand escalateKickThenBan(ModDefinition mod, ModRegistry.Snapshot active) {
        CacheRepositoryImpl cache = platform.getCacheRepository();
        String key = CacheKeys.modKickRecord(player.getUuid(), mod.id());
        Duration window = active.kickThenBanWindow();

        if (cache.contains(key)) {
            cache.remove(key);
            return active.banCommand();
        }
        cache.put(key, Boolean.TRUE, CacheCodecs.BOOLEAN, window);
        return active.kickCommand();
    }
}
