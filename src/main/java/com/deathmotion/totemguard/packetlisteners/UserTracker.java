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

package com.deathmotion.totemguard.packetlisteners;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsB;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserTracker implements PacketListener {
    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, TotemPlayer> totemPlayers = new ConcurrentHashMap<>();

    public UserTracker(TotemGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        User user = event.getUser();

        UUID userUUID = user.getUUID();
        if (userUUID == null) return;

        Player player = (Player) event.getPlayer();

        if (player.hasPermission("TotemGuard.Alerts")) {
            plugin.getAlertManager().enableAlerts(player);
        }

        String playerBrand = totemPlayers.get(userUUID).getClientBrandName();
        announceClientBrand(player.getName(), playerBrand);
        BadPacketsB.getInstance().check(player, playerBrand);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE && event.getPacketType() != PacketType.Configuration.Client.PLUGIN_MESSAGE)
            return;

        WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);

        String channelName = packet.getChannelName();
        if (!channelName.equalsIgnoreCase("minecraft:brand") && !channelName.equals("MC|Brand")) return;

        byte[] data = packet.getData();
        String brand = "Unknown";
        if (data.length <= 64 && data.length > 0) { // Check if the data length is valid
            byte[] minusLength = new byte[data.length - 1];
            System.arraycopy(data, 1, minusLength, 0, minusLength.length);
            brand = new String(minusLength).replace(" (Velocity)", "");
            if (brand.isEmpty()) {
                brand = "Unknown";
            }
        }

        User user = event.getUser();
        UUID userUUID = user.getUUID();

        TotemPlayer totemPlayer = new TotemPlayer();
        totemPlayer.setUuid(userUUID);
        totemPlayer.setUsername(user.getName());
        totemPlayer.setClientBrandName(brand);
        totemPlayer.setClientVersion(user.getClientVersion());
        totemPlayer.setBedrockPlayer(userUUID.getMostSignificantBits() == 0L);

        totemPlayers.put(userUUID, totemPlayer);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        UUID userUUID = event.getUser().getUUID();
        if (userUUID == null) return;

        plugin.debug("Removing data for: " + userUUID);

        plugin.getAlertManager().removePlayer(userUUID);
        plugin.getCheckManager().resetData(userUUID);
        totemPlayers.remove(userUUID);
    }

    private void announceClientBrand(String username, String brand) {
        final Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.isAlertClientBrand()) return;

        Component message = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text(username, NamedTextColor.GOLD))
                .append(Component.text(" joined using: ", NamedTextColor.GRAY))
                .append(Component.text(brand, NamedTextColor.GOLD))
                .build();

        Bukkit.broadcast(message, "TotemGuard.Alerts");
    }

    public Optional<TotemPlayer> getTotemPlayer(UUID uuid) {
        return Optional.ofNullable(totemPlayers.get(uuid));
    }

    public void clearTotemData() {
        totemPlayers.values().forEach(x -> x.getTotemData().clear());
    }
}
