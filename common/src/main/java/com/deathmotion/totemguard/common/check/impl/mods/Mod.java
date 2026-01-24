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

import com.deathmotion.totemguard.api.check.CheckType;
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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public final class Mod extends CheckImpl implements PacketCheck {

    private static final String REGISTER_CHANNEL = "minecraft:register";

    private final Set<String> flaggedMods = new HashSet<>();
    private final Set<String> pendingDetections = new HashSet<>();

    private final Map<String, Map<String, String>> translationIdsByMod = new HashMap<>();

    public Mod(TGPlayer player) {
        super(player);
        reload();
    }

    private static String normalize(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static String nextId() {
        String s = Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        return (s.length() <= 8) ? s : s.substring(s.length() - 8);
    }

    @Override
    public void reload() {
        translationIdsByMod.clear();

        for (Map.Entry<String, ModSignature> entry : ModSignatures.get().entrySet()) {
            String modId = entry.getKey();
            ModSignature sig = entry.getValue();

            if (sig.translations().isEmpty()) {
                continue;
            }

            Map<String, String> ids = new HashMap<>();
            for (String key : sig.translations()) {
                ids.put(nextId(), key);
            }

            translationIdsByMod.put(modId, ids);
        }
    }

    @SuppressWarnings("unused")
    public Map<String, Map<String, String>> translationIdsByMod() {
        return Collections.unmodifiableMap(translationIdsByMod);
    }

    public void handle() {
        flushDetections();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        } else if (type == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        }
    }

    private void handlePluginMessage(String channel, byte[] data) {
        if (channel == null) return;

        String normalizedChannel = normalize(channel);

        if (REGISTER_CHANNEL.equals(normalizedChannel)) {
            String payload = new String(data, StandardCharsets.UTF_8);
            for (String entry : payload.split("\0")) {
                detectFromValue(normalize(entry));
            }
        } else {
            detectFromValue(normalizedChannel);
        }

        flushDetections();
    }

    private void detectFromValue(String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        for (Map.Entry<String, ModSignature> modEntry : ModSignatures.get().entrySet()) {
            String modName = modEntry.getKey();

            if (flaggedMods.contains(modName)) {
                continue;
            }

            for (String keyword : modEntry.getValue().payloads()) {
                if (value.contains(keyword)) {
                    pendingDetections.add(modName);
                    break;
                }
            }
        }
    }

    private void flushDetections() {
        if (!player.isHasLoggedIn()) {
            return;
        }

        if (pendingDetections.isEmpty()) {
            return;
        }

        for (String mod : pendingDetections) {
            if (flaggedMods.add(mod)) {
                fail(mod);
            }
        }

        pendingDetections.clear();
    }
}
