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

package com.deathmotion.totemguard.checks.impl.autototem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.PacketCheck;
import com.deathmotion.totemguard.config.Checks;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.models.impl.DigAndPickupState;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.kyori.adventure.text.Component;

@CheckData(name = "AutoTotemD", description = "Suspicious re-totem packet sequence")
public class AutoTotemD extends Check implements PacketCheck {

    private static final long EXPECTED_AVERAGE_TIME = 50; // Expected average time in ms
    private static final long ACCEPTABLE_VARIATION = 20; // Allowable deviation in ms (e.g., ±20ms)

    public AutoTotemD(final TotemPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Handle Digging Packet
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            // Make sure the player has used a totem before
            if (player.totemData.getLastTotemUsage() == null) return;

            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                handleDiggingPacket();
            }
        }

        // Handle Pick Item Packet
        if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM) {
            handlePickItemPacket();
        }
    }

    // Handle the Digging Packet (we expect this to be called twice)
    private void handleDiggingPacket() {
        long currentTime = System.currentTimeMillis();

        if (player.digAndPickupState.sequence == 0 || player.digAndPickupState.sequence == 2) {
            player.digAndPickupState.sequence++;
            player.digAndPickupState.lastDiggingPacketTime = currentTime;
        }

        if (player.digAndPickupState.sequence == 3) {
            // Third Digging Packet → run the validation
            long totemPopTime = player.totemData.getLastTotemUsage();
            long totalPacketTime = currentTime - totemPopTime; // Total time from totem use to last packet
            long averageTimePerPacket = totalPacketTime / 3; // Average time per packet

            long timeToFirstDigging = player.digAndPickupState.firstPacketTime - totemPopTime;
            long timeToPickItem = player.digAndPickupState.timeToPickItem;
            long timeFromPickToLastDigging = currentTime - player.digAndPickupState.pickItemPacketTime;

            final Checks.AutoTotemD settings = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemD();

            // Check if the average time per packet is within the expected range
            if (isWithinExpectedRange(averageTimePerPacket)) {
                fail(createComponent(totalPacketTime, averageTimePerPacket, timeToFirstDigging, timeToPickItem, timeFromPickToLastDigging));
            }

            // Reset state after validation
            player.digAndPickupState = new DigAndPickupState();
        } else {
            // Update the state and store it
            if (player.digAndPickupState.sequence == 1) {
                player.digAndPickupState.firstPacketTime = currentTime;
            }
        }
    }

    // Handle the PickItem Packet (this should be called once)
    private void handlePickItemPacket() {
        long currentTime = System.currentTimeMillis();

        if (player.digAndPickupState.sequence == 1) {
            player.digAndPickupState.sequence++;
            player.digAndPickupState.pickItemPacketTime = currentTime;
            player.digAndPickupState.timeToPickItem = currentTime - player.digAndPickupState.firstPacketTime;
        }
    }

    // Helper method to check if a time difference is within the expected range
    private boolean isWithinExpectedRange(long averageTime) {
        return Math.abs(averageTime - EXPECTED_AVERAGE_TIME) <= ACCEPTABLE_VARIATION;
    }

    private Component createComponent(long totalPacketTime, long averageTimePerPacket, long timeToFirstDigging, long timeToPickItem, long timeFromPickToLastDigging) {
        return Component.text()
                .append(Component.text("Total time: ", color.getX()))
                .append(Component.text(totalPacketTime + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Average time per packet: ", color.getX()))
                .append(Component.text(averageTimePerPacket + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Time to first swap packet: ", color.getX()))
                .append(Component.text(timeToFirstDigging + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Time from swap -> pick up: ", color.getX()))
                .append(Component.text(timeToPickItem + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Time from pick up -> digging: ", color.getX()))
                .append(Component.text(timeFromPickToLastDigging + "ms", color.getY()))
                .build();
    }
}
