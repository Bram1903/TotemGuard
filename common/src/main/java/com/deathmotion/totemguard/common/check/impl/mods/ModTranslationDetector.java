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

package com.deathmotion.totemguard.common.check.impl.mods;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientNameItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

final class ModTranslationDetector {

    static final char SEPARATOR = '|';

    private static final int ANVIL_WINDOW_TYPE = 8;
    private static final int BATCH_SIZE = 50;
    private static final long BATCH_DELAY_MILLIS = 50L;
    private static final int ID_MAX_LENGTH = 8;

    private final TGPlayer player;
    private final Object stateLock = new Object();

    private volatile List<TranslationLookup> lookups = List.of();
    private volatile Map<String, TranslationLookup> lookupsById = Map.of();
    private boolean started;
    private int nextIndex;
    private long runId;

    ModTranslationDetector(TGPlayer player) {
        this.player = player;
    }

    private static String newId() {
        String value = Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        return value.length() <= ID_MAX_LENGTH ? value : value.substring(value.length() - ID_MAX_LENGTH);
    }

    void rebuild(ModRegistry.Snapshot snapshot) {
        List<TranslationLookup> rebuilt = new ArrayList<>();
        Map<String, TranslationLookup> byId = new HashMap<>();
        Set<String> usedIds = new HashSet<>();

        for (ModDefinition definition : snapshot.definitions().values()) {
            for (String translation : definition.translations()) {
                String id;
                do {
                    id = newId();
                } while (!usedIds.add(id));

                TranslationLookup lookup = new TranslationLookup(definition, translation, id);
                rebuilt.add(lookup);
                byId.put(id, lookup);
            }
        }

        synchronized (stateLock) {
            runId++;
            started = false;
            nextIndex = 0;
            lookups = List.copyOf(rebuilt);
            lookupsById = Map.copyOf(byId);
        }
    }

    void start() {
        long currentRun;
        synchronized (stateLock) {
            if (started || lookups.isEmpty()) return;
            started = true;
            nextIndex = 0;
            runId++;
            currentRun = runId;
        }
        scheduleBatch(currentRun, 0L);
    }

    @Nullable
    ModDefinition tryConsumeResponse(WrapperPlayClientNameItem packet, Runnable onCancel) {
        String itemName = packet.getItemName();
        if (itemName == null || itemName.isBlank()) return null;

        int separatorIndex = itemName.indexOf(SEPARATOR);
        if (separatorIndex <= 0) return null;

        String id = itemName.substring(0, separatorIndex);
        TranslationLookup lookup = lookupsById.get(id);
        if (lookup == null) return null;

        onCancel.run();

        String response = itemName.substring(separatorIndex + 1);
        if (response.isBlank()) return null;

        if (lookup.translationKey.startsWith(response)) return null;

        return lookup.mod;
    }

    private void scheduleBatch(long currentRun, long delayMillis) {
        Runnable task = () -> sendBatch(currentRun);
        if (delayMillis <= 0L) {
            player.getPlatform().getScheduler().runAsyncTask(task);
        } else {
            player.getPlatform().getScheduler().runAsyncTaskDelayed(task, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void sendBatch(long currentRun) {
        List<TranslationLookup> batch = new ArrayList<>();
        boolean hasMore;

        synchronized (stateLock) {
            if (currentRun != runId || nextIndex >= lookups.size()) return;
            int endIndex = Math.min(lookups.size(), nextIndex + BATCH_SIZE);
            batch.addAll(lookups.subList(nextIndex, endIndex));
            nextIndex = endIndex;
            hasMore = nextIndex < lookups.size();
        }

        boolean wrapInBundle = !player.getData().isSendingBundlePacket();
        WrapperPlayServerBundle boundary = new WrapperPlayServerBundle();

        if (wrapInBundle) player.getUser().sendPacket(boundary);
        for (TranslationLookup lookup : batch) sendLookup(lookup);
        if (wrapInBundle) player.getUser().sendPacket(boundary);

        if (hasMore) scheduleBatch(currentRun, BATCH_DELAY_MILLIS);
    }

    private void sendLookup(TranslationLookup lookup) {
        int windowId = player.getModDetectionWindowId();
        int stateId = ThreadLocalRandom.current().nextInt();

        player.getUser().sendPacket(new WrapperPlayServerOpenWindow(
                windowId,
                ANVIL_WINDOW_TYPE,
                Component.text("Repair & Name"),
                0,
                true,
                0
        ));

        Component itemName = Component.text(lookup.id)
                .append(Component.text(SEPARATOR))
                .append(Component.translatable(lookup.translationKey));

        player.getUser().sendPacket(new WrapperPlayServerSetSlot(
                windowId,
                stateId,
                0,
                new ItemStack.Builder()
                        .type(ItemTypes.DIAMOND_SWORD)
                        .component(ComponentTypes.CUSTOM_NAME, itemName)
                        .amount(1)
                        .build()
        ));

        player.getUser().sendPacket(new WrapperPlayServerCloseWindow(windowId));
    }

    private record TranslationLookup(ModDefinition mod, String translationKey, String id) {
    }
}
