/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.bukkit.network;

import com.deathmotion.totemguard.bukkit.player.BukkitPlatformPlayer;
import com.deathmotion.totemguard.common.TGPlatform;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class BungeeChannelManager implements PluginMessageListener, Listener {

    private final Plugin plugin;
    private final TGPlatform platform;
    private final AtomicReference<@Nullable String> proxyServerName = new AtomicReference<>();
    private final AtomicReference<@Nullable Set<String>> proxyServerSet = new AtomicReference<>();

    public BungeeChannelManager(Plugin plugin, TGPlatform platform) {
        this.plugin = plugin;
        this.platform = platform;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, BukkitPlatformPlayer.BUNGEE_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, BukkitPlatformPlayer.BUNGEE_CHANNEL, this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier != null) {
            sendSubchannel(carrier, "GetServer");
            sendSubchannel(carrier, "GetServers");
        }
    }

    public @Nullable String proxyServerName() {
        return proxyServerName.get();
    }

    public boolean isServerOnThisProxy(@NotNull String serverName) {
        Set<String> set = proxyServerSet.get();
        if (set == null) return true;
        for (String name : set) {
            if (name.equalsIgnoreCase(serverName)) return true;
        }
        return false;
    }

    public @NotNull String resolveServerName(@NotNull String serverName) {
        Set<String> set = proxyServerSet.get();
        if (set == null) return serverName;
        for (String name : set) {
            if (name.equalsIgnoreCase(serverName)) return name;
        }
        return serverName;
    }

    public @Nullable String resolveProxyServerId(@NotNull String serverName) {
        Set<String> set = proxyServerSet.get();
        if (set == null) return null;
        for (String name : set) {
            if (name.equalsIgnoreCase(serverName)) return name;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (proxyServerName.get() != null && proxyServerSet.get() != null) return;
        Player carrier = event.getPlayer();
        if (proxyServerName.get() == null) sendSubchannel(carrier, "GetServer");
        if (proxyServerSet.get() == null) sendSubchannel(carrier, "GetServers");
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!BukkitPlatformPlayer.BUNGEE_CHANNEL.equals(channel)) return;
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String sub = in.readUTF();
            switch (sub) {
                case "GetServer" -> {
                    String name = in.readUTF();
                    if (!name.isEmpty() && !name.equals(proxyServerName.get())) {
                        proxyServerName.set(name);
                        platform.getLogger().info("Proxy reports this server's name as \"" + name + "\".");
                        if (platform.getNetworkPresenceRepository() != null) {
                            platform.getNetworkPresenceRepository().updateEffectiveDisplayName(name);
                        }
                    }
                }
                case "GetServers" -> {
                    String csv = in.readUTF();
                    Set<String> set = new LinkedHashSet<>(Arrays.asList(csv.split(",\\s*")));
                    proxyServerSet.set(Collections.unmodifiableSet(set));
                }
                default -> {
                }
            }
        } catch (Exception ex) {
            platform.getLogger().warning("Failed to parse BungeeCord plugin message: " + ex.getMessage());
        }
    }

    private void sendSubchannel(Player carrier, String subchannel) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        byte[] payload = out.toByteArray();
        FoliaScheduler.getEntityScheduler().run(carrier, plugin, (o) -> {
            if (!carrier.isOnline()) return;
            carrier.sendPluginMessage(plugin, BukkitPlatformPlayer.BUNGEE_CHANNEL, payload);
        }, null);
    }
}
