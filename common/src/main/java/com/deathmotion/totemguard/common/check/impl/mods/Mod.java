/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.NBTUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public class Mod extends CheckImpl implements PacketCheck {

    private static final String REGISTER_CHANNEL = "minecraft:register";

    private static final int SIGN_LINES = 4;
    private static final int SIGN_LINE_LIMIT = 384;

    private static final char DELIM = '␟';
    private static final Pattern SIGN_ID_PATTERN = Pattern.compile("^[0-9a-z]+$");

    private static final long SIGN_TTL_NANOS = TimeUnit.SECONDS.toNanos(15);

    private static final List<ModSignature> SIGNATURES = List.of(
            new ModSignature("accurateblockplacement",
                    List.of("net.clayborn.accurateblockplacement.togglevanillaplacement"),
                    List.of()),
            new ModSignature("autototem", List.of(), List.of("autototem")),
            new ModSignature("tweakeroo", List.of(), List.of("servux:tweaks"))
    );

    private record SentEntry(ModSignature signature, String key) {}

    private static final class SentSign {
        final List<SentEntry> entriesInOrder;
        final long createdAtNanos;

        SentSign(List<SentEntry> entriesInOrder) {
            this.entriesInOrder = entriesInOrder;
            this.createdAtNanos = System.nanoTime();
        }
    }

    private final ConcurrentHashMap<String, SentSign> signIdToSent = new ConcurrentHashMap<>();
    private boolean isTriggered = false;

    public Mod(TGPlayer player) {
        super(player);
    }

    private void handle() {
        List<SentEntry> entries = collectEntries();
        if (entries.isEmpty()) return;

        List<NBTUtil.SignPayload<SentEntry>> payloads = NBTUtil.packTranslatablesIntoSigns(
                entries,
                SentEntry::key,
                this::newSignId,
                SIGN_LINE_LIMIT,
                DELIM
        );

        sendPayloads(payloads);
        TGPlatform.getInstance().getScheduler().runAsyncTaskDelayed(this::evictOldSignIds, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        } else if (type == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        } else if (type == PacketType.Play.Client.UPDATE_SIGN) {
            handleUpdateSign(event);
        } else if (type == PacketType.Play.Client.TELEPORT_CONFIRM) {
            triggerOnce();
        }
    }

    private void triggerOnce() {
        if (isTriggered) return;
        isTriggered = true;

        // For some reason if we run this too quickly, the sign doesn't get updated
        TGPlatform.getInstance().getScheduler().runAsyncTaskDelayed(this::handle, 2, TimeUnit.SECONDS);
    }

    private List<SentEntry> collectEntries() {
        List<SentEntry> all = new ArrayList<>();
        for (ModSignature sig : SIGNATURES) {
            List<String> keys = sig.translationKeys();
            if (keys == null || keys.isEmpty()) continue;

            for (String key : keys) {
                if (key == null || key.isBlank()) continue;
                if (key.length() >= SIGN_LINE_LIMIT) continue;
                all.add(new SentEntry(sig, key));
            }
        }
        return all;
    }

    private void sendPayloads(List<NBTUtil.SignPayload<SentEntry>> payloads) {
        User user = player.getUser();

        if (payloads.size() > 1028) {
            TGPlatform.getInstance().getLogger().warning("Mod check: Too many sign payloads to send to " + user.getName() + ", skipping mod check.");
            return;
        }

        WrapperPlayServerBundle bundle = new WrapperPlayServerBundle();
        Vector3i pos = player.getLocation()
                .getLocation()
                .getPosition()
                .toVector3i()
                .add(0, 2, 0);

        boolean wasSendingBundle = player.getData().isSendingBundlePacket();
        if (!wasSendingBundle) user.sendPacket(bundle);

        for (NBTUtil.SignPayload<SentEntry> payload : payloads) {
            if (payload.entriesInOrder().isEmpty()) continue;

            signIdToSent.put(payload.signId(), new SentSign(payload.entriesInOrder()));

            List<Component> lines = payload.lines();
            NBTCompound signNbt = NBTUtil.buildSignNbt(lines, player.getClientVersion());

            WrapperPlayServerBlockChange place = new WrapperPlayServerBlockChange(pos, WrappedBlockState.getDefaultState(StateTypes.OAK_SIGN));
            WrapperPlayServerBlockEntityData data = new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, signNbt);
            WrapperPlayServerOpenSignEditor open = new WrapperPlayServerOpenSignEditor(pos, true);
            WrapperPlayServerCloseWindow closeWindow = new WrapperPlayServerCloseWindow();

            user.sendPacket(place);
            user.sendPacket(data);
            user.sendPacket(open);
            user.sendPacket(closeWindow);
        }

        // TODO: Set back the block to the original state

        if (!wasSendingBundle) user.sendPacket(bundle);
    }

    private void handleUpdateSign(PacketReceiveEvent event) {
        String[] lines = readUpdateSignLines(event);
        if (lines == null || lines.length < 1) return;

        Split first = splitFirst(lines[0]);
        if (first == null) return;

        SentSign sent = consumeSentSign(first.head);
        if (sent == null) return;

        List<String> received = flattenReceived(first.tail, lines);
        evaluateResponse(sent, received);
    }

    private String[] readUpdateSignLines(PacketReceiveEvent event) {
        try {
            return new WrapperPlayClientUpdateSign(event).getTextLines();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Split splitFirst(String line0) {
        if (line0 == null || line0.isEmpty()) return null;

        int idx = line0.indexOf(DELIM);
        String signId = (idx < 0) ? line0 : line0.substring(0, idx);

        if (signId.isEmpty() || !SIGN_ID_PATTERN.matcher(signId).matches()) return null;

        String tail = (idx < 0) ? "" : line0.substring(idx + 1);
        return new Split(signId, tail);
    }

    private SentSign consumeSentSign(String signId) {
        return signIdToSent.remove(signId);
    }

    private List<String> flattenReceived(String tailLine0, String[] lines) {
        List<String> out = new ArrayList<>();

        if (tailLine0 != null && !tailLine0.isEmpty()) {
            splitAllInto(tailLine0, out);
        }

        for (int i = 1; i < Math.min(SIGN_LINES, lines.length); i++) {
            String li = lines[i];
            if (li == null || li.isEmpty()) continue;
            splitAllInto(li, out);
        }

        return out;
    }

    private void evaluateResponse(SentSign sent, List<String> received) {
        int n = Math.min(received.size(), sent.entriesInOrder.size());
        for (int i = 0; i < n; i++) {
            SentEntry entry = sent.entriesInOrder.get(i);
            String rendered = received.get(i);
            if (rendered == null) continue;

            if (!rendered.trim().equals(entry.key.trim())) {
                fail(entry.signature.name());
                return;
            }
        }
    }

    private void splitAllInto(String s, List<String> out) {
        int start = 0;
        int len = s.length();
        for (int i = 0; i <= len; i++) {
            if (i == len || s.charAt(i) == DELIM) {
                if (i > start) out.add(s.substring(start, i));
                start = i + 1;
            }
        }
    }

    private String newSignId() {
        String id = Integer.toUnsignedString(ThreadLocalRandom.current().nextInt(), 36);
        for (int i = 0; i < 16; i++) {
            if (!signIdToSent.containsKey(id)) return id;
            id = Integer.toUnsignedString(ThreadLocalRandom.current().nextInt(), 36);
        }
        return id + Integer.toUnsignedString(ThreadLocalRandom.current().nextInt(), 36);
    }

    private void evictOldSignIds() {
        long now = System.nanoTime();
        signIdToSent.entrySet().removeIf(e -> (now - e.getValue().createdAtNanos) > SIGN_TTL_NANOS);
    }

    private void handlePluginMessage(String channel, byte[] data) {
        if (channel == null) return;

        String normalized = channel.toLowerCase();

        if (REGISTER_CHANNEL.equals(normalized)) {
            String payload = new String(data, StandardCharsets.UTF_8);
            for (String entry : payload.split("\0")) {
                checkKeywords(entry.toLowerCase());
            }
            return;
        }

        checkKeywords(normalized);
    }

    private void checkKeywords(String value) {
        for (ModSignature sig : SIGNATURES) {
            for (String keyword : sig.pluginMessageKeywords()) {
                if (keyword != null && !keyword.isEmpty() && value.contains(keyword.toLowerCase())) {
                    fail(sig.name());
                    return;
                }
            }
        }
    }

    public record ModSignature(
            String name,
            List<String> translationKeys,
            List<String> pluginMessageKeywords
    ) {
    }

    private record Split(String head, String tail) {}
}
