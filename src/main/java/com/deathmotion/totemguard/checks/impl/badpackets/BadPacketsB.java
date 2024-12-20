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
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class BadPacketsB extends Check {
    private static BadPacketsB instance;
    private final TotemGuard plugin;
    private final MessageService messageService;

    private BadPacketsB(TotemGuard plugin) {
        super(plugin, "BadPacketsB", "Suspicious client brand");
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
    }

    public static BadPacketsB getInstance(TotemGuard plugin) {
        if (instance == null) {
            instance = new BadPacketsB(plugin);
        }
        return instance;
    }

    public void check(Player player, String clientBrand) {
        final var settings = plugin.getConfigManager().getChecks().getBadPacketB();

        if (settings.getBannedClientBrands().contains(clientBrand.toLowerCase())) {
            flag(player, createDetails(clientBrand), plugin.getConfigManager().getChecks().getBadPacketB());
        }
    }

    private Component createDetails(String clientBrand) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("Client Brand: ", colorScheme.getY()))
                .append(Component.text(clientBrand, colorScheme.getX()))
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
