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
import com.deathmotion.totemguard.api3.config.ConfigFile;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public final class Mod extends CheckImpl implements PacketCheck {

    private static final String REGISTER_CHANNEL = "minecraft:register";

    private static final int ANVIL_SCREEN = 8;

    private static final int TRANSLATION_BATCH_SIZE = 50;
    private static final long TRANSLATION_BATCH_DELAY_MS = 150L;

    private final Set<String> flaggedMods = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingDetections = ConcurrentHashMap.newKeySet();

    private final Map<String, String> translationKeyByProbeId = new HashMap<>();
    private final Map<String, String> modByProbeId = new HashMap<>();

    private final Object translationLock = new Object();
    private List<Map.Entry<String, String>> translationEntries = List.of();
    private int translationIndex = 0;
    private int translationRunId = 0;
    private boolean translationActive = false;

    public Mod(TGPlayer player) {
        super(player);

        ModSignatures.load(player.getPlatform().getConfigRepository().config(ConfigFile.MODS));
        reload();
    }

    private static String normalize(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static String nextProbeId() {
        String s = Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        return (s.length() <= 8) ? s : s.substring(s.length() - 8);
    }

    private static int randomWindowId() {
        return -ThreadLocalRandom.current().nextInt(10_000, Integer.MAX_VALUE);
    }

    private static int randomStateId() {
        return ThreadLocalRandom.current().nextInt();
    }

    @Override
    public void reload() {
        super.reload();

        translationKeyByProbeId.clear();
        modByProbeId.clear();

        for (Map.Entry<String, ModSignature> entry : ModSignatures.get().entrySet()) {
            final String modId = entry.getKey();
            final ModSignature sig = entry.getValue();

            for (String translationKey : sig.translations()) {
                final String probeId = nextProbeId();
                translationKeyByProbeId.put(probeId, translationKey);
                modByProbeId.put(probeId, modId);
            }
        }

        synchronized (translationLock) {
            translationRunId++;
            translationActive = false;
            translationEntries = List.of();
            translationIndex = 0;
        }
    }

    public void handle() {
        flushDetections();
        startTranslationProbeRunIfNeeded();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.NAME_ITEM) {
            handleTranslationResponse(event);
            flushDetections();
            return;
        }

        if (type == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
            return;
        }

        if (type == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        }
    }

    private void startTranslationProbeRunIfNeeded() {
        final int runId;
        final boolean shouldRun;

        synchronized (translationLock) {
            translationRunId++;
            runId = translationRunId;

            translationEntries = new ArrayList<>(translationKeyByProbeId.entrySet());
            translationIndex = 0;
            translationActive = !translationEntries.isEmpty();
            shouldRun = translationActive;
        }

        if (shouldRun) {
            platform.getScheduler().runAsyncTask(() -> sendTranslationBatch(runId));
        }
    }

    private void sendTranslationBatch(int runId) {
        final List<Map.Entry<String, String>> batch;
        final boolean hasMore;

        synchronized (translationLock) {
            if (!translationActive || runId != translationRunId) {
                return;
            }

            if (translationIndex >= translationEntries.size()) {
                translationActive = false;
                return;
            }

            int endExclusive = Math.min(translationEntries.size(), translationIndex + TRANSLATION_BATCH_SIZE);
            batch = new ArrayList<>(translationEntries.subList(translationIndex, endExclusive));
            translationIndex = endExclusive;

            hasMore = translationIndex < translationEntries.size();
            if (!hasMore) {
                translationActive = false;
            }
        }

        final boolean wasSendingBundle = player.getData().isSendingBundlePacket();
        final WrapperPlayServerBundle delimiter = new WrapperPlayServerBundle();

        if (!wasSendingBundle) {
            player.getUser().sendPacket(delimiter);
        }

        for (Map.Entry<String, String> entry : batch) {
            final String probeId = entry.getKey();
            final String translationKey = entry.getValue();

            final int windowId = randomWindowId();
            final int stateId = randomStateId();

            sendAnvilProbe(windowId, stateId, probeId, translationKey);
        }

        if (!wasSendingBundle) {
            player.getUser().sendPacket(delimiter);
        }

        if (hasMore) {
            platform.getScheduler().runAsyncTaskDelayed(
                    () -> sendTranslationBatch(runId),
                    TRANSLATION_BATCH_DELAY_MS,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void sendAnvilProbe(int windowId, int stateId, String probeId, String translationKey) {
        player.getUser().sendPacket(new WrapperPlayServerOpenWindow(
                windowId,
                ANVIL_SCREEN,
                Component.text("Repair & Name"),
                0,
                true,
                0
        ));

        Component name = Component.text(probeId)
                .append(Component.text("|"))
                .append(Component.translatable(translationKey));

        player.getUser().sendPacket(new WrapperPlayServerSetSlot(
                windowId,
                stateId,
                0,
                new ItemStack.Builder()
                        .type(ItemTypes.DIAMOND_SWORD)
                        .component(ComponentTypes.CUSTOM_NAME, name)
                        .amount(1)
                        .build()
        ));

        player.getUser().sendPacket(new WrapperPlayServerCloseWindow(windowId));
    }

    private void handleTranslationResponse(PacketReceiveEvent event) {
        WrapperPlayClientNameItem packet = new WrapperPlayClientNameItem(event);

        final String itemName = packet.getItemName();
        if (itemName.isBlank()) {
            return;
        }

        final int sep = itemName.indexOf('|');
        if (sep <= 0) {
            return;
        }

        final String probeId = itemName.substring(0, sep);
        final String payload = itemName.substring(sep + 1);

        final String expectedKey = translationKeyByProbeId.get(probeId);
        final String modId = modByProbeId.get(probeId);
        if (expectedKey == null || modId == null) {
            return;
        }

        event.setCancelled(true);

        if (!expectedKey.startsWith(payload)) {
            failOnce(modId);
        }
    }

    private void handlePluginMessage(String channel, byte[] data) {
        if (channel == null) {
            return;
        }

        final String normalizedChannel = normalize(channel);

        if (REGISTER_CHANNEL.equals(normalizedChannel)) {
            final String payload = new String(data, StandardCharsets.UTF_8);
            for (String entry : payload.split("\0")) {
                detectFromValue(normalize(entry));
            }
        } else {
            detectFromValue(normalizedChannel);
        }

        flushDetections();
    }

    private void detectFromValue(String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        for (Map.Entry<String, ModSignature> modEntry : ModSignatures.get().entrySet()) {
            final String modId = modEntry.getKey();

            if (flaggedMods.contains(modId)) {
                continue;
            }

            for (String keyword : modEntry.getValue().payloads()) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                if (value.contains(keyword)) {
                    pendingDetections.add(modId);
                    break;
                }
            }
        }
    }

    private void flushDetections() {
        if (pendingDetections.isEmpty()) {
            return;
        }

        for (String modId : pendingDetections) {
            failOnce(modId);
        }

        pendingDetections.clear();
    }

    private void failOnce(String modId) {
        if (flaggedMods.add(modId)) {
            fail(modId);
        }
    }
}