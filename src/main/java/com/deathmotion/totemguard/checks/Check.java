package com.deathmotion.totemguard.checks;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.data.CheckDetails;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.deathmotion.totemguard.manager.AlertManager;
import com.deathmotion.totemguard.manager.DiscordManager;
import com.deathmotion.totemguard.manager.PunishmentManager;
import com.deathmotion.totemguard.util.AlertCreator;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

        long resetIntervalMinutes = plugin.getConfigManager().getSettings().getResetViolationsInterval();
        long resetIntervalTicks = resetIntervalMinutes * 60L * 20L;
        long resetIntervalMillis = resetIntervalMinutes * 60L * 1000L;
        if (!FoliaScheduler.isFolia()) {
            FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> resetData(), resetIntervalTicks, resetIntervalTicks);
        } else {
            FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> resetData(), resetIntervalMillis, resetIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    public Check(TotemGuard plugin, String checkName, String checkDescription) {
        this(plugin, checkName, checkDescription, false);
    }

    public final void flag(Player player, Component details, Settings.Checks.CheckSettings settings) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            UUID uuid = player.getUniqueId();

            Optional<TotemPlayer> optionalTotemPlayer = plugin.getUserTracker().getTotemPlayer(uuid);
            if (optionalTotemPlayer.isEmpty()) {
                plugin.getLogger().severe("Failed to get data for player: " + player.getName() + " during check: " + checkName);
                return;
            }

            TotemPlayer totemPlayer = optionalTotemPlayer.get();
            if (!shouldCheck(player, totemPlayer.isBedrockPlayer(), settings)) return;

            int currentViolations = violations.compute(uuid, (key, value) -> value == null ? 1 : value + 1);
            CheckDetails checkDetails = createCheckDetails(player, totemPlayer, details, settings, currentViolations);

            alertManager.sendAlert(checkDetails.getAlert());
            discordManager.sendAlert(totemPlayer, checkDetails);

            if (punishmentManager.handlePunishment(totemPlayer, checkDetails)) {
                violations.remove(uuid);
            }
        });
    }

    public void resetData() {
        plugin.getLogger().info("Resetting violations...");
        violations.clear();
        plugin.getLogger().info("Violations after reset: " + violations.size());
    }

    private boolean shouldCheck(Player player, boolean bedrockPlayer, Settings.Checks.CheckSettings checkSettings) {
        if (!checkSettings.isEnabled()) return false;
        if (bedrockPlayer) return false;

        var settings = plugin.getConfigManager().getSettings();

        if (player.getPing() > settings.getDetermine().getMaxPing() || plugin.getTps() < settings.getDetermine().getMinTps()) {
            return false;
        }

        return !settings.getChecks().isBypass() || !player.hasPermission("TotemGuard.Bypass");
    }

    private CheckDetails createCheckDetails(Player player, TotemPlayer totemPlayer, Component details, Settings.Checks.CheckSettings settings, int currentViolations) {
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
        checkDetails.setPunishmentDelay(settings.getPunishmentDelayInSeconds());
        checkDetails.setMaxViolations(settings.getMaxViolations());
        checkDetails.setPunishmentCommands(settings.getPunishmentCommands());
        checkDetails.setAlert(AlertCreator.createAlertComponent(totemPlayer, checkDetails, details, plugin.getConfigManager().getSettings().getPrefix()));
        checkDetails.setDetails(details);

        return checkDetails;
    }
}
