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
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.check.impl.autototem.AutoTotemA;
import com.deathmotion.totemguard.common.check.impl.autototem.AutoTotemB;
import com.deathmotion.totemguard.common.check.impl.balance.BalanceA;
import com.deathmotion.totemguard.common.check.impl.balance.BalanceB;
import com.deathmotion.totemguard.common.check.impl.inventory.*;
import com.deathmotion.totemguard.common.check.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.common.check.impl.mods.Mod;
import com.deathmotion.totemguard.common.check.impl.physics.Physics;
import com.deathmotion.totemguard.common.check.impl.protocol.*;
import com.deathmotion.totemguard.common.check.impl.tick.TickA;
import com.deathmotion.totemguard.common.check.impl.tick.TickB;
import com.deathmotion.totemguard.common.check.impl.tick.TickC;
import com.deathmotion.totemguard.common.check.impl.tick.TickD;
import com.deathmotion.totemguard.common.check.type.EventCheck;
import com.deathmotion.totemguard.common.check.type.ExtendedCheck;
import com.deathmotion.totemguard.common.check.type.ManualCheck;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class CheckManagerImpl {

    private static volatile int knownCheckCount;

    public final ClassToInstanceMap<Check> allChecks;

    private final TGPlayer player;

    private final ClassToInstanceMap<PacketCheck> packetChecks;
    private final ClassToInstanceMap<ManualCheck> manualChecks;
    private final Map<String, CheckImpl> checksByName;

    private final PacketCheck[] allPacketChecks;
    private final EventCheck[] allEventChecks;
    private final ExtendedCheck[] allExtendedChecks;

    private volatile CheckDispatch dispatch;

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
                .put(TickD.class, new TickD(player))
                .put(BalanceA.class, new BalanceA(player))
                .put(BalanceB.class, new BalanceB(player))
                .put(ProtocolA.class, new ProtocolA(player))
                .put(ProtocolB.class, new ProtocolB(player))
                .put(ProtocolC.class, new ProtocolC(player))
                .put(ProtocolD.class, new ProtocolD(player))
                .put(ProtocolE.class, new ProtocolE(player))
                .put(ProtocolF.class, new ProtocolF(player))
                .put(ProtocolG.class, new ProtocolG(player))
                .put(Physics.class, new Physics(player))
                .put(InventoryA.class, new InventoryA(player))
                .put(InventoryB.class, new InventoryB(player))
                .put(InventoryC.class, new InventoryC(player))
                .put(InventoryD.class, new InventoryD(player))
                .build();

        ImmutableClassToInstanceMap<ExtendedCheck> extendedChecks = ImmutableClassToInstanceMap.<ExtendedCheck>builder()
                .build();

        this.manualChecks = ImmutableClassToInstanceMap.<ManualCheck>builder()
                .put(ManualTotemA.class, new ManualTotemA(player))
                .put(Mod.class, new Mod(player))
                .build();

        this.allChecks = ImmutableClassToInstanceMap.<Check>builder()
                .putAll(packetChecks)
                .putAll(eventChecks)
                .putAll(manualChecks)
                .build();

        this.allPacketChecks = packetChecks.values().toArray(PacketCheck[]::new);
        this.allEventChecks = eventChecks.values().toArray(EventCheck[]::new);
        this.allExtendedChecks = extendedChecks.values().toArray(ExtendedCheck[]::new);

        this.checksByName = new HashMap<>(allChecks.size());
        for (Check check : allChecks.values()) {
            checksByName.put(check.getName(), (CheckImpl) check);
        }

        this.dispatch = buildDispatch();

        knownCheckCount = allChecks.size();
    }

    public static int knownCheckCount() {
        return knownCheckCount;
    }

    private static <T extends Check> CheckSlot<T> buildSlot(T[] all, IntFunction<T[]> ctor, Predicate<T> respondsToDirection) {
        Predicate<T> base = c -> c.isEnabled() && respondsToDirection.test(c);
        return new CheckSlot<>(
                filter(all, ctor, base.and(c -> !c.requiresTickEnd())),
                filter(all, ctor, base.and(Check::requiresTickEnd))
        );
    }

    private static <T> T[] filter(T[] source, IntFunction<T[]> ctor, Predicate<T> include) {
        int count = 0;
        for (T item : source) {
            if (include.test(item)) count++;
        }
        T[] out = ctor.apply(count);
        int i = 0;
        for (T item : source) {
            if (include.test(item)) out[i++] = item;
        }
        return out;
    }

    private static boolean overrides(Check check, String methodName, Class<?>... paramTypes) {
        try {
            Method method = check.getClass().getMethod(methodName, paramTypes);
            return !method.getDeclaringClass().isInterface();
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public void rebuild() {
        this.dispatch = buildDispatch();
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        CheckDispatch d = this.dispatch;
        CheckSlot<PacketCheck> pkt = d.packetReceive();
        CheckSlot<ExtendedCheck> ext = d.extendedReceive();
        for (PacketCheck c : pkt.always()) c.onPacketReceive(packet);
        for (ExtendedCheck c : ext.always()) c.onPacketReceive(packet);
        if (player.supportsEndTick()) {
            for (PacketCheck c : pkt.tickEnd()) c.onPacketReceive(packet);
            for (ExtendedCheck c : ext.tickEnd()) c.onPacketReceive(packet);
        }
    }

    public void onPreFlying(final PacketReceiveEvent packet) {
        for (PacketCheck c : this.dispatch.preFlying().always()) c.onPreFlying(packet);
    }

    public void onPacketSend(final PacketSendEvent packet) {
        CheckDispatch d = this.dispatch;
        CheckSlot<PacketCheck> pkt = d.packetSend();
        CheckSlot<ExtendedCheck> ext = d.extendedSend();
        for (PacketCheck c : pkt.always()) c.onPacketSend(packet);
        for (ExtendedCheck c : ext.always()) c.onPacketSend(packet);
        if (player.supportsEndTick()) {
            for (PacketCheck c : pkt.tickEnd()) c.onPacketSend(packet);
            for (ExtendedCheck c : ext.tickEnd()) c.onPacketSend(packet);
        }
    }

    public void onTotemActivated(long timestamp) {
        CheckDispatch d = this.dispatch;
        CheckSlot<EventCheck> ev = d.event();
        CheckSlot<ExtendedCheck> ext = d.extendedEvent();
        for (EventCheck c : ev.always()) c.onTotemActivated(timestamp);
        for (ExtendedCheck c : ext.always()) c.onTotemActivated(timestamp);
        if (player.supportsEndTick()) {
            for (EventCheck c : ev.tickEnd()) c.onTotemActivated(timestamp);
            for (ExtendedCheck c : ext.tickEnd()) c.onTotemActivated(timestamp);
        }
    }

    public void onTotemReplenished(long totemActivatedTimestamp,
                                   long totemReplenishedTimestamp,
                                   @Nullable Long totemPickupTimestamp) {
        CheckDispatch d = this.dispatch;
        CheckSlot<EventCheck> ev = d.event();
        CheckSlot<ExtendedCheck> ext = d.extendedEvent();
        for (EventCheck c : ev.always())
            c.onTotemReplenished(totemActivatedTimestamp, totemReplenishedTimestamp, totemPickupTimestamp);
        for (ExtendedCheck c : ext.always())
            c.onTotemReplenished(totemActivatedTimestamp, totemReplenishedTimestamp, totemPickupTimestamp);
        if (player.supportsEndTick()) {
            for (EventCheck c : ev.tickEnd())
                c.onTotemReplenished(totemActivatedTimestamp, totemReplenishedTimestamp, totemPickupTimestamp);
            for (ExtendedCheck c : ext.tickEnd())
                c.onTotemReplenished(totemActivatedTimestamp, totemReplenishedTimestamp, totemPickupTimestamp);
        }
    }

    public void onInventoryChanged(@Nullable CarriedItem updatedCarriedItem,
                                   @NotNull List<InventorySlot> changedSlots,
                                   @NotNull Issuer lastIssuer) {
        CheckDispatch d = this.dispatch;
        CheckSlot<EventCheck> ev = d.event();
        CheckSlot<ExtendedCheck> ext = d.extendedEvent();
        for (EventCheck c : ev.always()) c.onInventoryChanged(updatedCarriedItem, changedSlots, lastIssuer);
        for (ExtendedCheck c : ext.always()) c.onInventoryChanged(updatedCarriedItem, changedSlots, lastIssuer);
        if (player.supportsEndTick()) {
            for (EventCheck c : ev.tickEnd()) c.onInventoryChanged(updatedCarriedItem, changedSlots, lastIssuer);
            for (ExtendedCheck c : ext.tickEnd()) c.onInventoryChanged(updatedCarriedItem, changedSlots, lastIssuer);
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

    private CheckDispatch buildDispatch() {
        return new CheckDispatch(
                buildSlot(allPacketChecks, PacketCheck[]::new, c -> overrides(c, "onPacketReceive", PacketReceiveEvent.class)),
                buildSlot(allPacketChecks, PacketCheck[]::new, c -> overrides(c, "onPacketSend", PacketSendEvent.class)),
                buildSlot(allPacketChecks, PacketCheck[]::new, c -> overrides(c, "onPreFlying", PacketReceiveEvent.class)),
                buildSlot(allEventChecks, EventCheck[]::new, c -> true),
                buildSlot(allExtendedChecks, ExtendedCheck[]::new, c -> overrides(c, "onPacketReceive", PacketReceiveEvent.class)),
                buildSlot(allExtendedChecks, ExtendedCheck[]::new, c -> overrides(c, "onPacketSend", PacketSendEvent.class)),
                buildSlot(allExtendedChecks, ExtendedCheck[]::new, c -> true)
        );
    }
}
