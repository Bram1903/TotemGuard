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
        return Component.text(label + ": ", Palette.LABEL)
                .append(Component.text(value ? "Yes" : "No", value ? Palette.SUCCESS : Palette.DANGER));
    }

    public static String itemSummary(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return "Empty";
        }
        return item.getType().getName().getKey() + " x" + item.getAmount();
    }
}
