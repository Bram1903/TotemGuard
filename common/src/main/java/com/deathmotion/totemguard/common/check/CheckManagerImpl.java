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

package com.deathmotion.totemguard.common.check;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.event.Event;
import com.deathmotion.totemguard.common.check.impl.autototem.AutoTotemA;
import com.deathmotion.totemguard.common.check.impl.protocol.ProtocolA;
import com.deathmotion.totemguard.common.check.impl.protocol.ProtocolB;
import com.deathmotion.totemguard.common.check.impl.protocol.ProtocolC;
import com.deathmotion.totemguard.common.check.type.ExtendedCheck;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

public class CheckManagerImpl {

    public ClassToInstanceMap<Check> allChecks;
    ClassToInstanceMap<PacketCheck> packetChecks;
    ClassToInstanceMap<ExtendedCheck> extendedChecks;

    public CheckManagerImpl(TGPlayer player) {
        extendedChecks = new ImmutableClassToInstanceMap.Builder<ExtendedCheck>()
                .put(AutoTotemA.class, new AutoTotemA(player))
                .build();

        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(ProtocolA.class, new ProtocolA(player))
                .put(ProtocolB.class, new ProtocolB(player))
                .put(ProtocolC.class, new ProtocolC(player))
                .build();

        allChecks = new ImmutableClassToInstanceMap.Builder<Check>()
                .putAll(packetChecks)
                .putAll(extendedChecks)
                .build();
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketCheck check : packetChecks.values()) {
            if (!check.isEnabled()) continue;
            check.onPacketReceive(packet);
        }

        for (PacketCheck check : extendedChecks.values()) {
            if (!check.isEnabled()) continue;
            check.onPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        for (PacketCheck check : packetChecks.values()) {
            if (!check.isEnabled()) continue;
            check.onPacketSend(packet);
        }

        for (PacketCheck check : extendedChecks.values()) {
            if (!check.isEnabled()) continue;
            check.onPacketSend(packet);
        }
    }

    public <T extends Event> void onEvent(final T event) {
        for (ExtendedCheck check : extendedChecks.values()) {
            if (!check.isEnabled()) continue;
            check.handleEvent(event);
        }
    }


    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPacketCheck(Class<T> check) {
        return (T) allChecks.get(check);
    }
}
