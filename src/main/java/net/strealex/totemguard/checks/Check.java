package net.strealex.totemguard.checks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.data.CheckDetails;
import net.strealex.totemguard.data.TotemPlayer;
import net.strealex.totemguard.manager.AlertManager;
import net.strealex.totemguard.manager.DiscordManager;
import net.strealex.totemguard.util.AlertCreator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Check {

    private final ConcurrentHashMap<UUID, Integer> violations;

    private final String checkName;
    private final String checkDescription;
    private final boolean experimental;

    private final TotemGuard plugin;
    private final AlertManager alertManager;
    private final DiscordManager discordManager;

    public Check(TotemGuard plugin, String checkName, String checkDescription, boolean experimental) {
        this.plugin = plugin;
        this.checkName = checkName;
        this.checkDescription = checkDescription;
        this.experimental = experimental;

        this.violations = new ConcurrentHashMap<>();

        this.alertManager = plugin.getAlertManager();
        this.discordManager = plugin.getDiscordManager();

        // Convert minutes to ticks (20 ticks = 1 second)
        long resetInterval = plugin.getConfigManager().getSettings().getResetViolationsInterval() * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::resetData, resetInterval, resetInterval);
    }

    public Check(TotemGuard plugin, String checkName, String checkDescription) {
        this(plugin, checkName, checkDescription, false);
    }

    public final void flag(Player player, Component details, ICheckSettings settings) {
        UUID uuid = player.getUniqueId();
        int currentViolations = violations.compute(uuid, (key, value) -> value == null ? 1 : value + 1);

        TotemPlayer totemPlayer = plugin.getUserTracker().getTotemPlayer(uuid);
        CheckDetails checkDetails = CheckDetails.builder()
                .checkName(checkName)
                .checkDescription(checkDescription)
                .violations(currentViolations)
                .tps(plugin.getTps())
                .ping(player.getPing())
                .gamemode(String.valueOf(player.getGameMode()))
                .experimental(experimental)
                .enabled(settings.isEnabled())
                .punishable(settings.isPunishable())
                .maxViolations(settings.getMaxViolations())
                .punishmentCommands(settings.getPunishmentCommands())
                .build();

        checkDetails.setAlert(AlertCreator.createAlertComponent(totemPlayer, checkDetails, details, plugin.getConfigManager().getSettings().getPrefix()));

        alertManager.sendAlert(checkDetails.getAlert());
        discordManager.sendAlert(totemPlayer, checkDetails);
        punishPlayer(player, settings);
    }

    public void resetData() {
        violations.clear();

        alertManager.sendAlert(Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                .append(Component.text("All flag counts have been reset.", NamedTextColor.GREEN))
                .build());
    }

    private int getViolations(UUID player) {
        return violations.getOrDefault(player, 0);
    }

    private void punishPlayer(Player player, ICheckSettings settings) {
        if (!(settings.isPunishable())) return;
        if (getViolations(player.getUniqueId()) >= settings.getMaxViolations()) {
            violations.remove(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                Arrays.stream(settings.getPunishmentCommands()).iterator().forEachRemaining(punishCommand -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCommand.replace("%player%", player.getName()));
                });
            });

            // Send punishment webhook
            //sendWebhookMessage(player, Component.text("Player has been punished for exceeding violation limit."), true);
        }
    }
}
