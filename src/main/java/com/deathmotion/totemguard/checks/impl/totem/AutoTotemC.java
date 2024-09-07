package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Settings;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoTotemC extends Check implements PacketListener, Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, Long> totemUsage;
    private final ConcurrentHashMap<UUID, PacketState> playerPacketState;

    public AutoTotemC(TotemGuard plugin) {
        super(plugin, "AutoTotemC", "Suspicious re-totem packet sequence", true);

        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        this.totemUsage = new ConcurrentHashMap<>();
        this.playerPacketState = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Record the time of the totem usage
        long totemPopTime = System.currentTimeMillis();
        totemUsage.put(player.getUniqueId(), totemPopTime);
        playerPacketState.remove(player.getUniqueId());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Handle Digging Packet
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            Player player = (Player) event.getPlayer();
            // Check if a totem has recently been used
            if (!totemUsage.containsKey(player.getUniqueId())) return;

            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                handleDiggingPacket(player, System.currentTimeMillis());
            }
        }

        // Handle Pick Item Packet
        if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM) {
            handlePickItemPacket((Player) event.getPlayer(), System.currentTimeMillis());
        }
    }

    // Handle the Digging Packet (we expect this to be called twice)
    private void handleDiggingPacket(Player player, long currentTime) {
        UUID playerId = player.getUniqueId();
        PacketState state = playerPacketState.getOrDefault(playerId, new PacketState());

        if (state.sequence == 0 || state.sequence == 2) {
            state.sequence++;
            state.lastDiggingPacketTime = currentTime;
        }

        if (state.sequence == 3) {
            // Third Digging Packet → run the validation
            long totemPopTime = totemUsage.getOrDefault(playerId, 0L);
            long totalPacketTime = currentTime - totemPopTime;
            long timeFromFirstDigging = currentTime - state.firstPacketTime;

            final Settings.Checks.AutoTotemC settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemC();

            // Check the timing and flag the player if needed
            if (totalPacketTime <= settings.getPacketTimeThreshold()) {
                Component details = Component.text()
                        .append(Component.text("Total time: ", NamedTextColor.GRAY))
                        .append(Component.text(totalPacketTime + " ms", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("Digging to PickItem: ", NamedTextColor.GRAY))
                        .append(Component.text(state.timeToPickItem + " ms", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("PickItem to Digging: ", NamedTextColor.GRAY))
                        .append(Component.text(timeFromFirstDigging + " ms", NamedTextColor.GOLD))
                        .build();

                flag(player, details, settings);
            }

            // Reset state after validation
            playerPacketState.remove(playerId);
        } else {
            // Update the state and store it
            if (state.sequence == 1) {
                state.firstPacketTime = currentTime;
            }
            playerPacketState.put(playerId, state);
        }
    }

    // Handle the PickItem Packet (this should be called once)
    private void handlePickItemPacket(Player player, long currentTime) {
        UUID playerId = player.getUniqueId();
        PacketState state = playerPacketState.getOrDefault(playerId, new PacketState());

        if (state.sequence == 1) {
            state.sequence++;
            state.pickItemPacketTime = currentTime;
            state.timeToPickItem = currentTime - state.firstPacketTime;

            // Update the state
            playerPacketState.put(playerId, state);
        }
    }

    @Override
    public void resetData() {
        totemUsage.clear();
        playerPacketState.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        totemUsage.remove(uuid);
        playerPacketState.remove(uuid);
    }

    // Inner class to store packet states for each player
    private static class PacketState {
        int sequence = 0; // Sequence: 0 (none), 1 (Digging), 2 (PickItem), 3 (Digging)
        long firstPacketTime = 0;
        long lastDiggingPacketTime = 0;
        long pickItemPacketTime = 0;
        long timeToPickItem = 0;
    }
}


