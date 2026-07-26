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

package com.deathmotion.totemguard.common.replay.viewer.hud;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public enum ViewerControl {

    PHASE(0, ItemTypes.COMPASS),
    CAMERA(2, ItemTypes.ENDER_EYE),
    REWIND(3, ItemTypes.SPECTRAL_ARROW),
    PLAY_PAUSE(4, ItemTypes.CLOCK),
    FORWARD(5, ItemTypes.ARROW),
    SPEED(6, ItemTypes.SUGAR),
    RESTART(7, ItemTypes.TOTEM_OF_UNDYING),
    LEAVE(8, ItemTypes.BARRIER);

    private static final ViewerControl[] BY_SLOT = bySlot();

    private final int slot;
    private final ItemType item;

    ViewerControl(int slot, ItemType item) {
        this.slot = slot;
        this.item = item;
    }

    private static ViewerControl[] bySlot() {
        ViewerControl[] slots = new ViewerControl[9];
        for (ViewerControl control : values()) slots[control.slot] = control;
        return slots;
    }

    public static @Nullable ViewerControl at(int slot) {
        return slot < 0 || slot >= BY_SLOT.length ? null : BY_SLOT[slot];
    }
}
