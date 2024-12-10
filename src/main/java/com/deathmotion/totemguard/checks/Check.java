/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.checks;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.manager.AlertManager;
import com.deathmotion.totemguard.manager.DiscordManager;
import com.deathmotion.totemguard.manager.PunishmentManager;
import com.deathmotion.totemguard.models.CheckDetails;
import com.deathmotion.totemguard.models.CheckRecord;
import com.deathmotion.totemguard.models.ICheckSettings;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.MessageService;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Check implements ICheck {

    private final ConcurrentHashMap<UUID, Integer> violations;

    private final String checkName;
    private final String checkDescription;
    private final boolean experimental;

    private final TotemGuard plugin;
    private final AlertManager alertManager;
    private final PunishmentManager punishmentManager;
    private final DiscordManager discordManager;
    private final MessageService messageService;

    public Check(TotemGuard plugin, String checkName, String checkDescription, boolean experimental) {
        this.plugin = plugin;
        this.checkName = checkName;
        this.checkDescription = checkDescription;
        this.experimental = experimental;

        this.violations = new ConcurrentHashMap<>();

        this.alertManager = plugin.getAlertManager();
        this.punishmentManager = plugin.getPunishmentManager();
        this.discordManager = plugin.getDiscordManager();
        this.messageService = plugin.getMessageService();
    }

    public Check(TotemGuard plugin, String checkName, String checkDescription) {
        this(plugin, checkName, checkDescription, false);
    }

    public final void flag(Player player, Component details, ICheckSettings settings) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            if (player == null || !player.isOnline()) return;
            UUID uuid = player.getUniqueId();

            Optional<TotemPlayer> optionalTotemPlayer = plugin.getUserTracker().getTotemPlayer(uuid);
            if (optionalTotemPlayer.isEmpty()) {
                plugin.getLogger().severe("Failed to get data for player: " + player.getName() + " during check: " + checkName);
                return;
            }

            TotemPlayer totemPlayer = optionalTotemPlayer.get();
            if (checkName.equals("ManualBan")) {
                if (!shouldCheckOverride(player, settings)) return;
            } else {
                if (!shouldCheck(player, totemPlayer.isBedrockPlayer(), settings)) return;
            }

            int currentViolations = violations.compute(uuid, (key, value) -> value == null ? 1 : value + 1);
            CheckDetails checkDetails = createCheckDetails(player, totemPlayer, details, settings, currentViolations);

            alertManager.sendAlert(totemPlayer, checkDetails);
            discordManager.sendAlert(totemPlayer, checkDetails);

            if (punishmentManager.handlePunishment(totemPlayer, checkDetails, plugin.getConfigManager().getSettings().getAlertFormat())) {
                violations.remove(uuid);
            }
        });
    }

    public void resetData() {
        violations.clear();
    }

    public void resetData(UUID uuid) {
        violations.remove(uuid);
    }

    public CheckRecord getViolations() {
        return new CheckRecord(checkName, new HashMap<>(violations));
    }

    private boolean shouldCheck(Player player, boolean bedrockPlayer, ICheckSettings checkSettings) {
        if (!checkSettings.isEnabled()) return false;
        if (bedrockPlayer) return false;

        var settings = plugin.getConfigManager().getSettings();

        if (player.getPing() > settings.getDetermine().getMaxPing() || plugin.getTps() < settings.getDetermine().getMinTps()) {
            return false;
        }

        return !settings.getChecks().isBypass() || !player.hasPermission("TotemGuard.Bypass");
    }

    private boolean shouldCheckOverride(Player player, ICheckSettings checkSettings) {
        if (!checkSettings.isEnabled()) return false;

        var settings = plugin.getConfigManager().getSettings();
        return !settings.getChecks().isBypass() || !player.hasPermission("TotemGuard.Bypass");
    }

    private CheckDetails createCheckDetails(Player player, TotemPlayer totemPlayer, Component details, ICheckSettings settings, int currentViolations) {
        final Settings globalSettings = plugin.getConfigManager().getSettings();

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
        checkDetails.setAlert(messageService.getAlertComponent(totemPlayer, checkDetails, details, globalSettings.getPrefix(), globalSettings.getAlertFormat()));
        checkDetails.setDetails(details);

        return checkDetails;
    }
}
