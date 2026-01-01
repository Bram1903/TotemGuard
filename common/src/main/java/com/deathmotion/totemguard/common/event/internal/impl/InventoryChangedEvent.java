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

package com.deathmotion.totemguard.common.event.internal.impl;/*
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

import com.deathmotion.totemguard.common.event.internal.InternalPlayerEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fired when a player's inventory changes in a single packet update.
 * <p>
 * Contains all inventory items updated during this packet update.
 */
@Getter
public class InventoryChangedEvent extends InternalPlayerEvent {

    /**
     * The carried item updated by this packet, or {@code null} if unchanged.
     */
    private final @Nullable CarriedItem updatedCarriedItem;

    /**
     * All inventory slots updated by this packet.
     */
    private final List<InventorySlot> changedSlots;

    /**
     * The source that issued this inventory update.
     */
    private final Issuer lastIssuer;

    /**
     * @param player             the player whose inventory changed
     * @param updatedCarriedItem carried item updated by this packet, or {@code null}
     * @param changedSlots       inventory slots updated by this packet
     * @param lastIssuer         issuer of the update
     */
    public InventoryChangedEvent(
            TGPlayer player,
            @Nullable CarriedItem updatedCarriedItem,
            List<InventorySlot> changedSlots,
            Issuer lastIssuer
    ) {
        super(player);
        this.updatedCarriedItem = updatedCarriedItem;
        this.changedSlots = changedSlots;
        this.lastIssuer = lastIssuer;
    }
}

