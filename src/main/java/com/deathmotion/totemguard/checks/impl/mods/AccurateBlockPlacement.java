/*
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

package com.deathmotion.totemguard.checks.impl.mods;

import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.ModCheck;
import com.deathmotion.totemguard.models.TotemPlayer;

import java.util.List;

@CheckData(name = "AccurateBlockPlacement", description = "Usage of the Accurate Block Placement mod")
public class AccurateBlockPlacement extends ModCheck {
    public AccurateBlockPlacement(TotemPlayer player) {
        super(player, List.of(
                "net.clayborn.accurateblockplacement.togglevanillaplacement",
                "net.clayborn.accurateblockplacement.togglefastbreaking"
        ));
    }
}
