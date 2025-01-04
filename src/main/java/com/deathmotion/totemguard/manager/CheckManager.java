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

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.api.interfaces.AbstractCheck;
import com.deathmotion.totemguard.checks.impl.autototem.AutoTotemA;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsA;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsB;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsC;
import com.deathmotion.totemguard.checks.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.checks.impl.misc.ClientBrand;
import com.deathmotion.totemguard.checks.type.BukkitEventCheck;
import com.deathmotion.totemguard.checks.type.GenericCheck;
import com.deathmotion.totemguard.checks.type.PacketCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import org.bukkit.event.Event;

public class CheckManager {

    public ClassToInstanceMap<AbstractCheck> allChecks;
    ClassToInstanceMap<PacketCheck> packetChecks;
    ClassToInstanceMap<BukkitEventCheck> bukkitEventChecks;
    ClassToInstanceMap<GenericCheck> genericChecks;

    public CheckManager(TotemPlayer player) {
        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(ClientBrand.class, new ClientBrand(player))
                .put(BadPacketsA.class, new BadPacketsA(player))
                .put(BadPacketsB.class, new BadPacketsB(player))
                .put(BadPacketsC.class, new BadPacketsC(player))
                .build();

        bukkitEventChecks = new ImmutableClassToInstanceMap.Builder<BukkitEventCheck>()
                .put(AutoTotemA.class, new AutoTotemA(player))
                .build();

        genericChecks = new ImmutableClassToInstanceMap.Builder<GenericCheck>()
                .put(ManualTotemA.class, new ManualTotemA(player))
                .build();

        allChecks = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                .putAll(packetChecks)
                .putAll(bukkitEventChecks)
                .putAll(genericChecks)
                .build();
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketCheck check : packetChecks.values()) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        for (PacketCheck check : packetChecks.values()) {
            check.onPacketSend(packet);
        }
    }

    public void onBukkitEvent(Event event) {
        for (BukkitEventCheck check : bukkitEventChecks.values()) {
            check.onBukkitEvent(event);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPacketCheck(Class<T> check) {
        return (T) packetChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends BukkitEventCheck> T getBukkitEventCheck(Class<T> check) {
        return (T) bukkitEventChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends GenericCheck> T getGenericCheck(Class<T> check) {
        return (T) genericChecks.get(check);
    }
}
