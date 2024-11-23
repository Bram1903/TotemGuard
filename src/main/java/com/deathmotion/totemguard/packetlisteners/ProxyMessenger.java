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
import com.deathmotion.totemguard.manager.AlertManager;
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
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public class ProxyMessenger extends PacketListenerAbstract {

    private final TotemGuard plugin;
    private final AlertManager alertManager;
    private final boolean proxyEnabled;

    public ProxyMessenger(TotemGuard plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();
        this.proxyEnabled = initializeProxy();

        if (proxyEnabled) {
            registerMessenger();
            plugin.debug("Proxy messenger has been enabled.");
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE || !canReceiveAlerts()) return;
        WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);

        String channel = wrapper.getChannelName();
        if (!channel.equals("BungeeCord") && !channel.equals("bungeecord:main")) return;

        String rawAlert = readRawAlert(wrapper.getData());
        if (rawAlert == null) return;

        Component alert = GsonComponentSerializer.gson().deserialize(rawAlert);
        alertManager.sendAlert(alert);
    }

    public void sendPluginMessage(Component message) {
        if (!canSendAlerts()) return;

        byte[] pluginMessage = createPluginMessage(message);
        if (pluginMessage == null) return;

        Bukkit.getOnlinePlayers().stream().findFirst().ifPresent(player -> player.sendPluginMessage(plugin, "BungeeCord", pluginMessage));
    }

    private String readRawAlert(byte[] data) {
        ByteArrayDataInput input = ByteStreams.newDataInput(data);
        if (!"TOTEMGUARD".equals(input.readUTF())) return null;

        byte[] messageBytes = new byte[input.readShort()];
        input.readFully(messageBytes);

        try (DataInputStream stream = new DataInputStream(new ByteArrayInputStream(messageBytes))) {
            return stream.readUTF();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read forwarded alert from another server.");
            e.printStackTrace();
            return null;
        }
    }

    private byte[] createPluginMessage(Component message) {
        try (ByteArrayOutputStream messageBytes = new ByteArrayOutputStream()) {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF("Forward");
            output.writeUTF("ONLINE");
            output.writeUTF("TOTEMGUARD");

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
        return proxyEnabled && plugin.getConfigManager().getSettings().getProxyAlerts().isSend() && !Bukkit.getOnlinePlayers().isEmpty();
    }

    private boolean canReceiveAlerts() {
        return proxyEnabled && plugin.getConfigManager().getSettings().getProxyAlerts().isReceive() && !alertManager.getEnabledAlerts().isEmpty();
    }

    private void registerMessenger() {
        PacketEvents.getAPI().getEventManager().registerListener(this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
    }

    private boolean initializeProxy() {
        return getBooleanFromConfig("spigot.yml", "settings.bungeecord") ||
                getBooleanFromConfig("paper.yml", "settings.velocity-support.enabled") ||
                (isModernVersion() && getBooleanFromConfig("config/paper-global.yml", "proxies.velocity.enabled"));
    }

    private boolean getBooleanFromConfig(String filePath, String key) {
        File file = new File(filePath);
        if (!file.exists()) return false;
        return YamlConfiguration.loadConfiguration(file).getBoolean(key);
    }

    private boolean isModernVersion() {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19);
    }
}
