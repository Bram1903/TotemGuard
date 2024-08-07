package de.outdev.totemguard.util;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Util {

    private static TotemGuard plugin = TotemGuard.getInstance();
    private static Settings settings = plugin.getConfigManager().getSettings();
    private static String prefix = settings.getPrefix();

    public Util(TotemGuard plugin) {
        Util.plugin = plugin;
        Util.settings = plugin.getConfigManager().getSettings();
        Util.prefix = settings.getPrefix();
    }

    // For now, most of these aren't in use, although, they may be sometime in the future, for now keep them.

    public static void sendMiniMessage(Player player, String message) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public static void sendMiniMessage(Player player, boolean usePrefix, String message) {
        String finalMessage = usePrefix ? prefix + " " + message : message;
        player.sendMessage(MiniMessage.miniMessage().deserialize(finalMessage));
    }

    public static void sendMiniMessage(CommandSender sender, String message) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public static void sendMiniMessage(CommandSender sender, boolean usePrefix, String message) {
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
