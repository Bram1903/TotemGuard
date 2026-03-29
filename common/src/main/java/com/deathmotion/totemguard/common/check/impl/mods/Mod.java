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
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public final class Mod extends CheckImpl implements PacketCheck {

    private static final String REGISTER_CHANNEL = "minecraft:register";

    private final Set<String> detectedMods = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingDetections = ConcurrentHashMap.newKeySet();

    private volatile Map<String, ModDefinition> definitionsById = Map.of();

    public Mod(TGPlayer player) {
        super(player);
        punishable = true;
        reload();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        final String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    @Override
    public void reload() {
        super.reload();

        definitionsById = ModRegistry.getDefinitions();
        detectedMods.removeIf(modId -> !definitionsById.containsKey(modId));
        pendingDetections.removeIf(modId -> !definitionsById.containsKey(modId));
    }

    public void handle() {
        flushPendingDetections();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.PLUGIN_MESSAGE) {
            final WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
            return;
        }

        if (packetType == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            final WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        }
    }

    private void handlePluginMessage(String channelName, byte[] data) {
        final String normalizedChannelName = normalize(channelName);
        if (normalizedChannelName == null) {
            return;
        }

        if (REGISTER_CHANNEL.equals(normalizedChannelName)) {
            final String payload = new String(data, StandardCharsets.UTF_8);
            for (String registeredChannel : payload.split("\0")) {
                queueDetections(registeredChannel);
            }
        } else {
            queueDetections(normalizedChannelName);
        }

        flushPendingDetections();
    }

    private void queueDetections(String value) {
        final String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return;
        }

        for (ModDefinition definition : definitionsById.values()) {
            if (detectedMods.contains(definition.id()) || !definition.hasPayloads()) {
                continue;
            }

            if (definition.matchesPayload(normalizedValue)) {
                pendingDetections.add(definition.id());
            }
        }
    }

    private void flushPendingDetections() {
        if (!player.isHasLoggedIn() || pendingDetections.isEmpty()) {
            return;
        }

        for (String modId : new ArrayList<>(pendingDetections)) {
            if (!pendingDetections.remove(modId)) {
                continue;
            }

            if (detectedMods.add(modId)) {
                fail(modId);
            }
        }
    }
}
