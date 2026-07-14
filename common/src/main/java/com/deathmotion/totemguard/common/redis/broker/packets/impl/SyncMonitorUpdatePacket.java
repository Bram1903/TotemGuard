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

package com.deathmotion.totemguard.common.redis.broker.packets.impl;

import com.deathmotion.totemguard.common.features.monitor.MonitorSnapshot;
import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketIO;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SyncMonitorUpdatePacket extends Packet<MonitorSnapshot> {

    public SyncMonitorUpdatePacket(int id) {
        super(id, MessagingTopic.PRESENCE);
    }

    @Override
    public MonitorSnapshot read(ByteArrayDataInput input) {
        int writerProtocol = input.readInt();
        ServerVersion writerVersion = PacketIO.resolveServerVersion(writerProtocol);

        UUID targetUuid = PacketIO.readUUID(input);
        String playerName = input.readUTF();
        String serverName = input.readUTF();
        String clientVersion = input.readUTF();
        String clientBrand = input.readUTF();
        int selectedHotbarIndex = input.readInt();
        int mainHandSlot = input.readInt();
        String lastIssuer = input.readUTF();
        boolean inventoryOpen = input.readBoolean();
        int keepAlivePing = input.readInt();
        int transactionPing = input.readInt();
        int pendingTransactionCount = input.readInt();
        int itemCount = input.readInt();
        Map<Integer, ItemStack> items = new HashMap<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            int slot = input.readInt();
            items.put(slot, PacketIO.readItemStack(input, writerVersion));
        }
        ItemStack carried = PacketIO.readItemStack(input, writerVersion);
        int carriedSlot = input.readInt();
        long capturedAt = input.readLong();
        return new MonitorSnapshot(
                targetUuid, playerName, serverName, clientVersion, clientBrand,
                selectedHotbarIndex, mainHandSlot, lastIssuer,
                inventoryOpen, keepAlivePing, transactionPing, pendingTransactionCount,
                items, carried, carriedSlot, capturedAt);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, MonitorSnapshot snapshot) {
        ServerVersion writerVersion = PacketEvents.getAPI().getServerManager().getVersion();
        output.writeInt(writerVersion.getProtocolVersion());
        PacketIO.writeUUID(output, snapshot.targetUuid());
        output.writeUTF(snapshot.playerName());
        output.writeUTF(snapshot.serverName());
        output.writeUTF(snapshot.clientVersion());
        output.writeUTF(snapshot.clientBrand());
        output.writeInt(snapshot.selectedHotbarIndex());
        output.writeInt(snapshot.mainHandSlot());
        output.writeUTF(snapshot.lastIssuer());
        output.writeBoolean(snapshot.inventoryOpen());
        output.writeInt(snapshot.keepAlivePing());
        output.writeInt(snapshot.transactionPing());
        output.writeInt(snapshot.pendingTransactionCount());
        output.writeInt(snapshot.inventoryItems().size());
        for (Map.Entry<Integer, ItemStack> entry : snapshot.inventoryItems().entrySet()) {
            output.writeInt(entry.getKey());
            PacketIO.writeItemStack(output, entry.getValue(), writerVersion);
        }
        PacketIO.writeItemStack(output, snapshot.carriedItem(), writerVersion);
        output.writeInt(snapshot.carriedItemSlot());
        output.writeLong(snapshot.capturedAtMillis());
    }
}
