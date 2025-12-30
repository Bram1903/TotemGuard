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

package com.deathmotion.totemguard.common.util;

import com.deathmotion.totemguard.common.TGPlatform;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class NBTUtil {

    private static final int SIGN_LINES = 4;

    private static final String NBT_FRONT_TEXT = "front_text";
    private static final String NBT_BACK_TEXT = "back_text";
    private static final String NBT_MESSAGES = "messages";
    private static final String NBT_FILTERED_MESSAGES = "filtered_messages";
    private static final String NBT_GLOWING = "has_glowing_text";
    private static final String NBT_COLOR = "color";
    private static final String NBT_IS_WAXED = "is_waxed";

    private static final String COMPONENT_TEXT_KEY = "text";

    private NBTUtil() {}

    public record SignPayload<T>(String signId, List<Component> lines, List<T> entriesInOrder) {}

    @FunctionalInterface
    public interface KeyExtractor<T> {
        String getKey(T t);
    }

    @FunctionalInterface
    public interface IdSupplier {
        String get();
    }

    public static <T> List<SignPayload<T>> packTranslatablesIntoSigns(
            List<T> entries,
            KeyExtractor<T> keyExtractor,
            IdSupplier idSupplier,
            int maxLineChars,
            char delim
    ) {
        List<SignPayload<T>> out = new ArrayList<>();
        int idx = 0;

        while (idx < entries.size()) {
            PackResult<T> r = packOneSign(entries, idx, keyExtractor, idSupplier.get(), maxLineChars, delim);
            idx = r.nextIndex;

            if (r.sentOrder.isEmpty()) {
                if (idx == r.startIndex) idx = idx + 1;
                continue;
            }

            out.add(new SignPayload<>(r.signId, r.lines, r.sentOrder));
        }

        return out;
    }

    private record PackResult<T>(int startIndex, int nextIndex, String signId, List<Component> lines, List<T> sentOrder) { }

    private static <T> PackResult<T> packOneSign(
            List<T> entries,
            int startIdx,
            KeyExtractor<T> keyExtractor,
            String signId,
            int maxLineChars,
            char delim
    ) {
        List<Component> lines = new ArrayList<>(SIGN_LINES);
        List<T> sentOrder = new ArrayList<>();
        int idx = startIdx;

        for (int line = 0; line < SIGN_LINES; line++) {
            LinePack<T> lp = packOneLine(entries, idx, keyExtractor, signId, line, maxLineChars, delim);
            lines.add(lp.component);
            sentOrder.addAll(lp.sent);
            idx = lp.nextIndex;
            if (idx >= entries.size()) break;
        }

        while (lines.size() < SIGN_LINES) lines.add(Component.empty());

        return new PackResult<>(startIdx, idx, signId, List.copyOf(lines), List.copyOf(sentOrder));
    }

    private record LinePack<T>(Component component, List<T> sent, int nextIndex) { }

    private static <T> LinePack<T> packOneLine(
            List<T> entries,
            int startIdx,
            KeyExtractor<T> keyExtractor,
            String signId,
            int lineIndex,
            int maxLineChars,
            char delim
    ) {
        Component comp = Component.empty();
        int used = 0;
        boolean hasAny = false;

        if (lineIndex == 0) {
            comp = comp.append(Component.text(signId));
            used = signId.length();
        }

        List<T> sent = new ArrayList<>();
        int idx = startIdx;

        while (idx < entries.size()) {
            T entry = entries.get(idx);
            String key = keyExtractor.getKey(entry);

            if (key == null || key.isBlank()) {
                idx++;
                continue;
            }

            if (key.length() >= maxLineChars) {
                idx++;
                continue;
            }

            boolean needDelim = (lineIndex == 0) || hasAny;
            int needed = key.length() + (needDelim ? 1 : 0);

            if (used + needed > maxLineChars) {
                break;
            }

            if (needDelim) {
                comp = comp.append(Component.text(String.valueOf(delim)));
                used += 1;
            }

            comp = comp.append(Component.translatable(key));
            used += key.length();

            sent.add(entry);
            idx++;
            hasAny = true;
        }

        return new LinePack<>(comp, sent, idx);
    }

    public static NBTCompound buildSignNbt(List<Component> lines, ClientVersion clientVersion) {
        ClientVersion version = effectiveVersion(clientVersion);

        if (version.isOlderThan(ClientVersion.V_1_20)) {
            return signNbtLegacy(lines, clientVersion);
        }
        if (version.isNewerThanOrEquals(ClientVersion.V_1_21_5)) {
            return signNbtModern(lines, clientVersion, true);
        }
        return signNbtModern(lines, clientVersion, false);
    }

    private static ClientVersion effectiveVersion(ClientVersion clientVersion) {
        ClientVersion server = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        return TGPlatform.getInstance().isProxy() ? clientVersion : server;
    }

    private static NBTCompound signNbtModern(List<Component> lines, ClientVersion clientVersion, boolean componentAsCompound) {
        AdventureSerializer adv = AdventureSerializer.serializer(clientVersion);
        PacketWrapper<?> wrapper = componentAsCompound ? PacketWrapper.createDummyWrapper(clientVersion) : null;

        if (componentAsCompound) {
            NBTList<NBTCompound> messages = new NBTList<>(NBTType.COMPOUND, SIGN_LINES);
            NBTList<NBTCompound> filtered = new NBTList<>(NBTType.COMPOUND, SIGN_LINES);

            for (Component line : lines) {
                NBTCompound c = toComponentCompound(adv.asNbtTag(line, wrapper));
                messages.addTag(c);
                filtered.addTag(c);
            }
            return signRoot(messages, filtered);
        } else {
            NBTList<NBTString> messages = new NBTList<>(NBTType.STRING, SIGN_LINES);
            NBTList<NBTString> filtered = new NBTList<>(NBTType.STRING, SIGN_LINES);

            for (Component line : lines) {
                String json = adv.asJson(line);
                NBTString s = new NBTString(json);
                messages.addTag(s);
                filtered.addTag(s);
            }
            return signRoot(messages, filtered);
        }
    }

    private static NBTCompound toComponentCompound(NBT tag) {
        if (tag instanceof NBTCompound c) {
            return c;
        }
        if (tag instanceof NBTString s) {
            NBTCompound c = new NBTCompound();
            c.setTag(COMPONENT_TEXT_KEY, s);
            return c;
        }
        return emptyComponentCompound();
    }

    private static NBTCompound emptyComponentCompound() {
        NBTCompound c = new NBTCompound();
        c.setTag(COMPONENT_TEXT_KEY, new NBTString(""));
        return c;
    }

    private static NBTCompound signRoot(NBTList<?> messages, NBTList<?> filtered) {
        NBTCompound side = new NBTCompound();
        side.setTag(NBT_MESSAGES, messages);
        side.setTag(NBT_FILTERED_MESSAGES, filtered);
        side.setTag(NBT_GLOWING, new NBTByte((byte) 0));
        side.setTag(NBT_COLOR, new NBTString("black"));

        NBTCompound root = new NBTCompound();
        root.setTag(NBT_FRONT_TEXT, side);
        root.setTag(NBT_BACK_TEXT, side);
        root.setTag(NBT_IS_WAXED, new NBTByte((byte) 0));
        return root;
    }

    private static NBTCompound signNbtLegacy(List<Component> lines, ClientVersion clientVersion) {
        AdventureSerializer adv = AdventureSerializer.serializer(clientVersion);

        NBTCompound root = new NBTCompound();
        root.setTag("Text1", new NBTString(adv.asLegacy(lines.get(0))));
        root.setTag("Text2", new NBTString(adv.asLegacy(lines.get(1))));
        root.setTag("Text3", new NBTString(adv.asLegacy(lines.get(2))));
        root.setTag("Text4", new NBTString(adv.asLegacy(lines.get(3))));
        return root;
    }
}
