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

package com.deathmotion.totemguard.common.event;

import com.deathmotion.totemguard.api.event.EventBus;
import com.deathmotion.totemguard.api.event.EventChannel;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import com.deathmotion.totemguard.common.event.channel.VirtualEventChannel;
import com.deathmotion.totemguard.common.event.channel.impl.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EventBusImpl implements EventBus {

    @Getter
    private final TGUserJoinChannel userJoin = new TGUserJoinChannel();
    @Getter
    private final TGUserQuitChannel userQuit = new TGUserQuitChannel();
    @Getter
    private final TGUserFlagChannel userFlag = new TGUserFlagChannel();
    @Getter
    private final TGUserPunishChannel userPunish = new TGUserPunishChannel();
    @Getter
    private final TGUserInventoryOpenChannel userInventoryOpen = new TGUserInventoryOpenChannel();
    @Getter
    private final TGUserInventoryCloseChannel userInventoryClose = new TGUserInventoryCloseChannel();
    @Getter
    private final TGModDetectionResolvedChannel modDetectionResolved = new TGModDetectionResolvedChannel();
    @Getter
    private final TGTeleportChannel teleport = new TGTeleportChannel();
    @Getter
    private final TGSetbackChannel setback = new TGSetbackChannel();
    @Getter
    private final TGMonitorOpenChannel monitorOpen = new TGMonitorOpenChannel();
    @Getter
    private final TGFollowChannel follow = new TGFollowChannel();
    @Getter
    private final TGFocusChannel focus = new TGFocusChannel();
    @Getter
    private final TGPluginShutdownChannel pluginShutdown = new TGPluginShutdownChannel();

    private final ConcurrentMap<Class<?>, EventChannelImpl<?>> concrete = new ConcurrentHashMap<>();

    private final ConcurrentMap<Class<?>, VirtualEventChannel<?>> virtualCache = new ConcurrentHashMap<>();

    public EventBusImpl() {
        concrete.put(userJoin.eventType(), userJoin);
        concrete.put(userQuit.eventType(), userQuit);
        concrete.put(userFlag.eventType(), userFlag);
        concrete.put(userPunish.eventType(), userPunish);
        concrete.put(userInventoryOpen.eventType(), userInventoryOpen);
        concrete.put(userInventoryClose.eventType(), userInventoryClose);
        concrete.put(modDetectionResolved.eventType(), modDetectionResolved);
        concrete.put(teleport.eventType(), teleport);
        concrete.put(setback.eventType(), setback);
        concrete.put(monitorOpen.eventType(), monitorOpen);
        concrete.put(follow.eventType(), follow);
        concrete.put(focus.eventType(), focus);
        concrete.put(pluginShutdown.eventType(), pluginShutdown);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> @NotNull EventChannel<E> get(@NotNull Class<E> eventClass) {
        EventChannelImpl<?> direct = concrete.get(eventClass);
        if (direct != null) return (EventChannel<E>) direct;

        VirtualEventChannel<?> cached = virtualCache.get(eventClass);
        if (cached != null) return (EventChannel<E>) cached;

        VirtualEventChannel<E> virtual = buildVirtual(eventClass);
        VirtualEventChannel<?> prior = virtualCache.putIfAbsent(eventClass, virtual);
        return (EventChannel<E>) (prior != null ? prior : virtual);
    }

    @Override
    public void unregisterAll(@NotNull Object pluginContext) {
        for (EventChannelImpl<?> channel : concrete.values()) {
            channel.unsubscribeAllFromPlugin(pluginContext);
        }
    }

    private <E> VirtualEventChannel<E> buildVirtual(Class<E> eventClass) {
        List<EventChannelImpl<? extends E>> matched = new ArrayList<>();
        for (EventChannelImpl<?> channel : concrete.values()) {
            if (eventClass.isAssignableFrom(channel.eventType())) {
                @SuppressWarnings("unchecked")
                EventChannelImpl<? extends E> typed = (EventChannelImpl<? extends E>) channel;
                matched.add(typed);
            }
        }
        if (matched.isEmpty()) {
            throw new IllegalArgumentException(
                    "No TotemGuard event matches " + eventClass.getName()
                            + ". Make sure you imported a TotemGuard event interface from com.deathmotion.totemguard.api.event.events.");
        }
        return new VirtualEventChannel<>(eventClass, matched);
    }
}
