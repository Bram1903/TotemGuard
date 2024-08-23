package net.strealex.totemguard.checks.impl.badpackets;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.checks.Check;
import net.strealex.totemguard.config.Settings;
import org.bukkit.entity.Player;

public class BadPacketsA extends Check implements PacketListener {

    private final TotemGuard plugin;

    public BadPacketsA(TotemGuard plugin) {
        super(plugin, "BadPacketsA", "Player is using a suspicious mod!");
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE && event.getPacketType() != PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            return;
        }

        WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
        String channel = packet.getChannelName();

        if (channel.equalsIgnoreCase("minecraft:using_autototem")) {
            final Settings.Checks.BadPacketsA settings = plugin.getConfigManager().getSettings().getChecks().getBadPacketsA();

            Component checkDetails = Component.text()
                    .append(Component.text("Channel: ", NamedTextColor.GRAY))
                    .append(Component.text(channel, NamedTextColor.GOLD))
                    .build();

            flag((Player) event.getPlayer(), checkDetails, settings);
        }
    }
}
