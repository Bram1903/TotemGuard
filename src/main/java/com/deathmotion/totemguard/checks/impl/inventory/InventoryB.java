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
import com.deathmotion.totemguard.config.impl.Checks;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryB extends Check implements PacketListener {

    private final TotemGuard plugin;
    private final MessageService messageService;
    private final ConcurrentHashMap<UUID, Long> inventoryClick;

    public InventoryB(TotemGuard plugin) {
        super(plugin, "InventoryB", "Actions with open inventory", true);
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.inventoryClick = new ConcurrentHashMap<>();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {

            Player player = event.getPlayer();

            if (!inventoryClick.containsKey(player.getUniqueId())) return;
            long storedTime = inventoryClick.get(player.getUniqueId());
            inventoryClick.remove(player.getUniqueId());
            String action = String.valueOf(event.getPacketType());
            check(player, storedTime, action);
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            inventoryClick.remove(event.getUser().getUUID());
        }
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            inventoryClick.put(event.getUser().getUUID(), System.currentTimeMillis());
        }

    }

    @Override
    public void resetData() {
        super.resetData();
        inventoryClick.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
        inventoryClick.remove(uuid);

    }

    private void check(Player player, long storedTime, String action) {
        long timeDifference = Math.abs(System.currentTimeMillis() - storedTime);

        final Checks.InventoryB settings = plugin.getConfigManager().getChecks().getInventoryB();
        if (timeDifference <= 1000) {
            flag(player, getCheckDetails(action, timeDifference), settings);
        }
    }

    private Component getCheckDetails(String action, long timeDifference) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("Action: ", colorScheme.getY()))
                .append(Component.text(action, colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Difference: ", colorScheme.getY()))
                .append(Component.text(timeDifference, colorScheme.getX()))
                .append(Component.text("ms", colorScheme.getX()))
                .build();
    }
}