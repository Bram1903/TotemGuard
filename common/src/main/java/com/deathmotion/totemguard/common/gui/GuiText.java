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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;

public final class GuiText {

    private GuiText() {
    }

    public static Component line(String label, String value) {
        return Component.text(label + ": ", Palette.LABEL)
                .append(Component.text(value, Palette.VALUE));
    }

    public static Component status(String label, boolean value) {
        MessageService messages = TGPlatform.getInstance().getMessageService();
        return Component.text(label + ": ", Palette.LABEL)
                .append(messages.getComponent(value ? MessagesKeys.GUI_STATUS_YES : MessagesKeys.GUI_STATUS_NO));
    }

    public static String itemSummary(ItemStack item) {
        if (item == null || item.isEmpty()) {
            // The configured value is rendered as a plain string inside an already-coloured
            // line, so strip any leading hex codes the operator may have set.
            String empty = TGPlatform.getInstance().getMessageService().getString(MessagesKeys.GUI_STATUS_EMPTY);
            return empty.replaceAll("[&§]#[A-Fa-f0-9]{6}", "");
        }
        return item.getType().getName().getKey() + " x" + item.getAmount();
    }
}
