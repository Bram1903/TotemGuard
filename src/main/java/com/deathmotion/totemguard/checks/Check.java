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
import com.deathmotion.totemguard.api.enums.CheckType;
import com.deathmotion.totemguard.api.events.FlagEvent;
import com.deathmotion.totemguard.config.impl.Settings;
import com.deathmotion.totemguard.manager.AlertManager;
import com.deathmotion.totemguard.manager.DiscordManager;
import com.deathmotion.totemguard.manager.PunishmentManager;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.models.checks.CheckDetails;
import com.deathmotion.totemguard.models.checks.CheckRecord;
import com.deathmotion.totemguard.models.checks.ICheckSettings;
import com.deathmotion.totemguard.util.MessageService;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Check implements ICheck {

    private final ConcurrentHashMap<UUID, Integer> violations;

    private final String checkName;
    private final String checkDescription;
    private final CheckType checkType;
    private final boolean experimental;

    private final TotemGuard plugin;
    private final AlertManager alertManager;
    private final PunishmentManager punishmentManager;
    private final DiscordManager discordManager;
    private final MessageService messageService;

    public Check(TotemGuard plugin, String checkName, String checkDescription, boolean experimental, CheckType checkType) {
        this.plugin = plugin;
        this.checkName = checkName;
        this.checkDescription = checkDescription;
        this.experimental = experimental;
        this.checkType = checkType;

        this.violations = new ConcurrentHashMap<>();

        this.alertManager = plugin.getAlertManager();
        this.punishmentManager = plugin.getPunishmentManager();
        this.discordManager = plugin.getDiscordManager();
        this.messageService = plugin.getMessageService();
    }

    public Check(TotemGuard plugin, String checkName, String checkDescription) {
        this(plugin, checkName, checkDescription, false, CheckType.Automatic);
    }

    public Check(TotemGuard plugin, String checkName, String checkDescription, CheckType checkType) {
        this(plugin, checkName, checkDescription, false, checkType);
    }

    public Check(TotemGuard plugin, String checkName, String checkDescription, boolean experimental) {
        this(plugin, checkName, checkDescription, experimental, CheckType.Automatic);
    }

    public final void flag(Player player, Component details, ICheckSettings checkSettings) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            if (player == null || !player.isOnline()) return;
            UUID uuid = player.getUniqueId();

            Optional<TotemPlayer> optionalTotemPlayer = plugin.getUserTracker().getTotemPlayer(uuid);
            if (optionalTotemPlayer.isEmpty()) {
                plugin.getLogger().severe("Failed to get data for player: " + player.getName() + " during check: " + checkName);
                return;
            }

            final com.deathmotion.totemguard.config.impl.Settings settings = plugin.getConfigManager().getSettings();

            TotemPlayer totemPlayer = optionalTotemPlayer.get();
            if (checkName.equals("ManualBan")) {
                if (!shouldCheckOverride(player, checkSettings)) return;
            } else {
                if (!shouldCheck(player, totemPlayer.isBedrockPlayer(), checkSettings)) return;
            }

            int currentViolations = violations.compute(uuid, (key, value) -> value == null ? 1 : value + 1);
            CheckDetails checkDetails = createCheckDetails(player, totemPlayer, details, checkSettings, currentViolations);

            if (settings.isApi()) {
                FlagEvent event = new FlagEvent(player, checkDetails);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
            }

            alertManager.sendAlert(totemPlayer, checkDetails);
            discordManager.sendAlert(totemPlayer, checkDetails);

            if (punishmentManager.handlePunishment(player, totemPlayer, checkDetails, settings.getAlertFormat())) {
                violations.remove(uuid);
            }
        });
    }

    private boolean shouldCheck(Player player, boolean bedrockPlayer, ICheckSettings checkSettings) {
        if (!checkSettings.isEnabled()) return false;
        if (bedrockPlayer) return false;

        var settings = plugin.getConfigManager().getSettings();

        if (player.getPing() > settings.getDetermine().getMaxPing() || plugin.getTps() < settings.getDetermine().getMinTps()) {
            return false;
        }

        return !settings.isBypass() || !player.hasPermission("TotemGuard.Bypass");
    }

    private boolean shouldCheckOverride(Player player, ICheckSettings checkSettings) {
        if (!checkSettings.isEnabled()) return false;

        var settings = plugin.getConfigManager().getSettings();
        return !settings.isBypass() || !player.hasPermission("TotemGuard.Bypass");
    }

    private CheckDetails createCheckDetails(Player player, TotemPlayer totemPlayer, Component details, ICheckSettings checkSettings, int currentViolations) {
        final Settings globalSettings = plugin.getConfigManager().getSettings();

        CheckDetails checkDetails = new CheckDetails();
        checkDetails.setCheckName(checkName);
        checkDetails.setCheckDescription(checkDescription);
        checkDetails.setCheckType(checkType);
        checkDetails.setServerName(globalSettings.getServer());
        checkDetails.setViolations(currentViolations);
        checkDetails.setTps(plugin.getTps());
        checkDetails.setPing(player.getPing());
        checkDetails.setGamemode(String.valueOf(player.getGameMode()));
        checkDetails.setExperimental(experimental);
        checkDetails.setEnabled(checkSettings.isEnabled());
        checkDetails.setPunishable(checkSettings.isPunishable());
        checkDetails.setPunishmentDelay(checkSettings.getPunishmentDelayInSeconds());
        checkDetails.setMaxViolations(checkSettings.getMaxViolations());
        checkDetails.setPunishmentCommands(checkSettings.getPunishmentCommands());
        checkDetails.setAlert(messageService.getAlertComponent(totemPlayer, checkDetails, details, globalSettings.getPrefix(), globalSettings.getAlertFormat()));
        checkDetails.setDetails(details);

        return checkDetails;
    }

    @Override
    public String getCheckName() {
        return checkName;
    }

    @Override
    public String getDescription() {
        return checkDescription;
    }

    @Override
    public boolean isExperimental() {
        return experimental;
    }

    @Override
    public void resetData() {
        violations.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        violations.remove(uuid);
    }

    @Override
    public CheckRecord getViolations() {
        return new CheckRecord(checkName, new HashMap<>(violations));
    }
}
