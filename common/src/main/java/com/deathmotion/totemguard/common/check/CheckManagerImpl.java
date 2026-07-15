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
import com.deathmotion.totemguard.common.check.impl.balance.BalanceC;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryA;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryB;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryC;
import com.deathmotion.totemguard.common.check.impl.inventory.InventoryD;
import com.deathmotion.totemguard.common.check.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.common.check.impl.mods.Mod;
import com.deathmotion.totemguard.common.check.impl.physics.Physics;
import com.deathmotion.totemguard.common.check.impl.protocol.*;
import com.deathmotion.totemguard.common.check.impl.tick.*;
import com.deathmotion.totemguard.common.check.impl.world.FastBreak;
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
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class CheckManagerImpl {

    private static final List<CheckEntry<? extends CheckImpl>> ENTRIES = List.of(
            entry(AutoTotemA.class, AutoTotemA::new),
            entry(AutoTotemB.class, AutoTotemB::new),
            entry(TickA.class, TickA::new),
            entry(TickB.class, TickB::new),
            entry(TickC.class, TickC::new),
            entry(TickD.class, TickD::new),
            entry(TickE.class, TickE::new),
            entry(TickF.class, TickF::new),
            entry(BalanceA.class, BalanceA::new),
            entry(BalanceB.class, BalanceB::new),
            entry(BalanceC.class, BalanceC::new),
            entry(ProtocolA.class, ProtocolA::new),
            entry(ProtocolB.class, ProtocolB::new),
            entry(ProtocolC.class, ProtocolC::new),
            entry(ProtocolD.class, ProtocolD::new),
            entry(ProtocolE.class, ProtocolE::new),
            entry(ProtocolF.class, ProtocolF::new),
            entry(ProtocolG.class, ProtocolG::new),
            entry(Physics.class, Physics::new),
            entry(FastBreak.class, FastBreak::new),
            entry(InventoryA.class, InventoryA::new),
            entry(InventoryB.class, InventoryB::new),
            entry(InventoryC.class, InventoryC::new),
            entry(InventoryD.class, InventoryD::new),
            entry(ManualTotemA.class, ManualTotemA::new),
            entry(Mod.class, Mod::new)
    );

    private static volatile int knownCheckCount;

    public final ClassToInstanceMap<Check> allChecks;

    private final TGPlayer player;

    private final ClassToInstanceMap<PacketCheck> packetChecks;
    private final ClassToInstanceMap<ManualCheck> manualChecks;
    private final Map<String, CheckImpl> checksByName;
    private final Physics physicsCheck;

    private final PacketCheck[] allPacketChecks;
    private final EventCheck[] allEventChecks;
    private final ExtendedCheck[] allExtendedChecks;

    private volatile CheckDispatch dispatch;

    public CheckManagerImpl(TGPlayer player) {
        this.player = player;

        ImmutableClassToInstanceMap.Builder<PacketCheck> packetBuilder = ImmutableClassToInstanceMap.builder();
        ImmutableClassToInstanceMap.Builder<EventCheck> eventBuilder = ImmutableClassToInstanceMap.builder();
        ImmutableClassToInstanceMap.Builder<ExtendedCheck> extendedBuilder = ImmutableClassToInstanceMap.builder();
        ImmutableClassToInstanceMap.Builder<ManualCheck> manualBuilder = ImmutableClassToInstanceMap.builder();

        for (CheckEntry<? extends CheckImpl> checkEntry : ENTRIES) {
            Class<? extends CheckImpl> checkClass = checkEntry.type();
            CheckImpl check = checkEntry.factory().apply(player);

            if (check instanceof ExtendedCheck) {
                putInstance(extendedBuilder, checkClass, check);
            } else if (check instanceof PacketCheck) {
                putInstance(packetBuilder, checkClass, check);
            } else if (check instanceof EventCheck) {
                putInstance(eventBuilder, checkClass, check);
            } else if (check instanceof ManualCheck) {
                putInstance(manualBuilder, checkClass, check);
            } else {
                throw new IllegalStateException("Check class " + checkClass.getName()
                        + " must implement PacketCheck, EventCheck, ExtendedCheck or ManualCheck");
            }
        }

        ImmutableClassToInstanceMap<EventCheck> eventChecks = eventBuilder.build();
        ImmutableClassToInstanceMap<ExtendedCheck> extendedChecks = extendedBuilder.build();
        this.packetChecks = packetBuilder.build();
        this.manualChecks = manualBuilder.build();

        this.allChecks = ImmutableClassToInstanceMap.<Check>builder()
                .putAll(packetChecks)
                .putAll(eventChecks)
                .putAll(manualChecks)
                .build();

        this.allPacketChecks = packetChecks.values().toArray(PacketCheck[]::new);
        this.allEventChecks = eventChecks.values().toArray(EventCheck[]::new);
        this.allExtendedChecks = extendedChecks.values().toArray(ExtendedCheck[]::new);

        this.physicsCheck = packetChecks.getInstance(Physics.class);

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

    public static List<String> bypassPermissionNodes() {
        List<String> nodes = new ArrayList<>(ENTRIES.size());
        for (CheckEntry<? extends CheckImpl> checkEntry : ENTRIES) {
            nodes.add(CheckImpl.resolveBypassPermission(checkEntry.type()));
        }
        return List.copyOf(nodes);
    }

    private static <T extends CheckImpl> CheckEntry<T> entry(Class<T> type, Function<TGPlayer, T> factory) {
        return new CheckEntry<>(type, factory);
    }

    @SuppressWarnings("unchecked")
    private static <B extends Check> void putInstance(ImmutableClassToInstanceMap.Builder<B> builder,
                                                      Class<? extends CheckImpl> type,
                                                      CheckImpl check) {
        builder.put((Class<B>) type, (B) check);
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

    public void updateBypassPermissions() {
        for (CheckImpl check : checksByName.values()) {
            check.updateBypassPermission();
        }
    }

    public boolean physicsBypassed() {
        return physicsCheck.isBypassed();
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

    private record CheckEntry<T extends CheckImpl>(Class<T> type, Function<TGPlayer, T> factory) {
    }
}
