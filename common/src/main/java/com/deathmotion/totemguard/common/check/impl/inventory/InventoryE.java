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

package com.deathmotion.totemguard.common.check.impl.inventory;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;

@CheckData(description = "Estimated movement with open inventory", type = CheckType.INVENTORY, experimental = true)
public class InventoryE extends CheckImpl implements PacketCheck {

    private boolean movementFlagged;

    public InventoryE(TGPlayer player) {
        super(player);
    }

    public void validateMovement() {
        if (!data.isOpenInventory()) {
            movementFlagged = false;
            return;
        }
        if (data.isServerOpenedInventoryThisTick()) return;

        if (data.isInVehicle()) {
            movementFlagged = false;
            return;
        }

        if (!data.getMovementEstimator().movedHorizontally()) {
            movementFlagged = false;
            return;
        }
        if (movementFlagged) return;

        movementFlagged = true;
        fail("movement");
    }
}
