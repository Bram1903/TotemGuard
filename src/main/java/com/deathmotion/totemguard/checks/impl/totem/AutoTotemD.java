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

package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Settings;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoTotemD extends Check implements PacketListener, Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, Long> totemUsage;
    private final ConcurrentHashMap<UUID, PacketState> playerPacketState;

    private static final long EXPECTED_AVERAGE_TIME = 50; // Expected average time in ms
    private static final long ACCEPTABLE_VARIATION = 20; // Allowable deviation in ms (e.g., ±20ms)

    public AutoTotemD(TotemGuard plugin) {
        super(plugin, "AutoTotemD", "Suspicious re-totem packet sequence", true);

        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        this.totemUsage = new ConcurrentHashMap<>();
        this.playerPacketState = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Record the time of the totem usage
        long totemPopTime = System.currentTimeMillis();
        totemUsage.put(player.getUniqueId(), totemPopTime);
        playerPacketState.remove(player.getUniqueId());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Handle Digging Packet
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            Player player = (Player) event.getPlayer();
            // Check if a totem has recently been used
            if (!totemUsage.containsKey(player.getUniqueId())) return;

            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                handleDiggingPacket(player, System.currentTimeMillis());
            }
        }

        // Handle Pick Item Packet
        if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM) {
            handlePickItemPacket((Player) event.getPlayer(), System.currentTimeMillis());
        }
    }

    // Handle the Digging Packet (we expect this to be called twice)
    private void handleDiggingPacket(Player player, long currentTime) {
        UUID playerId = player.getUniqueId();
        PacketState state = playerPacketState.getOrDefault(playerId, new PacketState());

        if (state.sequence == 0 || state.sequence == 2) {
            state.sequence++;
            state.lastDiggingPacketTime = currentTime;
        }

        if (state.sequence == 3) {
            // Third Digging Packet → run the validation
            long totemPopTime = totemUsage.getOrDefault(playerId, 0L);
            long totalPacketTime = currentTime - totemPopTime; // Total time from totem use to last packet
            long averageTimePerPacket = totalPacketTime / 3; // Average time per packet

            long timeToFirstDigging = state.firstPacketTime - totemPopTime;
            long timeToPickItem = state.timeToPickItem;
            long timeFromPickToLastDigging = currentTime - state.pickItemPacketTime;

            final Settings.Checks.AutoTotemC settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemC();

            // Check if the average time per packet is within the expected range
            if (isWithinExpectedRange(averageTimePerPacket)) {
                // Show individual packet timings as well as the average time
                Component details = Component.text()
                        .append(Component.text("Total time: ", NamedTextColor.GRAY))
                        .append(Component.text(totalPacketTime + "ms", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("Average time per packet: ", NamedTextColor.GRAY))
                        .append(Component.text(averageTimePerPacket + "ms", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("Time to first swap packet: ", NamedTextColor.GRAY))
                        .append(Component.text(timeToFirstDigging + "ms", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("Time from swap -> pick up: ", NamedTextColor.GRAY))
                        .append(Component.text(timeToPickItem + "ms", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("Time from pick up -> digging: ", NamedTextColor.GRAY))
                        .append(Component.text(timeFromPickToLastDigging + "ms", NamedTextColor.GOLD))
                        .build();

                flag(player, details, settings);
            }

            // Reset state after validation
            playerPacketState.remove(playerId);
        } else {
            // Update the state and store it
            if (state.sequence == 1) {
                state.firstPacketTime = currentTime;
            }
            playerPacketState.put(playerId, state);
        }
    }

    // Handle the PickItem Packet (this should be called once)
    private void handlePickItemPacket(Player player, long currentTime) {
        UUID playerId = player.getUniqueId();
        PacketState state = playerPacketState.getOrDefault(playerId, new PacketState());

        if (state.sequence == 1) {
            state.sequence++;
            state.pickItemPacketTime = currentTime;
            state.timeToPickItem = currentTime - state.firstPacketTime;

            // Update the state
            playerPacketState.put(playerId, state);
        }
    }

    // Helper method to check if a time difference is within the expected range
    private boolean isWithinExpectedRange(long averageTime) {
        return Math.abs(averageTime - EXPECTED_AVERAGE_TIME) <= ACCEPTABLE_VARIATION;
    }

    @Override
    public void resetData() {
        totemUsage.clear();
        playerPacketState.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        totemUsage.remove(uuid);
        playerPacketState.remove(uuid);
    }

    // Inner class to store packet states for each player
    private static class PacketState {
        int sequence = 0; // Sequence: 0 (none), 1 (Digging), 2 (PickItem), 3 (Digging)
        long firstPacketTime = 0;
        long lastDiggingPacketTime = 0;
        long pickItemPacketTime = 0;
        long timeToPickItem = 0;
    }
}