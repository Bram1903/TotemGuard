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

package com.deathmotion.totemguard.common.check;

import com.deathmotion.totemguard.api3.check.Check;
import com.deathmotion.totemguard.api3.event.Event;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.impl.autoclicker.AutoClickerA;
import com.deathmotion.totemguard.common.check.impl.autototem.AutoTotemA;
import com.deathmotion.totemguard.common.check.impl.autototem.AutoTotemB;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryA;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryB;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryC;
import com.deathmotion.totemguard.common.check.impl.mods.Mod;
import com.deathmotion.totemguard.common.check.impl.protocol.*;
import com.deathmotion.totemguard.common.check.type.EventCheck;
import com.deathmotion.totemguard.common.check.type.ExtendedCheck;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

public class CheckManagerImpl {

    private final TGPlayer player;

    public ClassToInstanceMap<Check> allChecks;

    ClassToInstanceMap<PacketCheck> packetChecks;
    ClassToInstanceMap<EventCheck> eventChecks;
    ClassToInstanceMap<ExtendedCheck> extendedChecks;

    public CheckManagerImpl(TGPlayer player) {
        this.player = player;

        var eventBuilder = new ImmutableClassToInstanceMap.Builder<EventCheck>();
        putIfSupported(eventBuilder, AutoTotemA.class, new AutoTotemA(player));
        putIfSupported(eventBuilder, AutoTotemB.class, new AutoTotemB(player));
        eventChecks = eventBuilder.build();

        var packetBuilder = new ImmutableClassToInstanceMap.Builder<PacketCheck>();
        putIfSupported(packetBuilder, ProtocolA.class, new ProtocolA(player));
        putIfSupported(packetBuilder, ProtocolB.class, new ProtocolB(player));
        putIfSupported(packetBuilder, ProtocolC.class, new ProtocolC(player));
        putIfSupported(packetBuilder, ProtocolD.class, new ProtocolD(player));
        putIfSupported(packetBuilder, ProtocolE.class, new ProtocolE(player));
        putIfSupported(packetBuilder, ProtocolF.class, new ProtocolF(player));
        putIfSupported(packetBuilder, AutoClickerA.class, new AutoClickerA(player));
        putIfSupported(packetBuilder, InventoryA.class, new InventoryA(player));
        putIfSupported(packetBuilder, InventoryB.class, new InventoryB(player));
        putIfSupported(packetBuilder, InventoryC.class, new InventoryC(player));
        putIfSupported(packetBuilder, Mod.class, new Mod(player));
        packetChecks = packetBuilder.build();

        var extendedBuilder = new ImmutableClassToInstanceMap.Builder<ExtendedCheck>();
        extendedChecks = extendedBuilder.build();

        allChecks = new ImmutableClassToInstanceMap.Builder<Check>()
                .putAll(packetChecks)
                .putAll(eventChecks)
                .putAll(extendedChecks)
                .build();
    }

    private boolean shouldRegister(Class<?> checkClass) {
        if (checkClass.isAnnotationPresent(RequiresTickEnd.class)) {
            return player.isSupportsTickEndPacket();
        }

        return true;
    }

    private <B, T extends B> void putIfSupported(
            ImmutableClassToInstanceMap.Builder<B> builder,
            Class<T> type,
            T instance
    ) {
        if (!shouldRegister(type)) return;
        builder.put(type, instance);
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketCheck check : packetChecks.values()) {
            if (!check.isEnabled()) continue;
            check.onPacketReceive(packet);
        }

        // Use ExtendedCheck type here for clarity (and to avoid accidental type mismatch)
        for (ExtendedCheck check : extendedChecks.values()) {
            if (!check.isEnabled()) continue;
            check.onPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        for (PacketCheck check : packetChecks.values()) {
            if (!check.isEnabled()) continue;
            check.onPacketSend(packet);
        }

        for (ExtendedCheck check : extendedChecks.values()) {
            if (!check.isEnabled()) continue;
            check.onPacketSend(packet);
        }
    }

    public <T extends Event> void onEvent(final T event) {
        for (EventCheck check : eventChecks.values()) {
            if (!check.isEnabled()) continue;
            check.handleEvent(event);
        }

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

