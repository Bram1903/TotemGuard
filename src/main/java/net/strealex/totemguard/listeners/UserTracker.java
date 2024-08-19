package net.strealex.totemguard.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.data.TotemPlayer;
import net.strealex.totemguard.manager.AlertManager;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserTracker implements PacketListener {

    private final ConcurrentHashMap<UUID, TotemPlayer> totemPlayers = new ConcurrentHashMap<>();
    private final AlertManager alertManager;

    public UserTracker(TotemGuard plugin) {
        this.alertManager = plugin.getAlertManager();
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        User user = event.getUser();

        UUID userUUID = user.getUUID();
        if (userUUID == null) return;

        Player player = (Player) event.getPlayer();

        if (player.hasPermission("TotemGuard.Alerts")) {
            alertManager.enableAlerts(player);
        }

        TotemPlayer totemPlayer = new TotemPlayer();
        totemPlayer.setUuid(userUUID);
        totemPlayer.setUsername(player.getName());
        totemPlayer.setClientBrandName(Objects.requireNonNullElse(player.getClientBrandName(), "Unknown"));
        totemPlayer.setClientVersion(user.getClientVersion());
        totemPlayer.setPing(player.getPing());

        totemPlayers.put(userUUID, totemPlayer);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        UUID userUUID = event.getUser().getUUID();
        if (userUUID == null) return;

        alertManager.removePlayer(userUUID);
        totemPlayers.remove(userUUID);
    }

    public TotemPlayer getTotemPlayer(UUID uuid) {
        return totemPlayers.get(uuid);
    }
}
