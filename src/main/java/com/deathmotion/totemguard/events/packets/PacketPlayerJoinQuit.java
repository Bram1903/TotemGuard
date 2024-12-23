package com.deathmotion.totemguard.events.packets;

import com.deathmotion.totemguard.TotemGuard;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            // Do this after send to avoid sending packets before the PLAY state
            event.getTasksAfterSend().add(() -> TotemGuard.getInstance().getPlayerDataManager().addUser(event.getUser()));
        }
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {
        // Player connected too soon, perhaps late bind is off
        // Don't kick everyone on reload
        if (event.getUser().getConnectionState() == ConnectionState.PLAY && !TotemGuard.getInstance().getPlayerDataManager().exemptUsers.contains(event.getUser())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("TotemGuard.Alerts") && player.hasPermission("TotemGuard.Alerts.EnableOnJoin")) {
            TotemGuard.getInstance().getAlertManager().toggleAlerts(player);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        TotemGuard.getInstance().getPlayerDataManager().remove(event.getUser());
        TotemGuard.getInstance().getPlayerDataManager().exemptUsers.remove(event.getUser());

        //Check if calling async is safe
        if (event.getUser().getProfile().getUUID() == null) return; // folia doesn't like null getPlayer()
        Player player = Bukkit.getPlayer(event.getUser().getProfile().getUUID());
        if (player != null) {
            TotemGuard.getInstance().getAlertManager().handlePlayerQuit(player);
        }
    }
}
