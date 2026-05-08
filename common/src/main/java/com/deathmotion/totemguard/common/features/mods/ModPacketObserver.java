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

package com.deathmotion.totemguard.common.features.mods;

import com.deathmotion.totemguard.api.mod.ModDetectionMethod;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientNameItem;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ModPacketObserver extends PacketListenerAbstract {

    private static final String REGISTER_CHANNEL = "minecraft:register";

    private final ModDetectionService service;

    public ModPacketObserver(ModDetectionService service) {
        super(PacketListenerPriority.LOW);
        this.service = service;
    }

    private static String normalizeChannel(String value) {
        if (value == null) return null;
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isBlank() ? null : trimmed;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        java.util.UUID uuid = event.getUser().getUUID();
        if (uuid == null) return;
        ModSession session = service.sessionFor(uuid);
        if (session == null) return;

        ConnectionState state = event.getConnectionState();
        PacketTypeCommon type = event.getPacketType();

        if (state == ConnectionState.CONFIGURATION) {
            if (type == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
                WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
                handlePluginMessage(session, packet.getChannelName(), packet.getData());
            }
            return;
        }

        if (state != ConnectionState.PLAY) return;

        if (type == PacketType.Play.Client.NAME_ITEM) {
            ModTranslationDetector.ConsumeResult result = session.translationDetector().tryConsumeResponse(
                    new WrapperPlayClientNameItem(event),
                    () -> event.setCancelled(true)
            );
            if (result.outcome() == ModTranslationDetector.ResponseOutcome.OURS_DETECTED && result.mod() != null) {
                service.recordDetection(session, result.mod(), ModDetectionMethod.TRANSLATION);
            }
            return;
        }

        if (type == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(session, packet.getChannelName(), packet.getData());
            return;
        }

        if (isBoundaryPacket(session, type)) {
            service.onTickBoundary(session);
        }
    }

    private boolean isBoundaryPacket(ModSession session, PacketTypeCommon type) {
        if (session.state() != ModSession.State.AWAITING_BOUNDARY) return false;
        TGPlayer player = session.player();
        if (player.supportsEndTick()) {
            return type == PacketType.Play.Client.CLIENT_TICK_END;
        }
        return WrapperPlayClientPlayerFlying.isFlying(type);
    }

    private void handlePluginMessage(ModSession session, String channelName, byte[] data) {
        String channel = normalizeChannel(channelName);
        if (channel == null) return;

        if (REGISTER_CHANNEL.equals(channel)) {
            String payload = new String(data, StandardCharsets.UTF_8);
            for (String registered : payload.split("\0")) {
                matchAndRecord(session, registered, ModDetectionMethod.PLUGIN_CHANNEL_REGISTRATION);
            }
            return;
        }
        matchAndRecord(session, channel, ModDetectionMethod.PLUGIN_MESSAGE);
    }

    private void matchAndRecord(ModSession session, String rawChannel, ModDetectionMethod method) {
        String channel = normalizeChannel(rawChannel);
        if (channel == null) return;
        ModDefinition mod = session.snapshot().matchPayload(channel);
        if (mod != null) service.recordDetection(session, mod, method);
    }
}
