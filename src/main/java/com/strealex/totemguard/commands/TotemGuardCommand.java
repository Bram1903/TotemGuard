package com.strealex.totemguard.commands;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.config.ConfigManager;
import com.strealex.totemguard.config.Settings;
import com.strealex.totemguard.manager.AlertManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TotemGuardCommand {

    private final ConfigManager configManager;
    private final AlertManager alertManager;

    public TotemGuardCommand(TotemGuard plugin) {
        this.configManager = plugin.getConfigManager();
        this.alertManager = plugin.getAlertManager();

        registerTotemGuardCommand();
    }

    private void registerTotemGuardCommand() {
        new CommandAPICommand("totemguard")
                .withAliases("tg")
                .withSubcommand(new CommandAPICommand("alerts")
                        .withPermission("TotemGuard.Alerts")
                        .withOptionalArguments(new PlayerArgument("target"))
                        .executes((sender, args) -> {
                            if (sender instanceof Player) {
                                handlePlayerCommand((Player) sender, args.getOptional("target"));
                            } else {
                                handleConsoleCommand(sender, args.get("target"));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("reload")
                        .withPermission("TotemGuard.Reload")
                        .executes((sender, args) -> {
                            configManager.reload();
                            sender.sendMessage(Component.text("The configuration has been reloaded!", NamedTextColor.GREEN));
                        }))
                .register();
    }

    private void handlePlayerCommand(Player player, Optional<Object> targetArg) throws WrapperCommandSyntaxException {
        if (targetArg.isEmpty()) {
            alertManager.toggleAlerts(player);
        } else {
            Player target = (Player) targetArg.get();
            if (!player.hasPermission("TotemGuard.Alerts.Others")) {
                throw CommandAPI.failWithString("You do not have permission to toggle alerts for other players.");
            }

            if (player.equals(target)) {
                alertManager.toggleAlerts(target);
                return;
            }

            alertManager.toggleAlerts(target);
            sendToggleMessage(player, target);
        }
    }

    private void handleConsoleCommand(CommandSender sender, Object targetArg) throws WrapperCommandSyntaxException {
        Player target = (Player) targetArg;

        alertManager.toggleAlerts(target);
        sendToggleMessage(sender, target);
    }

    private void sendToggleMessage(CommandSender sender, Player target) {
        Settings settings = configManager.getSettings();

        boolean alertsEnabled = alertManager.hasAlertsEnabled(target);
        NamedTextColor color = alertsEnabled ? NamedTextColor.RED : NamedTextColor.GREEN;
        String message = alertsEnabled ? "disabled" : "enabled";

        Component alertMessage = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text("Alerts are now ", NamedTextColor.WHITE))
                .append(Component.text(message, color))
                .append(Component.text(" for ", NamedTextColor.WHITE))
                .append(Component.text(target.getName(), NamedTextColor.AQUA))
                .append(Component.text(".", NamedTextColor.WHITE))
                .build();

        sender.sendMessage(alertMessage);
    }
}
