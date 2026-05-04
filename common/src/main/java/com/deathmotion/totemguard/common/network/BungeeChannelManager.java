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

package com.deathmotion.totemguard.common.network;

import com.deathmotion.totemguard.common.TGPlatform;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class BungeeChannelManager extends PacketListenerAbstract {

    private static final String LEGACY_CHANNEL = "BungeeCord";
    private static final String MODERN_CHANNEL = "bungeecord:main";

    private final TGPlatform platform;
    private final AtomicReference<@Nullable String> proxyServerName = new AtomicReference<>();
    private final AtomicReference<@Nullable Set<String>> proxyServerSet = new AtomicReference<>();

    public BungeeChannelManager(TGPlatform platform) {
        this.platform = platform;
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

    public void routeToProxyServer(@NotNull User user, @NotNull String targetServerName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(targetServerName);
        sendBungeePayload(user, out.toByteArray());
    }

    public void refresh() {
        proxyServerName.set(null);
        proxyServerSet.set(null);
        User carrier = pickCarrier();
        if (carrier != null) queryProxy(carrier);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.JOIN_GAME) return;
        if (proxyServerName.get() != null && proxyServerSet.get() != null) return;

        User user = event.getUser();
        event.getTasksAfterSend().add(() -> {
            if (user.getConnectionState() == ConnectionState.PLAY) queryProxy(user);
        });
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

        WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
        String channel = wrapper.getChannelName();
        if (!LEGACY_CHANNEL.equals(channel) && !MODERN_CHANNEL.equals(channel)) return;

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(wrapper.getData());
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

    private void queryProxy(@NotNull User user) {
        if (proxyServerName.get() == null) sendBungeeSubchannel(user, "GetServer");
        if (proxyServerSet.get() == null) sendBungeeSubchannel(user, "GetServers");
    }

    private void sendBungeeSubchannel(@NotNull User user, @NotNull String subchannel) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        sendBungeePayload(user, out.toByteArray());
    }

    private void sendBungeePayload(@NotNull User user, byte @NotNull [] payload) {
        user.sendPacket(new WrapperPlayServerPluginMessage(MODERN_CHANNEL, payload));
    }

    private @Nullable User pickCarrier() {
        for (User user : PacketEvents.getAPI().getProtocolManager().getUsers()) {
            if (user.getConnectionState() == ConnectionState.PLAY) return user;
        }
        return null;
    }
}
