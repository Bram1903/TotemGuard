package com.deathmotion.totemguard.events.packets;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsB;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    private final TotemGuard plugin;

    public PacketPlayerJoinQuit(TotemGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            // Do this after send to avoid sending packets before the PLAY state
            event.getTasksAfterSend().add(() -> TotemGuard.getInstance().getPlayerDataManager().addUser(event.getUser()));
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("TotemGuard.Alerts") && player.hasPermission("TotemGuard.Alerts.EnableOnJoin")) {
            TotemGuard.getInstance().getAlertManager().toggleAlerts(player);
        }

        if (plugin.getConfigManager().getSettings().getUpdateChecker().isNotifyInGame() && plugin.getUpdateChecker().isUpdateAvailable()) {
            if (player.hasPermission("TotemGuard.Update")) {
                FoliaScheduler.getAsyncScheduler().runDelayed(plugin, (o) -> {
                    player.sendMessage(plugin.getUpdateChecker().getUpdateComponent());
                }, 2, TimeUnit.SECONDS);
            }
        }

        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getUser());
        if (totemPlayer == null) return;

        totemPlayer.bukkitPlayer = player;
        totemPlayer.loadDatabasePlayer();

        // Trigger the BadPacketsB check here, as it will otherwise still be in the configuration state
        totemPlayer.checkManager.getPacketCheck(BadPacketsB.class).handle(totemPlayer.getBrand());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        TotemGuard.getInstance().getPlayerDataManager().remove(event.getUser());

        //Check if calling async is safe
        if (event.getUser().getProfile().getUUID() == null) return; // folia doesn't like null getPlayer()
        Player player = Bukkit.getPlayer(event.getUser().getProfile().getUUID());
        if (player != null) {
            TotemGuard.getInstance().getAlertManager().handlePlayerQuit(player);
        }
    }
}
