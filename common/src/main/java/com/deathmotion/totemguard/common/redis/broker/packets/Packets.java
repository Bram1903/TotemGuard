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

package com.deathmotion.totemguard.common.redis.broker.packets;

import com.deathmotion.totemguard.common.redis.broker.packets.impl.*;

public enum Packets {

    SYNC_ALERT_MESSAGE(new SyncAlertMessagePacket(1)),
    SYNC_UPDATE_AVAILABLE(new SyncUpdateAvailablePacket(2)),
    SYNC_SERVER_OFFLINE(new SyncServerOfflinePacket(4)),
    SYNC_PLAYER_JOIN(new SyncPlayerJoinPacket(5)),
    SYNC_PLAYER_OFFLINE(new SyncPlayerOfflinePacket(6)),
    SYNC_TELEPORT_REQUEST(new SyncTeleportRequestPacket(7)),
    SYNC_MONITOR_SUBSCRIBE(new SyncMonitorSubscribePacket(8)),
    SYNC_MONITOR_UNSUBSCRIBE(new SyncMonitorUnsubscribePacket(9)),
    SYNC_MONITOR_UPDATE(new SyncMonitorUpdatePacket(10)),
    SYNC_FOCUS_ALERT(new SyncFocusAlertPacket(11)),
    SYNC_CHECK_REQUEST(new SyncCheckRequestPacket(12)),
    SYNC_CHECK_RESULT(new SyncCheckResultPacket(13));

    private final Packet<?> packet;

    Packets(Packet<?> packet) {
        this.packet = packet;
    }

    public int getId() {
        return packet.getId();
    }

    @SuppressWarnings("unchecked")
    public <T> Packet<T> packet() {
        return (Packet<T>) packet;
    }
}
