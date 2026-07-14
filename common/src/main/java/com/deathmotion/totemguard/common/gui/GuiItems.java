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

import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemTooltipDisplay;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Dummy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Set;

public final class GuiItems {

    private static final Component BLANK_NAME = Component.text(" ", Palette.PARCH_50);

    private GuiItems() {
    }

    public static ItemStack filler() {
        return filler(ItemTypes.GRAY_STAINED_GLASS_PANE);
    }

    public static ItemStack filler(ItemType type) {
        ItemStack item = simple(type, BLANK_NAME, List.of());
        hideTooltip(item);
        return item;
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
        item.setComponent(ComponentTypes.CUSTOM_NAME, guiText(name));
        if (lore == null || lore.isEmpty()) {
            item.unsetComponent(ComponentTypes.LORE);
            return;
        }
        item.setComponent(ComponentTypes.LORE, new ItemLore(lore.stream()
                .map(GuiItems::guiText)
                .toList()));
    }

    private static void hideTooltip(ItemStack item) {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        if (version.isNewerThanOrEquals(ServerVersion.V_1_21_5)) {
            item.setComponent(ComponentTypes.TOOLTIP_DISPLAY, new ItemTooltipDisplay(true, Set.of()));
            return;
        }

        if (version.isNewerThanOrEquals(ServerVersion.V_1_20_5)) {
            item.setComponent(ComponentTypes.HIDE_TOOLTIP, Dummy.DUMMY);
        }
    }

    private static Component guiText(Component component) {
        Component text = component == null ? BLANK_NAME : component;
        if (text.decoration(TextDecoration.ITALIC) != TextDecoration.State.NOT_SET) {
            return text;
        }
        return text.decoration(TextDecoration.ITALIC, false);
    }
}
