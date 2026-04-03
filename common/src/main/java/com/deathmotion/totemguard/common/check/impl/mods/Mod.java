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

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientNameItem;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import net.kyori.adventure.text.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public final class Mod extends CheckImpl implements PacketCheck {

    private static final String REGISTER_CHANNEL = "minecraft:register";
    private static final int ANVIL_WINDOW_TYPE = 8;
    private static final char PROBE_SEPARATOR = '|';
    private static final int TRANSLATION_BATCH_SIZE = 50;
    private static final long TRANSLATION_BATCH_DELAY_MILLIS = 150L;

    private final Set<String> detectedMods = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingDetections = ConcurrentHashMap.newKeySet();
    private final Object translationProbeLock = new Object();

    private volatile Map<String, ModDefinition> definitionsById = Map.of();
    private volatile List<TranslationProbe> translationProbes = List.of();
    private volatile Map<String, TranslationProbe> translationProbesById = Map.of();

    private boolean translationProbeStarted;
    private int nextTranslationProbeIndex;
    private long translationProbeRunId;

    public Mod(TGPlayer player) {
        super(player);
        punishable = true;
        reload();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        final String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    @Override
    public void reload() {
        super.reload();

        definitionsById = ModRegistry.getDefinitions();
        detectedMods.removeIf(modId -> !definitionsById.containsKey(modId));
        pendingDetections.removeIf(modId -> !definitionsById.containsKey(modId));
        rebuildTranslationProbes();
    }

    public void handle() {
        flushPendingDetections();
        startTranslationProbes();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.NAME_ITEM) {
            handleTranslationResponse(event);
            flushPendingDetections();
            return;
        }

        if (packetType == PacketType.Play.Client.PLUGIN_MESSAGE) {
            final WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
            return;
        }

        if (packetType == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            final WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        }
    }

    private void handlePluginMessage(String channelName, byte[] data) {
        final String normalizedChannelName = normalize(channelName);
        if (normalizedChannelName == null) {
            return;
        }

        if (REGISTER_CHANNEL.equals(normalizedChannelName)) {
            final String payload = new String(data, StandardCharsets.UTF_8);
            for (String registeredChannel : payload.split("\0")) {
                queueDetections(registeredChannel);
            }
        } else {
            queueDetections(normalizedChannelName);
        }

        flushPendingDetections();
    }

    private void queueDetections(String value) {
        final String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return;
        }

        for (ModDefinition definition : definitionsById.values()) {
            if (detectedMods.contains(definition.id()) || !definition.hasPayloads()) {
                continue;
            }

            if (definition.matchesPayload(normalizedValue)) {
                pendingDetections.add(definition.id());
            }
        }
    }

    private void flushPendingDetections() {
        if (!player.isHasLoggedIn() || pendingDetections.isEmpty()) {
            return;
        }

        for (String modId : new ArrayList<>(pendingDetections)) {
            if (!pendingDetections.remove(modId)) {
                continue;
            }

            if (detectedMods.add(modId)) {
                fail(modId);
            }
        }
    }

    private void rebuildTranslationProbes() {
        final List<TranslationProbe> rebuiltProbes = new ArrayList<>();
        final Map<String, TranslationProbe> rebuiltProbesById = new HashMap<>();
        final Set<String> usedProbeIds = new HashSet<>();

        for (ModDefinition definition : definitionsById.values()) {
            if (!definition.hasTranslations()) {
                continue;
            }

            for (String translationKey : definition.translations()) {
                String probeId;
                do {
                    probeId = createProbeId();
                } while (!usedProbeIds.add(probeId));

                final TranslationProbe probe = new TranslationProbe(definition.id(), translationKey, probeId);
                rebuiltProbes.add(probe);
                rebuiltProbesById.put(probeId, probe);
            }
        }

        synchronized (translationProbeLock) {
            translationProbeRunId++;
            translationProbeStarted = false;
            nextTranslationProbeIndex = 0;
            translationProbes = List.copyOf(rebuiltProbes);
            translationProbesById = Map.copyOf(rebuiltProbesById);
        }
    }

    private void startTranslationProbes() {
        final long runId;

        synchronized (translationProbeLock) {
            if (translationProbeStarted || translationProbes.isEmpty()) {
                return;
            }

            translationProbeStarted = true;
            nextTranslationProbeIndex = 0;
            translationProbeRunId++;
            runId = translationProbeRunId;
        }

        scheduleTranslationBatch(runId, 0L);
    }

    private void scheduleTranslationBatch(long runId, long delayMillis) {
        final Runnable task = () -> sendTranslationBatch(runId);

        if (delayMillis <= 0L) {
            platform.getScheduler().runAsyncTask(task);
            return;
        }

        platform.getScheduler().runAsyncTaskDelayed(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void sendTranslationBatch(long runId) {
        final List<TranslationProbe> batch = new ArrayList<>();
        final boolean hasMore;

        synchronized (translationProbeLock) {
            if (runId != translationProbeRunId || nextTranslationProbeIndex >= translationProbes.size()) {
                return;
            }

            final int endIndex = Math.min(translationProbes.size(), nextTranslationProbeIndex + TRANSLATION_BATCH_SIZE);
            batch.addAll(translationProbes.subList(nextTranslationProbeIndex, endIndex));
            nextTranslationProbeIndex = endIndex;
            hasMore = nextTranslationProbeIndex < translationProbes.size();
        }

        final boolean wrapInBundle = !player.getData().isSendingBundlePacket();
        final WrapperPlayServerBundle bundleBoundary = new WrapperPlayServerBundle();

        if (wrapInBundle) {
            player.getUser().sendPacket(bundleBoundary);
        }

        for (TranslationProbe probe : batch) {
            sendTranslationProbe(probe);
        }

        if (wrapInBundle) {
            player.getUser().sendPacket(bundleBoundary);
        }

        if (hasMore) {
            scheduleTranslationBatch(runId, TRANSLATION_BATCH_DELAY_MILLIS);
        }
    }

    private void sendTranslationProbe(TranslationProbe probe) {
        final int windowId = -ThreadLocalRandom.current().nextInt(10_000, Integer.MAX_VALUE);
        final int stateId = ThreadLocalRandom.current().nextInt();

        player.getUser().sendPacket(new WrapperPlayServerOpenWindow(
                windowId,
                ANVIL_WINDOW_TYPE,
                Component.text("Repair & Name"),
                0,
                true,
                0
        ));

        final Component itemName = Component.text(probe.probeId())
                .append(Component.text(PROBE_SEPARATOR))
                .append(Component.translatable(probe.translationKey()));

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

    private void handleTranslationResponse(PacketReceiveEvent event) {
        final WrapperPlayClientNameItem packet = new WrapperPlayClientNameItem(event);
        final String itemName = packet.getItemName();
        if (itemName == null || itemName.isBlank()) {
            return;
        }

        final int separatorIndex = itemName.indexOf(PROBE_SEPARATOR);
        if (separatorIndex <= 0) {
            return;
        }

        final String probeId = itemName.substring(0, separatorIndex);
        final TranslationProbe probe = translationProbesById.get(probeId);
        if (probe == null) {
            return;
        }

        event.setCancelled(true);

        final String responseValue = itemName.substring(separatorIndex + 1);
        if (responseValue.isBlank()) {
            return;
        }

        if (!probe.translationKey().startsWith(responseValue)) {
            pendingDetections.add(probe.modId());
        }
    }

    private static String createProbeId() {
        final String randomValue = Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        return randomValue.length() <= 8
                ? randomValue
                : randomValue.substring(randomValue.length() - 8);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        final String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private record TranslationProbe(
            String modId,
            String translationKey,
            String probeId
    ) {
    }
}
