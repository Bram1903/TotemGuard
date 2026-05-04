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

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.event.Event;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.check.impl.autototem.AutoTotemA;
import com.deathmotion.totemguard.common.check.impl.autototem.AutoTotemB;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryA;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryB;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryC;
import com.deathmotion.totemguard.common.check.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.common.check.impl.mods.Mod;
import com.deathmotion.totemguard.common.check.impl.protocol.*;
import com.deathmotion.totemguard.common.check.impl.tick.TickA;
import com.deathmotion.totemguard.common.check.impl.tick.TickB;
import com.deathmotion.totemguard.common.check.impl.tick.TickC;
import com.deathmotion.totemguard.common.check.type.EventCheck;
import com.deathmotion.totemguard.common.check.type.ExtendedCheck;
import com.deathmotion.totemguard.common.check.type.ManualCheck;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckManagerImpl {

    private static volatile int knownCheckCount;

    public final ClassToInstanceMap<Check> allChecks;
    private final TGPlayer player;

    private final PacketCheck[] packetCheckArray;
    private final EventCheck[] eventCheckArray;
    private final ExtendedCheck[] extendedCheckArray;

    private final ClassToInstanceMap<PacketCheck> packetChecks;
    private final ClassToInstanceMap<ManualCheck> manualChecks;

    private final Map<String, CheckImpl> checksByName;

    public CheckManagerImpl(TGPlayer player) {
        this.player = player;

        ImmutableClassToInstanceMap<EventCheck> eventChecks = ImmutableClassToInstanceMap.<EventCheck>builder()
                .put(AutoTotemA.class, new AutoTotemA(player))
                .put(AutoTotemB.class, new AutoTotemB(player))
                .build();

        this.packetChecks = ImmutableClassToInstanceMap.<PacketCheck>builder()
                .put(TickA.class, new TickA(player))
                .put(TickB.class, new TickB(player))
                .put(TickC.class, new TickC(player))
                .put(ProtocolA.class, new ProtocolA(player))
                .put(ProtocolB.class, new ProtocolB(player))
                .put(ProtocolC.class, new ProtocolC(player))
                .put(ProtocolD.class, new ProtocolD(player))
                .put(ProtocolE.class, new ProtocolE(player))
                .put(ProtocolF.class, new ProtocolF(player))
                .put(ProtocolG.class, new ProtocolG(player))
                .put(InventoryA.class, new InventoryA(player))
                .put(InventoryB.class, new InventoryB(player))
                .put(InventoryC.class, new InventoryC(player))
                .put(Mod.class, new Mod(player))
                .build();

        ImmutableClassToInstanceMap<ExtendedCheck> extendedChecks = ImmutableClassToInstanceMap.<ExtendedCheck>builder()
                .build();

        this.manualChecks = ImmutableClassToInstanceMap.<ManualCheck>builder()
                .put(ManualTotemA.class, new ManualTotemA(player))
                .build();

        this.allChecks = ImmutableClassToInstanceMap.<Check>builder()
                .putAll(packetChecks)
                .putAll(eventChecks)
                .putAll(manualChecks)
                .build();

        this.packetCheckArray = packetChecks.values().toArray(PacketCheck[]::new);
        this.eventCheckArray = eventChecks.values().toArray(EventCheck[]::new);
        this.extendedCheckArray = extendedChecks.values().toArray(ExtendedCheck[]::new);

        this.checksByName = new HashMap<>(allChecks.size());
        for (Check check : allChecks.values()) {
            checksByName.put(check.getName(), (CheckImpl) check);
        }

        knownCheckCount = allChecks.size();
    }

    public static int knownCheckCount() {
        return knownCheckCount;
    }

    private boolean skip(Check check) {
        if (!check.isEnabled()) return true;
        return check.requiresTickEnd() && !player.supportsEndTick();
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketCheck check : packetCheckArray) {
            if (skip(check)) continue;
            check.onPacketReceive(packet);
        }
        for (ExtendedCheck check : extendedCheckArray) {
            if (skip(check)) continue;
            check.onPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        for (PacketCheck check : packetCheckArray) {
            if (skip(check)) continue;
            check.onPacketSend(packet);
        }
        for (ExtendedCheck check : extendedCheckArray) {
            if (skip(check)) continue;
            check.onPacketSend(packet);
        }
    }

    public <T extends Event> void onEvent(final T event) {
        for (EventCheck check : eventCheckArray) {
            if (skip(check)) continue;
            check.handleEvent(event);
        }
        for (ExtendedCheck check : extendedCheckArray) {
            if (skip(check)) continue;
            check.handleEvent(event);
        }
    }

    public List<CheckSnapshot> getSnapshot() {
        List<CheckSnapshot> snapshots = new ArrayList<>(checksByName.size());
        for (CheckImpl check : checksByName.values()) {
            snapshots.add(check.getSnapshot());
        }
        return snapshots;
    }

    public void applySnapshot(List<CheckSnapshot> snapshots) {
        for (CheckSnapshot snapshot : snapshots) {
            CheckImpl check = checksByName.get(snapshot.checkName());
            if (check != null) {
                check.applySnapshot(snapshot);
            }
        }
    }

    public void clearAllViolations() {
        for (CheckImpl check : checksByName.values()) {
            check.clearViolations();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPacketCheck(Class<T> check) {
        return (T) packetChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends ManualCheck> T getManualCheck(Class<T> check) {
        return (T) manualChecks.get(check);
    }
}
