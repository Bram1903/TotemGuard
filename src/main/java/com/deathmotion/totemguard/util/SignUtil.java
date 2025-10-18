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

package com.deathmotion.totemguard.util;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@UtilityClass
public final class SignUtil {
    private static final int MAX_KEYS = 3;

    private static final String NBT_FRONT_TEXT = "front_text";
    private static final String NBT_BACK_TEXT = "back_text";
    private static final String NBT_MESSAGES = "messages";
    private static final String NBT_GLOWING = "has_glowing_text";
    private static final String NBT_IS_WAXED = "is_waxed";
    private static final String COMP_TEXT = "text";
    private static final String COMP_KEYBIND = "keybind";

    private static final ServerVersion SERVER_VERSION = PacketEvents.getAPI().getServerManager().getVersion();

    private static final Logger LOG = TotemGuard.getInstance().getLogger();

    public static void placeSign(final TotemPlayer totemPlayer, final UUID packetSecret, final List<String> keys) {
        final Location loc = totemPlayer.bukkitPlayer.getLocation();
        final boolean wasBundling = totemPlayer.sendingBundlePacket;
        final NBTCompound signNbt = buildSignNbt(packetSecret, keys);

        FoliaScheduler.getRegionScheduler().run(TotemGuard.getInstance(), loc, task -> {
            final WrappedBlockState original = SpigotConversionUtil.fromBukkitBlockData(loc.getBlock().getState().getBlockData());

            FoliaScheduler.getAsyncScheduler().runNow(TotemGuard.getInstance(), task2 -> {
                final Vector3i pos = SpigotConversionUtil.fromBukkitLocation(loc).getPosition().toVector3i();

                final WrapperPlayServerBundle bundle = new WrapperPlayServerBundle();
                final WrapperPlayServerBlockChange placeSign = new WrapperPlayServerBlockChange(pos, WrappedBlockState.getDefaultState(StateTypes.OAK_SIGN));
                final WrapperPlayServerBlockEntityData setText = new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, signNbt);
                final WrapperPlayServerOpenSignEditor openEditor = new WrapperPlayServerOpenSignEditor(pos, true);
                final WrapperPlayServerCloseWindow closeEditor = new WrapperPlayServerCloseWindow();
                final WrapperPlayServerBlockChange revertBlock = new WrapperPlayServerBlockChange(pos, original);

                if (!wasBundling) totemPlayer.user.sendPacket(bundle);
                totemPlayer.user.sendPacket(placeSign);
                totemPlayer.user.sendPacket(setText);
                totemPlayer.user.sendPacket(openEditor);
                totemPlayer.user.sendPacket(closeEditor);
                totemPlayer.user.sendPacket(revertBlock);
                if (!wasBundling) totemPlayer.user.sendPacket(bundle);
            });
        });
    }

    public static boolean isSignContentValid(final UUID packetSecret, final String[] textLines, final List<String> originalKeys) {
        if (!isOurSign(packetSecret, textLines)) return false;

        final List<String> keys = safeKeys(originalKeys);
        for (int i = 1; i < (textLines == null ? 0 : textLines.length); i++) {
            final String line = textLines[i];
            if (line != null && !line.isEmpty() && !keys.contains(line)) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull NBTCompound buildSignNbt(final UUID packetSecret, final List<String> keys) {
        if (SERVER_VERSION.isOlderThan(ServerVersion.V_1_20)) {
            // Legacy (1.19.x): Text1..Text4 JSON strings
            final List<String> jsonLines = buildFourJsonStrings(packetSecret, keys);
            final NBTCompound legacy = new NBTCompound();
            for (int i = 0; i < 4; i++) {
                legacy.setTag("Text" + (i + 1), new NBTString(jsonLines.get(i)));
            }
            return legacy;
        }

        if (SERVER_VERSION.isNewerThanOrEquals(ServerVersion.V_1_21_5)) {
            // 1.21.5+: messages are COMPOUNDS (components)
            final List<NBTCompound> comps = buildFourCompounds(packetSecret, keys);
            final NBTList<@NotNull NBTCompound> messages = new NBTList<>(NBTType.COMPOUND);
            for (NBTCompound c : comps) messages.addTag(c);

            return signRoot(messages);
        }

        // 1.20–1.21.4: messages are STRINGs (each a JSON text component)
        final List<String> jsonLines = buildFourJsonStrings(packetSecret, keys);
        final NBTList<@NotNull NBTString> messages = new NBTList<>(NBTType.STRING);
        for (String json : jsonLines) messages.addTag(new NBTString(json));

        return signRoot(messages);
    }

    private static @NotNull NBTCompound signRoot(final NBTList<?> messages) {
        final NBTCompound text = compoundWithMessages(messages);
        final NBTCompound root = new NBTCompound();
        root.setTag(NBT_FRONT_TEXT, text);
        root.setTag(NBT_BACK_TEXT, text);
        root.setTag(NBT_IS_WAXED, new NBTByte((byte) 0));
        return root;
    }

    private static @NotNull NBTCompound compoundWithMessages(final NBTList<?> messages) {
        final NBTCompound c = new NBTCompound();
        c.setTag(NBT_MESSAGES, messages);
        c.setTag(NBT_GLOWING, new NBTByte((byte) 0));
        return c;
    }

    // For 1.20–1.21.4 (JSON strings)
    private static @NotNull List<String> buildFourJsonStrings(final UUID secret, final List<String> keys) {
        return fourLines(secret, keys,
                SignUtil::jsonText,
                SignUtil::jsonKeybind,
                SignUtil::jsonEmpty);
    }

    // For 1.21.5+ (component compounds)
    private static @NotNull List<NBTCompound> buildFourCompounds(final UUID secret, final List<String> keys) {
        final ArrayList<NBTCompound> out = new ArrayList<>(4);
        final List<String> safe = safeKeys(keys);
        warnOnKeyCount(safe);

        out.add(compText(secret == null ? "" : secret.toString()));
        out.add(safe.size() >= 1 && nonEmpty(safe.get(0)) ? compKeybind(safe.get(0)) : compText(""));
        out.add(safe.size() >= 2 && nonEmpty(safe.get(1)) ? compKeybind(safe.get(1)) : compText(""));
        out.add(safe.size() >= 3 && nonEmpty(safe.get(2)) ? compKeybind(safe.get(2)) : compText(""));
        return out;
    }

    private static @NotNull List<String> fourLines(final UUID secret, final List<String> keys, final LineRenderer textRenderer, final LineRenderer keyRenderer, final SupplierRenderer emptyRenderer) {
        final List<String> safe = safeKeys(keys);
        warnOnKeyCount(safe);

        final ArrayList<String> out = new ArrayList<>(4);
        out.add(textRenderer.render(secret == null ? "" : secret.toString()));
        out.add(safe.size() >= 1 && nonEmpty(safe.get(0)) ? keyRenderer.render(safe.get(0)) : emptyRenderer.render());
        out.add(safe.size() >= 2 && nonEmpty(safe.get(1)) ? keyRenderer.render(safe.get(1)) : emptyRenderer.render());
        out.add(safe.size() >= 3 && nonEmpty(safe.get(2)) ? keyRenderer.render(safe.get(2)) : emptyRenderer.render());
        return out;
    }

    private static void warnOnKeyCount(final List<String> keys) {
        if (keys.isEmpty()) {
            LOG.warning("Tried to send a sign without any content!");
        }
        if (keys.size() > MAX_KEYS) {
            LOG.warning("Tried to send a sign with more than " + MAX_KEYS + " lines of content!");
        }
    }

    private static @NotNull List<String> safeKeys(final List<String> keys) {
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        return keys;
    }

    private static boolean nonEmpty(final String s) {
        return s != null && !s.isEmpty();
    }

    private static @NotNull String jsonText(final String text) {
        return "{\"text\":" + quote(text) + "}";
    }

    private static @NotNull String jsonKeybind(final String keybind) {
        return "{\"keybind\":" + quote(keybind == null ? "" : keybind) + "}";
    }

    private static @NotNull String jsonEmpty() {
        return "{\"text\":\"\"}";
    }

    private static @NotNull String quote(String s) {
        if (s == null) s = "";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static @NotNull NBTCompound compText(final String text) {
        final NBTCompound c = new NBTCompound();
        c.setTag(COMP_TEXT, new NBTString(text == null ? "" : text));
        return c;
    }

    private static @NotNull NBTCompound compKeybind(final String key) {
        final NBTCompound c = new NBTCompound();
        c.setTag(COMP_KEYBIND, new NBTString(key == null ? "" : key));
        return c;
    }

    private static boolean isOurSign(final UUID packetSecret, final String[] textLines) {
        if (packetSecret == null || textLines == null || textLines.length == 0) return false;
        final String first = textLines[0];
        if (first == null) return false;
        try {
            return UUID.fromString(first).equals(packetSecret);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private interface LineRenderer {
        String render(String s);
    }

    private interface SupplierRenderer {
        String render();
    }
}

