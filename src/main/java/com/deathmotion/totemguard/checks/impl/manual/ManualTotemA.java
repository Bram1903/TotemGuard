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

package com.deathmotion.totemguard.checks.impl.manual;

import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.GenericCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

@CheckData(name = "ManualTotemA", description = "Manual totem removal")
public class ManualTotemA extends Check implements GenericCheck {

    public ManualTotemA(final TotemPlayer player) {
        super(player);
    }

    public void handle(CommandSender sender, long elapsedMs, long checkTime) {
        fail(getCheckDetails(sender, elapsedMs, checkTime));
    }

    private Component getCheckDetails(CommandSender sender, long elapsedMs, long checkTime) {
        return Component.text()
                .append(Component.text("Staff: ", color.getX()))
                .append(Component.text(sender.getName(), color.getY()))
                .append(Component.newline())
                .append(Component.text("Elapsed Time: ", color.getX()))
                .append(Component.text(elapsedMs + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Max Check Duration: ", color.getX()))
                .append(Component.text(checkTime + "ms", color.getY()))
                .build();
    }
}
