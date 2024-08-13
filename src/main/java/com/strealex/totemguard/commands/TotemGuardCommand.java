package com.strealex.totemguard.commands;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.config.ConfigManager;
import com.strealex.totemguard.config.Settings;
import com.strealex.totemguard.manager.AlertManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public class TotemGuardCommand implements CommandExecutor, TabExecutor {

    private final TotemGuard plugin;
    private final ConfigManager configManager;
    private final AlertManager alertManager;

    public TotemGuardCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.alertManager = plugin.getAlertManager();

        plugin.getCommand("totemguard").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasRequiredPermissions(sender)) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return false;
        }

        if (args.length == 0) {
            sendPrefixMessage(sender, Component.text("Usage: /totemguard <alerts|reload>", NamedTextColor.RED));
            return false;
        }

        return switch (args[0].toLowerCase()) {
            case "alerts" -> handleAlertsCommand(sender, args);
            case "reload" -> handleReloadCommand(sender);
            default -> {
                sendPrefixMessage(sender, Component.text("Usage: /totemguard <alerts|reload>", NamedTextColor.RED));
                yield false;
            }
        };
    }

    private boolean hasRequiredPermissions(CommandSender sender) {
        return sender.hasPermission("TotemGuard.Alerts") || sender.hasPermission("TotemGuard.Reload");
    }

    private boolean handleAlertsCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return toggleAlertsForSender(sender);
        } else if (sender.hasPermission("TotemGuard.Alerts.Others")) {
            return toggleAlertsForOther(sender, args[1]);
        } else {
            sender.sendMessage(Component.text("You do not have permission to toggle alerts for other players!", NamedTextColor.RED));
            return false;
        }
    }

    private boolean toggleAlertsForSender(CommandSender sender) {
        if (sender instanceof Player player) {
            alertManager.toggleAlerts(player);
            return true;
        } else {
            sendPrefixMessage(sender, Component.text("Only players can toggle alerts!", NamedTextColor.RED));
            return false;
        }
    }

    private boolean toggleAlertsForOther(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sendPrefixMessage(sender, Component.text("Player not found!", NamedTextColor.RED));
            return false;
        }

        if (sender instanceof Player player) {
            if ((player.getUniqueId().equals(target.getUniqueId()))) {
                alertManager.toggleAlerts(player);
                return true;
            }
        }

        alertManager.toggleAlerts(target);
        boolean alertsEnabled = alertManager.hasAlertsEnabled(target);

        sendPrefixMessage(sender, Component.text((alertsEnabled ? "Enabled" : "Disabled") + " alerts for " + target.getName() + "!").color(alertsEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        configManager.reload();
        sendPrefixMessage(sender, Component.text("The configuration has been reloaded!", NamedTextColor.GREEN));
        return true;
    }

    private void sendPrefixMessage(CommandSender sender, Component message) {
        final Settings settings = plugin.getConfigManager().getSettings();

        Component prefixMessage = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(message)
                .build();

        sender.sendMessage(prefixMessage);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasRequiredPermissions(sender)) {
            return List.of();
        }

        if (args.length == 1) {
            return Stream.of("alerts", "reload")
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("alerts") && sender.hasPermission("TotemGuard.Alerts.Others")) {
            String argsLowerCase = args[1].toLowerCase();

            if (sender instanceof Player) {
                String senderName = sender.getName().toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> !name.toLowerCase().equals(senderName)) // Prevent self-suggestion
                        .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                        .toList();
            }

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                    .toList();
        }

        return List.of();
    }

}
