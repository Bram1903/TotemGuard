/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.checks.impl.badpackets;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class BadPacketsB extends Check {

    @Getter
    private static BadPacketsB instance;
    private final TotemGuard plugin;

    private BadPacketsB(TotemGuard plugin) {
        super(plugin, "BadPacketsB", "Suspicious client brand");
        this.plugin = plugin;
    }

    public static void init(TotemGuard plugin) {
        if (instance == null) {
            instance = new BadPacketsB(plugin);
        }
    }

    public void check(Player player, String clientBrand) {
        final var settings = plugin.getConfigManager().getSettings().getChecks().getBadPacketsB();

        if (settings.getBannedClientBrands().contains(clientBrand.toLowerCase())) {
            flag(player, createDetails(clientBrand), plugin.getConfigManager().getSettings().getChecks().getBadPacketsB());
        }
    }

    private Component createDetails(String clientBrand) {
        return Component.text()
                .append(Component.text("Client Brand: ", NamedTextColor.GRAY))
                .append(Component.text(clientBrand, NamedTextColor.GOLD))
                .build();

    }

    @Override
    public void resetData() {
        super.resetData();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
    }
}
