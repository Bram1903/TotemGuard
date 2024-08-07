package de.outdev.totemguard.commands;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.config.Settings;
import de.outdev.totemguard.manager.AlertManager;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.PlayerArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AlertsCommand {
    private final Settings settings;
    private final AlertManager alertManager;

    public AlertsCommand(TotemGuard plugin) {
        this.settings = plugin.getConfigManager().getSettings();
        this.alertManager = plugin.getAlertManager();

        registerCommand();
    }

    private void registerCommand() {
        new CommandTree("tgalerts")
                .withPermission("TotemGuard.Alerts")
                .executesConsole((sender, args) -> {
                    sender.sendMessage(Component.text("You need to specify a player!", NamedTextColor.RED));
                })
                .executesPlayer((player) -> {
                    alertManager.toggleAlerts(player.sender());
                })
                .then(new PlayerArgument("target")
                        .withPermission("TotemGuard.Alerts.Others")
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            handleTargetAlerts(sender, target);
                        }))
                .register();
    }

    private void handleTargetAlerts(CommandSender sender, Player target) {
        boolean alertsEnabled = alertManager.hasAlertsEnabled(target);
        NamedTextColor color = alertsEnabled ? NamedTextColor.RED : NamedTextColor.GREEN;
        String message = alertsEnabled ? "disabled" : "enabled";

        sender.sendMessage(Component.text(settings.getPrefix() + "Alerts are now " + message + " for " + target.getName() + ".", color));
        alertManager.toggleAlerts(target);
    }
}
