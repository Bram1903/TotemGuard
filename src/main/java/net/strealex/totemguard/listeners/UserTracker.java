package net.strealex.totemguard.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.util.GeyserUtil;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.data.TotemPlayer;
import net.strealex.totemguard.manager.AlertManager;
import net.strealex.totemguard.util.bedrock.FloodgateUtil;
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
        totemPlayer.setBedrockPlayer(isBedrockPlayer(user));

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

    private boolean isBedrockPlayer(User user) {
        if (user.getUUID() != null) {
            // Geyser players don't have Java movement
            // Floodgate is the authentication system for Geyser on servers that use Geyser as a proxy instead of installing it as a plugin directly on the server
            if (GeyserUtil.isGeyserPlayer(user.getUUID()) || FloodgateUtil.isFloodgatePlayer(user.getUUID())) {
                return true;
            }

            // Geyser formatted player string
            // This will never happen for Java players, as the first character in the 3rd group is always 4 (xxxxxxxx-xxxx-4xxx-xxxx-xxxxxxxxxxxx)
            return user.getUUID().toString().startsWith("00000000-0000-0000-0009");
        }

        return false;
    }
}
