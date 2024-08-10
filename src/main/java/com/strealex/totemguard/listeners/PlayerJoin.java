package com.strealex.totemguard.listeners;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.manager.AlertManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {

    private final AlertManager alertManager;

    public PlayerJoin(TotemGuard plugin) {
        this.alertManager = plugin.getAlertManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("TotemGuard.Alerts")) {
            alertManager.enableAlerts(player);
        }
    }
}
