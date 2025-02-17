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

package com.deathmotion.totemguard.messaging.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.manager.AlertManagerImpl;
import com.deathmotion.totemguard.messaging.ProxyAlertMessenger;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class PluginMessageProxyMessenger extends PacketListenerAbstract implements ProxyAlertMessenger {
    private static final String BUNGEECORD_CHANNEL = "BungeeCord";
    private static final String BUNGEECORD_CHANNEL_ALT = "bungeecord:main";

    private final @NotNull TotemGuard plugin;
    private final @NotNull AlertManagerImpl alertManager;

    private boolean proxyEnabled;
    private String messageChannel;

    public PluginMessageProxyMessenger(@NotNull TotemGuard plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();
    }

    public String readRawAlert(byte[] data) {
        ByteArrayDataInput input = ByteStreams.newDataInput(data);
        if (!messageChannel.equals(input.readUTF())) return null;

        byte[] messageBytes = new byte[input.readShort()];
        input.readFully(messageBytes);

        try (DataInputStream stream = new DataInputStream(new ByteArrayInputStream(messageBytes))) {
            return stream.readUTF();
        } catch (IOException e) {
            TotemGuard.getInstance().getLogger().severe("Failed to read forwarded alert from another server.");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void start() {
        // TODO(Any): Maybe we should check if we are successfully connected to a proxy?
        // This currently only checks if it is enabled.
        this.proxyEnabled = isProxyEnabled();
        if (proxyEnabled) {
            // Register incoming listener and outgoing plugin channel
            PacketEvents.getAPI().getEventManager().registerListener(this);
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEECORD_CHANNEL);
            this.messageChannel = plugin.getConfigManager().getSettings().getProxy().getChannel();
            plugin.getLogger().info("Proxy messenger enabled successfully.");
        } else {
            plugin.debug("Proxy messenger failed to enable.");
        }
    }

    @Override
    public void stop() {
        if (proxyEnabled) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListeners(this);
                plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEECORD_CHANNEL);
                plugin.debug("Proxy messenger stopped successfully");
            } catch (Exception ex) {
                plugin.debug("Proxy messenger failed to stop gracefully!");
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void sendAlert(@NotNull Component alert) {
        if (!canSendAlerts()) return;

        byte[] pluginMessage = createPluginMessage(alert);
        if (pluginMessage == null) return;

        Bukkit.getOnlinePlayers()
                .stream()
                .findFirst()
                .ifPresent(player -> player.sendPluginMessage(plugin, BUNGEECORD_CHANNEL, pluginMessage));
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE || !canReceiveAlerts()) return;
        WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);

        String channel = wrapper.getChannelName();
        if (!channel.equals(BUNGEECORD_CHANNEL) && !channel.equals(BUNGEECORD_CHANNEL_ALT)) return;

        String rawAlert = readRawAlert(wrapper.getData());
        if (rawAlert == null) return;

        Component alert = GsonComponentSerializer.gson().deserialize(rawAlert);
        alertManager.sendAlert(alert);
    }

    private byte[] createPluginMessage(Component message) {
        try (ByteArrayOutputStream messageBytes = new ByteArrayOutputStream()) {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF("Forward");
            output.writeUTF("ONLINE");
            output.writeUTF(messageChannel);

            String rawMessage = GsonComponentSerializer.gson().serialize(message);
            new DataOutputStream(messageBytes).writeUTF(rawMessage);

            output.writeShort(messageBytes.size());
            output.write(messageBytes.toByteArray());
            return output.toByteArray();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to forward alert to other servers.");
            e.printStackTrace();
            return null;
        }
    }

    private boolean canSendAlerts() {
        return proxyEnabled && plugin.getConfigManager().getSettings().getProxy().isSend() && !Bukkit.getOnlinePlayers().isEmpty();
    }

    private boolean canReceiveAlerts() {
        return proxyEnabled && plugin.getConfigManager().getSettings().getProxy().isReceive() && !alertManager.getEnabledAlerts().isEmpty();
    }

    private boolean isProxyEnabled() {
        return Bukkit.spigot().getPaperConfig().getBoolean("proxies.velocity-support.enabled")
                || Bukkit.spigot().getSpigotConfig().getBoolean("settings.bungeecord")
                || (isModernVersion() && Bukkit.spigot().getPaperConfig().getBoolean("proxies.velocity.enabled"));
    }

    private boolean isModernVersion() {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19);
    }
}
