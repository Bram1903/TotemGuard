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

package com.deathmotion.totemguard.checks.impl.misc;

import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.type.PacketCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ClientBrand extends Check implements PacketCheck {
    @Getter
    private String brand = "vanilla";
    private boolean hasBrand = false;

    public ClientBrand(TotemPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;
        WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
        handle(packet.getChannelName(), packet.getData());
    }

    public void handle(String channel, byte[] data) {
        if (channel.equalsIgnoreCase("minecraft:brand") || channel.equals("MC|Brand")) {
            if (data.length > 64 || data.length == 0) {
                brand = "sent " + data.length + " bytes as brand";
            } else if (!hasBrand) {
                byte[] minusLength = new byte[data.length - 1];
                System.arraycopy(data, 1, minusLength, 0, minusLength.length);

                brand = new String(minusLength).replace(" (Velocity)", ""); //removes velocity's brand suffix
                brand = ChatColor.stripColor(brand); //strip color codes from client brand

                // TODO: Implement this method
                //announceBrand(player.bukkitPlayer, brand);
            }

            hasBrand = true;
        }
    }

    private void announceBrand(Player player, String brand) {
        // sendMessage is async safe while broadcast isn't due to adventure
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("TotemGuard.Alerts")) {
                // Finish sending the message to online players
            }
        }
    }
}
