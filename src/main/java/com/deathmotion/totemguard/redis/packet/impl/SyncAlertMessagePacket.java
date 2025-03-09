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

package com.deathmotion.totemguard.redis.packet.impl;

import com.deathmotion.totemguard.redis.packet.Packet;
import com.deathmotion.totemguard.redis.packet.Packets;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * Packet implementation for synchronizing alert messages.
 */
public class SyncAlertMessagePacket extends Packet<SyncAlertMessagePacket.AlertComponents> {

    @AllArgsConstructor
    public static class AlertComponents {
        public Component gameAlert;
        public Component consoleAlert;
    }

    public SyncAlertMessagePacket() {
        super(Packets.SYNC_ALERT_MESSAGE.getId());
    }

    @Override
    public AlertComponents read(ByteArrayDataInput input) {
        String gameAlert = input.readUTF();
        String consoleAlert = input.readUTF();

        return new AlertComponents(GsonComponentSerializer.gson().deserialize(gameAlert), GsonComponentSerializer.gson().deserialize(consoleAlert));
    }

    @Override
    public void writeData(ByteArrayDataOutput output, AlertComponents alertComponents) {
        String gameAlert = GsonComponentSerializer.gson().serialize(alertComponents.gameAlert);
        String consoleAlert = GsonComponentSerializer.gson().serialize(alertComponents.consoleAlert);

        output.writeUTF(gameAlert);
        output.writeUTF(consoleAlert);
    }
}

