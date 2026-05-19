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

package com.deathmotion.totemguard.common.player.processor.inbound;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.deathmotion.totemguard.common.util.ChatUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;

import java.util.concurrent.ThreadLocalRandom;

public class InboundClientProcessor extends ProcessorInbound {

    private static final String BRAND_CHANNEL = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13) ? "minecraft:brand" : "MC|Brand";

    private static final String MARLOW_VERSION_CHANNEL = "marlowcrystal:version";
    private static final String MARLOW_CHALLENGE_CHANNEL = "marlowcrystal:challenge";
    private static final String MARLOW_RESPONSE_CHANNEL = "marlowcrystal:challenge_response";

    private boolean hasBrand;
    private boolean awaitingMarlowResponse;
    private int marlowChallengeId;

    public InboundClientProcessor(TGPlayer player) {
        super(player);
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            dispatch(packet.getChannelName(), packet.getData(), true);
        } else if (type == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            dispatch(packet.getChannelName(), packet.getData(), false);
        }
    }

    private void dispatch(String channel, byte[] data, boolean play) {
        if (channel == null) return;

        if (!hasBrand && BRAND_CHANNEL.equals(channel)) {
            handleBrand(data);
            return;
        }
        if (!play || player.isMarlowOptimizer()) return;

        if (MARLOW_VERSION_CHANNEL.equals(channel)) {
            marlowChallengeId = ThreadLocalRandom.current().nextInt();
            awaitingMarlowResponse = true;
            byte[] payload = new byte[]{
                    (byte) (marlowChallengeId >>> 24),
                    (byte) (marlowChallengeId >>> 16),
                    (byte) (marlowChallengeId >>> 8),
                    (byte) marlowChallengeId
            };
            player.getUser().sendPacket(new WrapperPlayServerPluginMessage(MARLOW_CHALLENGE_CHANNEL, payload));
        } else if (MARLOW_RESPONSE_CHANNEL.equals(channel)) {
            if (!awaitingMarlowResponse) return;
            awaitingMarlowResponse = false;
            if (data == null || data.length != 4) return;
            int received = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
            if (received != marlowChallengeId) return;
            player.setMarlowOptimizer(true);
        }
    }

    private void handleBrand(byte[] data) {
        String brand = "Vanilla";

        if (data.length > 64 || data.length == 0) {
            player.setClientBrand(brand);
            hasBrand = true;
            return;
        }

        byte[] minusLength = new byte[data.length - 1];
        System.arraycopy(data, 1, minusLength, 0, minusLength.length);

        brand = new String(minusLength).replace(" (Velocity)", "");
        brand = ChatUtil.stripColor(brand);

        player.setClientBrand(brand);
        hasBrand = true;
    }
}
