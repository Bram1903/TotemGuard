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

package com.deathmotion.totemguard.common.punishment;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemDeathProtection;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;

import java.util.List;
import java.util.UUID;

public final class BanAnimation {

    public static final long ANIMATION_DURATION_MS = 1250L;
    private static final int TOTEM_OF_UNDYING_STATUS = 35;
    private static final String SKIN_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTlmOTJkMjI3YTY2MDg0NGI2OWY1OWMzOTRkNDNhY2U3MWNiMzZkMzIzNTIxMTQ4YWYxMTQ2MzdhMDg0ZjZiZCJ9fX0=";

    private static final UUID PROFILE_ID = new UUID(0L, 0L);

    private BanAnimation() {
    }

    /**
     * @return {@code true} if the client supports the hacker-head pop. Older
     * clients can't render it (the totem-pop check is hardcoded to the totem
     * item type pre-1.21.2), so callers should skip the animation entirely.
     */
    public static boolean isSupported(TGPlayer player) {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2);
    }

    public static void play(TGPlayer player) {
        if (!isSupported(player)) return;

        User user = player.getUser();
        int entityId = user.getEntityId();

        ItemStack originalMainHand = player.getInventory().getMainHandItem();
        ItemStack head = buildHead();

        user.sendPacketSilently(new WrapperPlayServerEntityEquipment(
                entityId,
                List.of(new Equipment(EquipmentSlot.MAIN_HAND, head))
        ));
        user.sendPacketSilently(new WrapperPlayServerEntityStatus(entityId, TOTEM_OF_UNDYING_STATUS));
        user.sendPacketSilently(new WrapperPlayServerEntityEquipment(
                entityId,
                List.of(new Equipment(EquipmentSlot.MAIN_HAND, originalMainHand))
        ));
    }

    private static ItemStack buildHead() {
        ItemProfile.Property property = new ItemProfile.Property("textures", SKIN_TEXTURE, null);
        ItemProfile profile = new ItemProfile(null, PROFILE_ID, List.of(property));
        return ItemStack.builder()
                .type(ItemTypes.PLAYER_HEAD)
                .amount(1)
                .component(ComponentTypes.DEATH_PROTECTION, new ItemDeathProtection(List.of()))
                .component(ComponentTypes.PROFILE, profile)
                .build();
    }
}
