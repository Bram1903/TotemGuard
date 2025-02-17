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

package com.deathmotion.totemguard.checks.impl.misc;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.type.PacketCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

// This class has mostly been copied from https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/checks/impl/misc/ClientBrand.java
public class ClientBrand extends Check implements PacketCheck {
    @Getter
    private String brand = "vanilla";
    private boolean hasBrand = false;

    public ClientBrand(TotemPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handle(packet.getChannelName(), packet.getData());
        } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handle(packet.getChannelName(), packet.getData());
        }
    }

    private void handle(String channel, byte[] data) {
        final String expectedChannel = player.user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) ? "minecraft:brand" : "MC|Brand";
        if (!channel.equals(expectedChannel)) return;

        if (data.length > 64 || data.length == 0) {
            brand = "sent " + data.length + " bytes as brand";
        } else if (!hasBrand) {
            byte[] minusLength = new byte[data.length - 1];
            System.arraycopy(data, 1, minusLength, 0, minusLength.length);

            brand = new String(minusLength).replace(" (Velocity)", ""); //removes velocity's brand suffix
            brand = ChatColor.stripColor(brand); //strip color codes from client brand

            announceBrand();
        }

        hasBrand = true;
    }

    private void announceBrand() {
        if (!settings.isAnnounceClientBrand()) return;

        String alertBrandTemplate = TotemGuard.getInstance().getConfigManager().getMessages().getAlertBrand();

        Component brandAlert = TotemGuard.getInstance().getMessengerService().format(
                alertBrandTemplate
                        .replace("%prefix%", messages.getPrefix())
                        .replace("%player%", player.getName())
                        .replace("%client_brand%", brand)
        );

        // sendMessage is async safe while broadcast isn't due to adventure
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("TotemGuard.Brand")) {
                onlinePlayer.sendMessage(brandAlert);
            }
        }
    }
}
