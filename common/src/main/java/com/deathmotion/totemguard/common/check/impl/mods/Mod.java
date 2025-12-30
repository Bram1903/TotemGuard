/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public class Mod extends CheckImpl implements PacketCheck {

    private static final AtomicLong SEQ = new AtomicLong(ThreadLocalRandom.current().nextLong());

    private static final String REGISTER_CHANNEL = "minecraft:register";

    // TODO: Pull this from the config later
    private static final List<ModSignature> SIGNATURES = List.of(
            new ModSignature(
                    "accurateblockplacement",
                    List.of("net.clayborn.accurateblockplacement.togglevanillaplacement"),
                    List.of()
            ),
            new ModSignature(
                    "autototem",
                    List.of(),
                    List.of("autototem")
            ),
            new ModSignature(
                    "tweakeroo",
                    List.of(),
                    List.of("servux:tweaks")
            )
    );

    private final ConcurrentHashMap<String, ModSignature> idToSignature = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> idToTranslationKey = new ConcurrentHashMap<>();

    public Mod(TGPlayer player) {
        super(player);

        for (ModSignature sig : SIGNATURES) {
            for (String translationKey : sig.translationKeys()) {
                String id = nextId();
                idToSignature.put(id, sig);
                idToTranslationKey.put(id, translationKey);
            }
        }
    }

    private static String nextId() {
        String s = Long.toUnsignedString(SEQ.getAndIncrement(), 36);
        return (s.length() <= 8) ? s : s.substring(s.length() - 8);
    }

    public void handle() {
        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            WrapperPlayServerBundle bundle = new WrapperPlayServerBundle();

            for (var entry : idToTranslationKey.entrySet()) {
                final String id = entry.getKey();
                final String translationKey = entry.getValue();

                boolean wasBundling = player.getData().isSendingBundlePacket();

                if (!wasBundling) {
                    player.getUser().sendPacket(bundle);
                }

                // TODO: Implement the sign exploit here

                if (!wasBundling) {
                    player.getUser().sendPacket(bundle);
                }
            }
        });
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        } else if (packetType == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        }
    }

    private void handlePluginMessage(String channel, byte[] data) {
        if (channel == null) return;

        String normalizedChannel = channel.toLowerCase();

        if (REGISTER_CHANNEL.equals(normalizedChannel)) {
            String payload = new String(data, StandardCharsets.UTF_8);

            for (String registered : payload.split("\0")) {
                checkKeywords(registered.toLowerCase());
            }
            return;
        }

        checkKeywords(normalizedChannel);
    }

    private void checkKeywords(String value) {
        for (ModSignature sig : SIGNATURES) {
            for (String keyword : sig.pluginMessageKeywords()) {
                if (keyword == null || keyword.isEmpty()) continue;

                if (value.contains(keyword.toLowerCase())) {
                    fail(sig.name());
                    return;
                }
            }
        }
    }
}
