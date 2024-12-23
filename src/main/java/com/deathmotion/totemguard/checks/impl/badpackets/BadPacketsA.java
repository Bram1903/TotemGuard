/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.checks.impl.badpackets;

import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.PacketCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import net.kyori.adventure.text.Component;

@CheckData(name = "BadPacketsA", description = "Suspicious mod message")
public class BadPacketsA extends Check implements PacketCheck {

    private static final String SUSPICIOUS_CHANNEL_KEYWORD = "autototem";

    public BadPacketsA(final TotemPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isPluginMessage(event.getPacketType())) {
            return;
        }

        // Parse the packet and extract the channel name
        WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
        String channel = packet.getChannelName();
        if (channel == null || !channel.toLowerCase().contains(SUSPICIOUS_CHANNEL_KEYWORD)) {
            return;
        }

        fail(getCheckDetails(channel));
    }

    private boolean isPluginMessage(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PLUGIN_MESSAGE || packetType == PacketType.Configuration.Client.PLUGIN_MESSAGE;
    }

    private Component getCheckDetails(String channel) {
        return checkSettings.getCheckAlertMessage()
                .replaceText(builder -> builder
                        .matchLiteral("%channel%")
                        .replacement(channel));
    }
}
