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
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BadPacketsC extends Check implements PacketListener {

    private final TotemGuard plugin;
    private final MessageService messageService;
    private final ConcurrentHashMap<UUID, Integer> playerLastSlotMap;

    public BadPacketsC(TotemGuard plugin) {
        super(plugin, "BadPacketsC", "Impossible same slot packet");
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.playerLastSlotMap = new ConcurrentHashMap<>();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.HELD_ITEM_CHANGE) {
            return;
        }

        WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(event);
        int slot = packet.getSlot();
        UUID playerUUID = event.getUser().getUUID();

        // Set default value to an impossible slot to prevent a rare false positive on the first slot change
        int lastSlot = playerLastSlotMap.computeIfAbsent(playerUUID, k -> -69);
        if (lastSlot == slot) {
            final Settings.Checks.BadPacketsC settings = plugin.getConfigManager().getSettings().getChecks().getBadPacketsC();
            flag(event.getPlayer(), getCheckDetails(slot, lastSlot), settings);
        }

        playerLastSlotMap.put(playerUUID, slot);
    }

    @Override
    public void resetData() {
        super.resetData();
        playerLastSlotMap.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
        playerLastSlotMap.remove(uuid);
    }

    private Component getCheckDetails(int slot, int lastSlot) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("New Slot Change: ", colorScheme.getY()))
                .append(Component.text(slot, colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Last Slot Change: ", colorScheme.getY()))
                .append(Component.text(lastSlot, colorScheme.getX()))
                .build();
    }
}
