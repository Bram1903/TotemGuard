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
import com.deathmotion.totemguard.common.check.impl.mods.ModSignature;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NBTUtil {

    private static final int SIGN_LINES = 4;
    private static final int CHUNK_SIZE = 3;

    private static final String NBT_FRONT_TEXT = "front_text";
    private static final String NBT_BACK_TEXT = "back_text";
    private static final String NBT_MESSAGES = "messages";
    private static final String NBT_FILTERED_MESSAGES = "filtered_messages";
    private static final String NBT_GLOWING = "has_glowing_text";
    private static final String NBT_COLOR = "color";
    private static final String NBT_IS_WAXED = "is_waxed";

    private static final String COMPONENT_TEXT_KEY = "text";

    private NBTUtil() {}

    public static List<Component> buildLines(String batchId, List<Map.Entry<ModSignature, String>> batch) {
        List<Component> out = new ArrayList<>(SIGN_LINES);
        out.add(Component.text(batchId));
        for (int i = 0; i < 3; i++) {
            out.add(i < batch.size() ? Component.translatable(batch.get(i).getValue()) : Component.empty());
        }
        return out;
    }

    public static <T> List<List<T>> chunk(List<T> list) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += CHUNK_SIZE) {
            out.add(list.subList(i, Math.min(list.size(), i + CHUNK_SIZE)));
        }
        return out;
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
