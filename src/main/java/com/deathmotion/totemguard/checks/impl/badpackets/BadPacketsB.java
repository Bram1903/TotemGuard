/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.checks.impl.badpackets;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.PacketCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import net.kyori.adventure.text.Component;

@CheckData(name = "BadPacketsB", description = "Suspicious client brand")
public class BadPacketsB extends Check implements PacketCheck {

    public BadPacketsB(final TotemPlayer player) {
        super(player);
    }

    public void handle(String clientBrand) {
        if (TotemGuard.getInstance().getConfigManager().getChecks().getBadPacketsB().getBannedBrands().contains(clientBrand.toLowerCase())) {
            fail(createDetails(clientBrand));
        }
    }

    private Component createDetails(String clientBrand) {
        return Component.text()
                .append(Component.text("Client Brand: ", color.getX()))
                .append(Component.text(clientBrand, color.getY()))
                .build();
    }
}
