package com.strealex.totemguard.util;

import com.strealex.totemguard.TotemGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Util {

    private static TotemGuard plugin;

    public Util(TotemGuard plugin) {
        Util.plugin = plugin;
    }

    // For now, most of these aren't in use, although, they may be sometime in the future, for now keep them.

    public static void sendMiniMessage(Player player, String message) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public static void sendMiniMessage(Player player, boolean usePrefix, String message) {
        final String prefix = plugin.getConfigManager().getSettings().getPrefix();

        String finalMessage = usePrefix ? prefix + " " + message : message;
        player.sendMessage(MiniMessage.miniMessage().deserialize(finalMessage));
    }

    public static void sendMiniMessage(CommandSender sender, String message) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public static void sendMiniMessage(CommandSender sender, boolean usePrefix, String message) {
        final String prefix = plugin.getConfigManager().getSettings().getPrefix();

        String finalMessage = usePrefix ? prefix + " " + message : message;
        sender.sendMessage(MiniMessage.miniMessage().deserialize(finalMessage));
    }

    public static String convertLegacyToMiniMessage(String legacyText) {
        LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexCharacter('#')
                .extractUrls()
                .build();
        Component component = legacySerializer.deserialize(legacyText);
        return MiniMessage.miniMessage().serialize(component);
    }
}
