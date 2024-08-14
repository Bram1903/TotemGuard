package com.strealex.totemguard.checks.impl;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.checks.Check;
import com.strealex.totemguard.config.Settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.BatchUpdateException;
import java.util.UUID;

public class BadPacketsA extends Check implements PacketListener {

    private final TotemGuard plugin;

    public BadPacketsA(TotemGuard plugin) {
        super(plugin, "BadPacketsA", "Player is using a suspicious mod!");

        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final Settings.Checks.BadPacketsA settings = plugin.getConfigManager().getSettings().getChecks().getBadPacketsA();
        if (!settings.isEnabled()) {
            return;
        }

        User user = event.getUser();

        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE && event.getPacketType() != PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            return;
        }

        WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);

        if (packet.getChannelName().equalsIgnoreCase("minecraft:using_autototem")) {
            Component checkDetails = Component.text("AutoTotem Mod Packet Detected", NamedTextColor.RED);
            UUID uuid = user.getUUID();
            Player player = Bukkit.getPlayer(uuid);
            assert player != null;
            flag(player, checkDetails, settings);
        }
    }
}
