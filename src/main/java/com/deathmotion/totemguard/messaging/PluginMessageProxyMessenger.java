package com.deathmotion.totemguard.messaging;

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
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class PluginMessageProxyMessenger extends PacketListenerAbstract implements ProxyAlertMessenger {
    private static final String BUNGEECORD_CHANNEL = "BungeeCord";
    private static final String BUNGEECORD_CHANNEL_ALT = "bungeecord:main";
    private static final String TOTEMGUARD_MESSAGE = "TOTEMGUARD";
    private final @NotNull TotemGuard plugin;
    private final @NotNull AlertManager alertManager;
    private boolean proxyEnabled;

    public PluginMessageProxyMessenger(@NotNull TotemGuard plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();
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
            plugin.debug("Proxy messenger has been enabled.");
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

    public static String readRawAlert(byte[] data) {
        ByteArrayDataInput input = ByteStreams.newDataInput(data);
        if (!TOTEMGUARD_MESSAGE.equals(input.readUTF())) return null;

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

    private byte[] createPluginMessage(Component message) {
        try (ByteArrayOutputStream messageBytes = new ByteArrayOutputStream()) {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF("Forward");
            output.writeUTF("ONLINE");
            output.writeUTF(TOTEMGUARD_MESSAGE);

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

    private boolean isProxyEnabled() {
        return Bukkit.spigot().getPaperConfig().getBoolean("proxies.velocity-support.enabled")
            || Bukkit.spigot().getSpigotConfig().getBoolean("settings.bungeecord")
            || (isModernVersion() && Bukkit.spigot().getPaperConfig().getBoolean("proxies.velocity.enabled"));
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
