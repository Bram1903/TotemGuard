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

package com.deathmotion.totemguard.common.event.internal;

import com.deathmotion.totemguard.api.event.EventPriority;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.channel.ChannelBase;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;

public final class InventoryChangedChannel extends ChannelBase<InventoryChangedChannel.Handler> {

    public void register(@NotNull Object owner, @NotNull Handler handler) {
        addEntry(handler, EventPriority.NORMAL, false, owner);
    }

    public void fire(@NotNull TGPlayer player, @Nullable CarriedItem updatedCarriedItem,
                     @NotNull List<InventorySlot> changedSlots, @NotNull Issuer lastIssuer) {
        Entry<Handler>[] arr = entries();
        if (arr.length == 0) return;
        for (Entry<Handler> e : arr) {
            try {
                e.handler.onInventoryChanged(player, updatedCarriedItem, changedSlots, lastIssuer);
            } catch (Throwable t) {
                TGPlatform platform = TGPlatform.getInstance();
                if (platform != null) {
                    platform.getLogger().log(Level.WARNING, "Internal InventoryChanged handler threw", t);
                }
            }
        }
    }

    @FunctionalInterface
    public interface Handler {
        void onInventoryChanged(@NotNull TGPlayer player,
                                @Nullable CarriedItem updatedCarriedItem,
                                @NotNull List<InventorySlot> changedSlots,
                                @NotNull Issuer lastIssuer);
    }
}
