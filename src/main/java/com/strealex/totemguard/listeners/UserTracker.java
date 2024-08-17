package com.strealex.totemguard.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.manager.AlertManager;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UserTracker implements PacketListener {

    private final AlertManager alertManager;

    public UserTracker(TotemGuard plugin) {
        this.alertManager = plugin.getAlertManager();
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        UUID userUUID = event.getUser().getUUID();
        if (userUUID == null) return;

        Player player = (Player) event.getPlayer();

        if (player.hasPermission("TotemGuard.Alerts")) {
            alertManager.enableAlerts(player);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        UUID userUUID = event.getUser().getUUID();
        if (userUUID == null) return;

        alertManager.removePlayer(userUUID);
    }
}
