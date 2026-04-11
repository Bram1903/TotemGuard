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

package com.deathmotion.totemguard.common.gui;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public final class GuiItems {

    private static final Component BLANK_NAME = Component.text(" ", NamedTextColor.WHITE);

    private GuiItems() {
    }

    public static ItemStack filler() {
        return simple(
                ItemTypes.GRAY_STAINED_GLASS_PANE,
                BLANK_NAME,
                List.of()
        );
    }

    public static ItemStack simple(ItemType type, Component name, List<Component> lore) {
        ItemStack item = ItemStack.builder()
                .type(type)
                .build();
        applyMeta(item, name, lore);
        return item;
    }

    public static ItemStack from(ItemStack base, Component name, List<Component> lore) {
        ItemStack item = base == null || base.isEmpty() ? ItemStack.EMPTY : base.copy();
        if (item.isEmpty()) {
            item = ItemStack.builder().type(ItemTypes.BARRIER).build();
        }
        applyMeta(item, name, lore);
        return item;
    }

    public static ItemStack playerHead(UserProfile profile, Component name, List<Component> lore) {
        ItemStack item = ItemStack.builder()
                .type(ItemTypes.PLAYER_HEAD)
                .build();

        if (profile != null) {
            List<ItemProfile.Property> properties = profile.getTextureProperties().stream()
                    .map(GuiItems::toItemProperty)
                    .toList();
            item.setComponent(ComponentTypes.PROFILE, new ItemProfile(profile.getName(), profile.getUUID(), properties));
        }

        applyMeta(item, name, lore);
        return item;
    }

    private static ItemProfile.Property toItemProperty(TextureProperty property) {
        return new ItemProfile.Property(
                property.getName(),
                property.getValue(),
                property.getSignature()
        );
    }

    private static void applyMeta(ItemStack item, Component name, List<Component> lore) {
        item.setComponent(ComponentTypes.CUSTOM_NAME, name);
        if (lore == null || lore.isEmpty()) {
            item.unsetComponent(ComponentTypes.LORE);
            return;
        }
        item.setComponent(ComponentTypes.LORE, new ItemLore(List.copyOf(lore)));
    }
}
