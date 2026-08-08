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

package com.deathmotion.totemguard.common.util;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessageLegacy;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_16;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ActionBars {

    private static final UUID ZERO_UUID = new UUID(0L, 0L);

    private ActionBars() {
    }

    public static void send(@NotNull User user, @NotNull Component message) {
        if (user.getEncoderState() != ConnectionState.PLAY) return;
        user.sendPacket(buildPacket(user, message));
    }

    private static PacketWrapper<?> buildPacket(User user, Component message) {
        ClientVersion version = user.getPacketVersion();
        if (version.isNewerThanOrEquals(ClientVersion.V_1_19)) {
            return new WrapperPlayServerSystemChatMessage(true, message);
        }
        ChatMessage chatMessage = version.isNewerThanOrEquals(ClientVersion.V_1_16)
                ? new ChatMessage_v1_16(message, ChatTypes.GAME_INFO, ZERO_UUID)
                : new ChatMessageLegacy(message, ChatTypes.GAME_INFO);
        return new WrapperPlayServerChatMessage(chatMessage);
    }
}
