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
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public class Mod extends CheckImpl implements PacketCheck {

    private static final String REGISTER_CHANNEL = "minecraft:register";

    private static final List<ModSignature> SIGNATURES = List.of(
            new ModSignature("accurateblockplacement",
                    List.of("net.clayborn.accurateblockplacement.togglevanillaplacement"),
                    List.of()),
            new ModSignature("autototem", List.of(), List.of("autototem")),
            new ModSignature("tweakeroo", List.of(), List.of("servux:tweaks"))
    );

    public Mod(TGPlayer player) {
        super(player);
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

        String normalized = channel.toLowerCase();

        if (REGISTER_CHANNEL.equals(normalized)) {
            String payload = new String(data, StandardCharsets.UTF_8);
            for (String entry : payload.split("\0")) {
                checkKeywords(entry.toLowerCase());
            }
            return;
        }

        checkKeywords(normalized);
    }

    private void checkKeywords(String value) {
        for (ModSignature sig : SIGNATURES) {
            for (String keyword : sig.pluginMessageKeywords()) {
                if (keyword != null && !keyword.isEmpty() && value.contains(keyword.toLowerCase())) {
                    fail(sig.name());
                    return;
                }
            }
        }
    }
}
