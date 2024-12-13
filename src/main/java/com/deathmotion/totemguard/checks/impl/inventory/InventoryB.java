package com.deathmotion.totemguard.checks.impl.inventory;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryB extends Check implements PacketListener {

    private final TotemGuard plugin;
    private final MessageService messageService;
    private final ConcurrentHashMap<UUID, Long> inventoryClick;

    public InventoryB(TotemGuard plugin) {
        super(plugin, "InventoryB", "Actions with open inventory");
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.inventoryClick = new ConcurrentHashMap<>();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            if (!inventoryClick.containsKey(player.getUniqueId())) return;
            long storedTime = inventoryClick.get(player.getUniqueId());
            inventoryClick.remove(player.getUniqueId());
            String action = String.valueOf(event.getPacketType());
            check(player, storedTime, action);
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            inventoryClick.remove(player.getUniqueId());
        }
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            inventoryClick.put(player.getUniqueId(), System.currentTimeMillis());
        }

    }
    @Override
    public void resetData () {
        super.resetData();
        inventoryClick.clear();
    }

    @Override
    public void resetData (UUID uuid){
        super.resetData(uuid);
        inventoryClick.remove(uuid);

    }
    private void check(Player player, long storedTime, String action) {
        long timeDifference = Math.abs(System.currentTimeMillis() - storedTime);

        final Settings.Checks.InventoryB settings = plugin.getConfigManager().getSettings().getChecks().getInventoryB();
        if (timeDifference <= 1000) {
            flag(player, getCheckDetails(action, timeDifference, player), settings);
        }
    }

    private Component getCheckDetails(String action, long timeDifference, Player player) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        Component component = Component.text()
                .append(Component.text("Action: ", colorScheme.getY()))
                .append(Component.text(action, colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Difference: ", colorScheme.getY()))
                .append(Component.text(timeDifference, colorScheme.getX()))
                .append(Component.text("ms", colorScheme.getX()))
                .build();
        return component;
    }
}