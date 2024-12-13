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

package com.deathmotion.totemguard.checks.impl.inventory;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.models.PlayerState;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.packetlisteners.UserTracker;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InventoryA extends Check implements PacketListener {

    private final TotemGuard plugin;
    private final UserTracker userTracker;
    private final MessageService messageService;

    public InventoryA(TotemGuard plugin) {
        super(plugin, "InventoryA", "Invalid action with open inventory", true);
        this.plugin = plugin;
        this.userTracker = plugin.getUserTracker();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            Player player = event.getPlayer();

            Optional<TotemPlayer> optionalTotemPlayer = userTracker.getTotemPlayer(player.getUniqueId());
            if (optionalTotemPlayer.isEmpty()) return;

            TotemPlayer totemPlayer = optionalTotemPlayer.get();
            PlayerState playerState = totemPlayer.playerState();

            if (playerState.isSprinting() || playerState.isSneaking()) {
                final Settings.Checks.InventoryA settings = plugin.getConfigManager().getSettings().getChecks().getInventoryA();
                flag(player, getCheckDetails(playerState), settings);
            }
        }
    }

    private Component getCheckDetails(PlayerState playerState) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        List<String> activeStates = new ArrayList<>();
        if (playerState.isSprinting()) {
            activeStates.add("Sprinting");
        }
        if (playerState.isSneaking()) {
            activeStates.add("Sneaking");
        }

        if (activeStates.isEmpty()) {
            return Component.empty();
        }

        return Component.text()
                .append(Component.text("States: ", colorScheme.getY()))
                .append(Component.text(String.join(", ", activeStates), colorScheme.getX()))
                .build();
    }
}
