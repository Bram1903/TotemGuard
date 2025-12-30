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

    private static final String NBT_FRONT_TEXT = "front_text";
    private static final String NBT_BACK_TEXT = "back_text";
    private static final String NBT_MESSAGES = "messages";
    private static final String NBT_FILTERED_MESSAGES = "filtered_messages";
    private static final String NBT_GLOWING = "has_glowing_text";
    private static final String NBT_COLOR = "color";
    private static final String NBT_IS_WAXED = "is_waxed";

    private NBTUtil() {
    }

    public static List<Component> buildLines(String batchId, List<Map.Entry<ModSignature, String>> batch) {
        Component l0 = Component.text(batchId);
        Component l1 = !batch.isEmpty() ? Component.translatable(batch.get(0).getValue()) : Component.empty();
        Component l2 = batch.size() > 1 ? Component.translatable(batch.get(1).getValue()) : Component.empty();
        Component l3 = batch.size() > 2 ? Component.translatable(batch.get(2).getValue()) : Component.empty();
        return List.of(l0, l1, l2, l3);
    }

    public static <T> List<List<T>> chunk(List<T> list) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 3) {
            out.add(list.subList(i, Math.min(list.size(), i + 3)));
        }
        return out;
    }

    public static NBTCompound buildSignNbt(List<Component> lines, ClientVersion clientVersion) {
        ClientVersion version = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        if (TGPlatform.getInstance().isProxy()) {
            version = clientVersion;
        }

        if (version.isOlderThan(ClientVersion.V_1_20)) {
            return signNbt_legacy(lines, clientVersion);
        }
        if (version.isNewerThanOrEquals(ClientVersion.V_1_21_5)) {
            return signNbt_1_21_5_plus(lines, clientVersion);
        }
        return signNbt_1_20_to_1_21_4(lines, clientVersion);
    }

    private static NBTCompound signNbt_1_20_to_1_21_4(List<Component> lines, ClientVersion clientVersion) {
        AdventureSerializer adv = AdventureSerializer.serializer(clientVersion);

        NBTList<NBTString> messages = new NBTList<>(NBTType.STRING, 4);
        NBTList<NBTString> filtered = new NBTList<>(NBTType.STRING, 4);

        for (Component line : lines) {
            String json = adv.asJson(line);
            messages.addTag(new NBTString(json));
            filtered.addTag(new NBTString(json));
        }

        return signRoot(messages, filtered);
    }

    private static NBTCompound signNbt_1_21_5_plus(List<Component> lines, ClientVersion clientVersion) {
        AdventureSerializer adv = AdventureSerializer.serializer(clientVersion);
        PacketWrapper<?> wrapper = PacketWrapper.createDummyWrapper(clientVersion);

        NBTList<NBTCompound> messages = new NBTList<>(NBTType.COMPOUND, 4);
        NBTList<NBTCompound> filtered = new NBTList<>(NBTType.COMPOUND, 4);

        for (Component line : lines) {
            NBT tag = adv.asNbtTag(line, wrapper);
            NBTCompound c = toComponentCompound(tag);
            messages.addTag(c);
            filtered.addTag(c);
        }

        return signRoot(messages, filtered);
    }

    private static NBTCompound toComponentCompound(NBT tag) {
        if (tag instanceof NBTCompound c) {
            return c;
        }

        if (tag instanceof NBTString s) {
            NBTCompound c = new NBTCompound();
            c.setTag("text", s);
            return c;
        }

        return emptyComponentCompound();
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

    private static NBTCompound emptyComponentCompound() {
        NBTCompound c = new NBTCompound();
        c.setTag("text", new NBTString(""));
        return c;
    }

    private static NBTCompound signNbt_legacy(List<Component> lines, ClientVersion clientVersion) {
        AdventureSerializer adv = AdventureSerializer.serializer(clientVersion);

        NBTCompound root = new NBTCompound();
        root.setTag("Text1", new NBTString(adv.asLegacy(lines.get(0))));
        root.setTag("Text2", new NBTString(adv.asLegacy(lines.get(1))));
        root.setTag("Text3", new NBTString(adv.asLegacy(lines.get(2))));
        root.setTag("Text4", new NBTString(adv.asLegacy(lines.get(3))));
        return root;
    }
}
