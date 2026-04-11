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

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class GuiRenderResult {

    private final Component title;
    private final int rows;
    private final ItemStack[] items;
    private final GuiClickAction[] clickActions;

    private GuiRenderResult(Component title, int rows, ItemStack[] items, GuiClickAction[] clickActions) {
        this.title = title;
        this.rows = rows;
        this.items = items;
        this.clickActions = clickActions;
    }

    public static Builder builder(int rows, Component title) {
        return new Builder(rows, title);
    }

    public Component title() {
        return title;
    }

    public int rows() {
        return rows;
    }

    public int size() {
        return items.length;
    }

    public ItemStack item(int slot) {
        return items[slot];
    }

    public GuiClickAction clickAction(int slot) {
        return clickActions[slot];
    }

    public List<ItemStack> itemList() {
        return Arrays.asList(items.clone());
    }

    public static final class Builder {

        private final Component title;
        private final int rows;
        private final ItemStack[] items;
        private final GuiClickAction[] clickActions;

        private Builder(int rows, Component title) {
            if (rows < 1 || rows > 6) {
                throw new IllegalArgumentException("GUI rows must be between 1 and 6, found " + rows);
            }

            this.title = Objects.requireNonNull(title, "title");
            this.rows = rows;
            this.items = new ItemStack[rows * 9];
            this.clickActions = new GuiClickAction[rows * 9];

            Arrays.fill(this.items, ItemStack.EMPTY);
        }

        public Builder set(int slot, ItemStack item) {
            validateSlot(slot);
            this.items[slot] = normalizeItem(item);
            return this;
        }

        public Builder set(int slot, ItemStack item, GuiClickAction action) {
            return this.set(slot, item).action(slot, action);
        }

        public Builder action(int slot, GuiClickAction action) {
            validateSlot(slot);
            this.clickActions[slot] = action;
            return this;
        }

        public Builder fillEmpty(ItemStack item) {
            ItemStack normalized = normalizeItem(item);
            for (int slot = 0; slot < items.length; slot++) {
                if (items[slot].isEmpty()) {
                    items[slot] = normalized.copy();
                }
            }
            return this;
        }

        public GuiRenderResult build() {
            return new GuiRenderResult(
                    title,
                    rows,
                    items.clone(),
                    clickActions.clone()
            );
        }

        private void validateSlot(int slot) {
            if (slot < 0 || slot >= items.length) {
                throw new IllegalArgumentException("Slot " + slot + " is outside GUI bounds 0.." + (items.length - 1));
            }
        }

        private ItemStack normalizeItem(ItemStack item) {
            if (item == null || item.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return item.copy();
        }
    }
}
