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

package com.deathmotion.totemguard.common.features.mods;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class AnvilNameEcho {

    private static final char SEPARATOR = '|';
    private static final int ANVIL_WINDOW_TYPE = 8;
    private static final int ID_LENGTH = 8;

    private AnvilNameEcho() {
    }

    static String newId() {
        String value = Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        return value.length() <= ID_LENGTH ? value : value.substring(value.length() - ID_LENGTH);
    }

    static @Nullable Reflection parse(@Nullable String itemName) {
        if (itemName == null || itemName.isBlank()) return null;
        int separatorIndex = itemName.indexOf(SEPARATOR);
        if (separatorIndex <= 0) return null;
        return new Reflection(itemName.substring(0, separatorIndex), itemName.substring(separatorIndex + 1));
    }

    static void send(TGPlayer player, int windowId, List<Question> questions) {
        if (questions.isEmpty()) return;

        boolean wrapInBundle = !player.getData().isSendingBundlePacket();
        WrapperPlayServerBundle boundary = new WrapperPlayServerBundle();

        if (wrapInBundle) player.getUser().sendPacket(boundary);
        player.getUser().sendPacket(openAnvil(windowId));
        for (Question question : questions) player.getUser().sendPacket(fill(windowId, question));
        player.getUser().sendPacket(new WrapperPlayServerCloseWindow(windowId));
        if (wrapInBundle) player.getUser().sendPacket(boundary);
    }

    private static WrapperPlayServerOpenWindow openAnvil(int windowId) {
        return new WrapperPlayServerOpenWindow(windowId, ANVIL_WINDOW_TYPE, Component.text("Repair & Name"), 0, true, 0);
    }

    private static WrapperPlayServerSetSlot fill(int windowId, Question question) {
        Component itemName = Component.text(question.id())
                .append(Component.text(SEPARATOR))
                .append(question.content());

        return new WrapperPlayServerSetSlot(
                windowId,
                ThreadLocalRandom.current().nextInt(),
                0,
                new ItemStack.Builder()
                        .type(ItemTypes.DIAMOND_SWORD)
                        .component(ComponentTypes.CUSTOM_NAME, itemName)
                        .amount(1)
                        .build()
        );
    }

    record Question(String id, Component content) {
    }

    record Reflection(String id, String rendered) {
    }
}
