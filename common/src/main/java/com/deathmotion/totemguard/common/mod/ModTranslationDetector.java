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

package com.deathmotion.totemguard.common.mod;

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

public final class ModTranslationDetector {

    static final char SEPARATOR = '|';
    private static final int ANVIL_WINDOW_TYPE = 8;
    private static final int BATCH_SIZE = 100;
    private static final long BATCH_DELAY_MILLIS = 25L;
    private static final int ID_MAX_LENGTH = 8;
    private final TGPlayer player;
    private final Object stateLock = new Object();
    private final Set<String> pendingProbeIds = new HashSet<>();
    private final List<TranslationLookup> lookups;
    private final Map<String, TranslationLookup> lookupsById;
    private boolean started;
    private int sentCount;
    private int answeredCount;
    private boolean active;
    private @Nullable Runnable onTransactionEcho;

    public ModTranslationDetector(TGPlayer player, ModRegistry.Snapshot snapshot) {
        this.player = player;

        List<TranslationLookup> built = new ArrayList<>();
        Map<String, TranslationLookup> byId = new HashMap<>();
        Set<String> usedIds = new HashSet<>();

        for (ModDefinition definition : snapshot.definitions().values()) {
            for (String translation : definition.translations()) {
                String id;
                do {
                    id = newId();
                } while (!usedIds.add(id));

                TranslationLookup lookup = new TranslationLookup(definition, translation, id);
                built.add(lookup);
                byId.put(id, lookup);
            }
        }

        this.lookups = List.copyOf(built);
        this.lookupsById = Map.copyOf(byId);
    }

    private static String newId() {
        String value = Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        return value.length() <= ID_MAX_LENGTH ? value : value.substring(value.length() - ID_MAX_LENGTH);
    }

    public boolean isActive() {
        synchronized (stateLock) {
            return active;
        }
    }

    public boolean hasLookups() {
        return !lookups.isEmpty();
    }

    public int sentCount() {
        synchronized (stateLock) {
            return sentCount;
        }
    }

    public int answeredCount() {
        synchronized (stateLock) {
            return answeredCount;
        }
    }

    public boolean allAnswered() {
        synchronized (stateLock) {
            return sentCount == answeredCount;
        }
    }

    public void start(Runnable onTransactionEcho) {
        synchronized (stateLock) {
            if (started) return;
            started = true;
            this.onTransactionEcho = onTransactionEcho;
            if (lookups.isEmpty()) {
                queueTransaction();
                return;
            }
            active = true;
        }
        scheduleBatch(0, 0L);
    }

    public ConsumeResult tryConsumeResponse(WrapperPlayClientNameItem packet, Runnable onCancel) {
        String itemName = packet.getItemName();
        if (itemName == null || itemName.isBlank()) return ConsumeResult.notOurs();

        int separatorIndex = itemName.indexOf(SEPARATOR);
        if (separatorIndex <= 0) return ConsumeResult.notOurs();

        String id = itemName.substring(0, separatorIndex);
        TranslationLookup lookup = lookupsById.get(id);
        if (lookup == null) return ConsumeResult.notOurs();

        onCancel.run();

        synchronized (stateLock) {
            if (pendingProbeIds.remove(id)) {
                answeredCount++;
            }
        }

        String response = itemName.substring(separatorIndex + 1);
        if (response.isBlank() || lookup.translationKey.startsWith(response)) {
            return new ConsumeResult(ResponseOutcome.OURS_NO_DETECTION, null);
        }
        return new ConsumeResult(ResponseOutcome.OURS_DETECTED, lookup.mod);
    }

    private void scheduleBatch(int startIndex, long delayMillis) {
        Runnable task = () -> sendBatch(startIndex);
        if (delayMillis <= 0L) {
            player.getPlatform().getScheduler().runAsyncTask(task);
        } else {
            player.getPlatform().getScheduler().runAsyncTaskDelayed(task, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void sendBatch(int startIndex) {
        int endIndex = Math.min(lookups.size(), startIndex + BATCH_SIZE);
        if (startIndex >= endIndex) return;

        List<TranslationLookup> batch = lookups.subList(startIndex, endIndex);

        synchronized (stateLock) {
            for (TranslationLookup lookup : batch) {
                pendingProbeIds.add(lookup.id);
                sentCount++;
            }
        }

        boolean wrapInBundle = !player.getData().isSendingBundlePacket();
        WrapperPlayServerBundle boundary = new WrapperPlayServerBundle();

        if (wrapInBundle) player.getUser().sendPacket(boundary);
        for (TranslationLookup lookup : batch) sendLookup(lookup);
        if (wrapInBundle) player.getUser().sendPacket(boundary);

        boolean done = endIndex >= lookups.size();
        if (done) {
            synchronized (stateLock) {
                active = false;
            }
            queueTransaction();
        } else {
            scheduleBatch(endIndex, BATCH_DELAY_MILLIS);
        }
    }

    private void queueTransaction() {
        Runnable callback;
        synchronized (stateLock) {
            callback = onTransactionEcho;
            onTransactionEcho = null;
        }
        if (callback == null) return;
        player.getLatencyHandler().sendTransaction(timestamp -> callback.run());
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

    enum ResponseOutcome {
        NOT_OURS,
        OURS_NO_DETECTION,
        OURS_DETECTED
    }

    public record ConsumeResult(ResponseOutcome outcome, @Nullable ModDefinition mod) {

        static ConsumeResult notOurs() {
            return new ConsumeResult(ResponseOutcome.NOT_OURS, null);
        }
    }

    private record TranslationLookup(ModDefinition mod, String translationKey, String id) {
    }
}
