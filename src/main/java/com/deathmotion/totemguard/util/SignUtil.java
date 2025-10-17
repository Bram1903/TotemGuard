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
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@UtilityClass
public final class SignUtil {

    /*
     * A sign can hold up to 4 lines of text, but the first line is used for the secret
     */
    private final int MAX_KEYS = 3;

    public void placeSign(TotemPlayer totemPlayer, UUID packetSecret, List<String> keybinds) {
        FoliaScheduler.getAsyncScheduler().runNow(TotemGuard.getInstance(), (o -> {
            boolean wasSendingBundlePacket = totemPlayer.sendingBundlePacket;
            Location bukkitLocation = totemPlayer.bukkitPlayer.getLocation();
            WrappedBlockState originalBlockState = SpigotConversionUtil.fromBukkitBlockData(bukkitLocation.getBlock().getState().getBlockData());
            Vector3i location = SpigotConversionUtil.fromBukkitLocation(bukkitLocation).getPosition().toVector3i();
            NBTCompound exploitMessage = buildSignMessage(packetSecret, keybinds);

            WrapperPlayServerBundle bundlePacket = new WrapperPlayServerBundle();
            WrapperPlayServerBlockChange placeSignPacket = new WrapperPlayServerBlockChange(location, WrappedBlockState.getDefaultState(StateTypes.OAK_SIGN));
            WrapperPlayServerBlockEntityData updateSignContentPacket = new WrapperPlayServerBlockEntityData(location, BlockEntityTypes.SIGN, exploitMessage);
            WrapperPlayServerOpenSignEditor openSignPacket = new WrapperPlayServerOpenSignEditor(location, true);
            WrapperPlayServerCloseWindow closeSignPacket = new WrapperPlayServerCloseWindow();
            WrapperPlayServerBlockChange revertOriginalBlockState = new WrapperPlayServerBlockChange(location, originalBlockState);

            if (!wasSendingBundlePacket) totemPlayer.user.sendPacket(bundlePacket);
            totemPlayer.user.sendPacket(placeSignPacket);
            totemPlayer.user.sendPacket(updateSignContentPacket);
            totemPlayer.user.sendPacket(openSignPacket);
            totemPlayer.user.sendPacket(closeSignPacket);
            totemPlayer.user.sendPacket(revertOriginalBlockState);
            if (!wasSendingBundlePacket) totemPlayer.user.sendPacket(bundlePacket);
        }));
    }

    private static NBTCompound buildSignMessage(UUID packetSecret, List<String> keys) {
        NBTList<@NotNull NBTCompound> messages = buildLineContent(packetSecret, keys);

        NBTCompound frontText = new NBTCompound();
        frontText.setTag("messages", messages);

        NBTCompound backText = new NBTCompound();
        backText.setTag("messages", messages);

        NBTCompound result = new NBTCompound();
        result.setTag("front_text", frontText);
        result.setTag("back_text", backText);
        return result;
    }

    private static NBTList<@NotNull NBTCompound> buildLineContent(UUID packetSecret, List<String> keys) {
        List<String> safeKeys = keys == null ? Collections.emptyList() : keys;

        if (safeKeys.isEmpty()) {
            TotemGuard.getInstance().getLogger().warning("Tried to send a sign without any content!");
        }
        if (safeKeys.size() > MAX_KEYS) {
            TotemGuard.getInstance().getLogger().warning("Tried to send a sign with more than " + MAX_KEYS + " lines of content!");
        }

        NBTList<@NotNull NBTCompound> messages = new NBTList<>(NBTType.COMPOUND);

        // first tag contains the secret
        messages.addTag(textCompound(packetSecret.toString()));

        // then up to MAX_KEYS keybind entries, empty string if not present
        for (int i = 0; i < MAX_KEYS; i++) {
            if (i < safeKeys.size()) {
                String key = safeKeys.get(i);
                if (key != null && !key.isEmpty()) {
                    NBTCompound keybindComp = new NBTCompound();
                    keybindComp.setTag("keybind", new NBTString(key));
                    messages.addTag(keybindComp);
                    continue;
                }
            }
            // fallback empty text tag
            messages.addTag(textCompound(""));
        }

        return messages;
    }

    private static NBTCompound textCompound(String text) {
        NBTCompound comp = new NBTCompound();
        comp.setTag("text", new NBTString(text == null ? "" : text));
        return comp;
    }

    public static boolean isSignContentValid(UUID packetSecret, String[] textLines, List<String> originalKeys) {
        if (!isOurSign(packetSecret, textLines)) return false;

        List<String> keys = originalKeys == null ? Collections.emptyList() : originalKeys;

        // start at index 1 because index 0 is the secret
        for (int i = 1; i < textLines.length; i++) {
            String line = textLines[i];
            if (line != null && !line.isEmpty() && !keys.contains(line)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOurSign(UUID packetSecret, String[] textLines) {
        if (textLines == null || textLines.length == 0) return false;
        String secret = textLines[0];
        if (secret == null) return false;
        try {
            return UUID.fromString(secret).equals(packetSecret);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

