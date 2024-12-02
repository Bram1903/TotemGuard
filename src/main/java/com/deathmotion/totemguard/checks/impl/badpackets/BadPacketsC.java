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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSlotStateChange;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.HashMap;
import java.util.UUID;

public final class BadPacketsC extends Check implements PacketListener {

    private final TotemGuard plugin;
    private final MessageService messageService;
    private final HashMap<UUID, Integer> playerlastSlotmap;

    public BadPacketsC(TotemGuard plugin) {
        super(plugin, "BadPacketsC", "Impossible same slot packet");
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.playerlastSlotmap = new HashMap<>();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.SLOT_STATE_CHANGE) {
            return;
        }

        WrapperPlayClientSlotStateChange packet = new WrapperPlayClientSlotStateChange(event);
        int slot = packet.getSlot();
        UUID playerUUID = event.getUser().getUUID();

        Integer lastSlot = playerlastSlotmap.get(playerUUID);
        if (lastSlot != null && lastSlot == slot) {
            final Settings.Checks.BadPacketsC settings = plugin.getConfigManager().getSettings().getChecks().getBadPacketsC();
            flag(event.getPlayer(), getCheckDetails(slot, lastSlot), settings);
        }

        playerlastSlotmap.put(playerUUID, slot);
    }

    @Override
    public void resetData() {
        super.resetData();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
    }

    private Component getCheckDetails(int slot, int lastSlot) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("slot: ", colorScheme.getY()))
                .append(Component.text(slot, colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("lastSlot: ", colorScheme.getY()))
                .append(Component.text(lastSlot, colorScheme.getX()))
                .build();
    }
}
