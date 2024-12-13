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

public final class InventoryA extends Check implements PacketListener {

    private final TotemGuard plugin;
    private final MessageService messageService;

    public InventoryA(TotemGuard plugin) {
        super(plugin, "InventoryA", "Invalid action with open inventory");
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        plugin.debug(String.valueOf(event.getPacketType()));
        Player player = event.getPlayer();
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            if (player.isSprinting() || player.isBlocking() || player.isSneaking() || player.isSwimming()) {
                final Settings.Checks.InventoryA settings = plugin.getConfigManager().getSettings().getChecks().getInventoryA();
                flag(player, getCheckDetails(player), settings);
            }
        }
    }
    private Component getCheckDetails(Player player) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        Component component = Component.text()
                .build();

        StringBuilder states = new StringBuilder();
        if (player.isSprinting() || player.isSwimming()) {
            states.append("Sprinting, ");
        }
        if (player.isSneaking()) {
            states.append("Sneaking, ");
        }
        if (player.isBlocking()) {
            states.append("Blocking, ");
        }

        if (!states.isEmpty()) {
            states.setLength(states.length() - 2);
            component = component.append(Component.text("States: ", colorScheme.getY()))
                    .append(Component.text(states.toString(), colorScheme.getX()));
        }

        return component;
    }
}
