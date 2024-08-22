package net.strealex.totemguard.checks;

import net.kyori.adventure.text.Component;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.data.CheckDetails;
import net.strealex.totemguard.data.TotemPlayer;
import net.strealex.totemguard.interfaces.ICheckSettings;
import net.strealex.totemguard.manager.AlertManager;
import net.strealex.totemguard.manager.DiscordManager;
import net.strealex.totemguard.manager.PunishmentManager;
import net.strealex.totemguard.util.AlertCreator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Check {

    private final ConcurrentHashMap<UUID, Integer> violations;

    private final String checkName;
    private final String checkDescription;
    private final boolean experimental;

    private final TotemGuard plugin;
    private final AlertManager alertManager;
    private final PunishmentManager punishmentManager;
    private final DiscordManager discordManager;

    public Check(TotemGuard plugin, String checkName, String checkDescription, boolean experimental) {
        this.plugin = plugin;
        this.checkName = checkName;
        this.checkDescription = checkDescription;
        this.experimental = experimental;

        this.violations = new ConcurrentHashMap<>();

        this.alertManager = plugin.getAlertManager();
        this.punishmentManager = plugin.getPunishmentManager();
        this.discordManager = plugin.getDiscordManager();

        long resetInterval = plugin.getConfigManager().getSettings().getResetViolationsInterval() * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::resetData, resetInterval, resetInterval);
    }

    public Check(TotemGuard plugin, String checkName, String checkDescription) {
        this(plugin, checkName, checkDescription, false);
    }

    public final void flag(Player player, Component details, ICheckSettings settings) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = player.getUniqueId();
            int currentViolations = violations.compute(uuid, (key, value) -> value == null ? 1 : value + 1);

            Optional<TotemPlayer> optionalTotemPlayer = plugin.getUserTracker().getTotemPlayer(uuid);
            if (optionalTotemPlayer.isEmpty()) {
                plugin.getLogger().severe("Failed to get data for player: " + player.getName() + " during check: " + checkName);
                return;
            }

            TotemPlayer totemPlayer = optionalTotemPlayer.get();
            if (totemPlayer.isBedrockPlayer()) return;

            CheckDetails checkDetails = createCheckDetails(player, totemPlayer, details, settings, currentViolations);

            alertManager.sendAlert(checkDetails.getAlert());
            discordManager.sendAlert(totemPlayer, checkDetails);

            if (punishmentManager.handlePunishment(totemPlayer, checkDetails)) {
                violations.remove(uuid);
            }

        });
    }

    public void resetData() {
        violations.clear();
    }

    private CheckDetails createCheckDetails(Player player, TotemPlayer totemPlayer, Component details, ICheckSettings settings, int currentViolations) {
        CheckDetails checkDetails = new CheckDetails();
        checkDetails.setCheckName(checkName);
        checkDetails.setCheckDescription(checkDescription);
        checkDetails.setViolations(currentViolations);
        checkDetails.setTps(plugin.getTps());
        checkDetails.setPing(player.getPing());
        checkDetails.setGamemode(String.valueOf(player.getGameMode()));
        checkDetails.setExperimental(experimental);
        checkDetails.setEnabled(settings.isEnabled());
        checkDetails.setPunishable(settings.isPunishable());
        checkDetails.setMaxViolations(settings.getMaxViolations());
        checkDetails.setPunishmentCommands(settings.getPunishmentCommands());
        checkDetails.setAlert(AlertCreator.createAlertComponent(totemPlayer, checkDetails, details, plugin.getConfigManager().getSettings().getPrefix()));
        checkDetails.setDetails(details);

        return checkDetails;
    }
}
