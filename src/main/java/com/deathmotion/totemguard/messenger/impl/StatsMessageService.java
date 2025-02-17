/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.messenger.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Messages;
import com.deathmotion.totemguard.manager.ConfigManager;
import com.deathmotion.totemguard.messenger.MessengerService;
import net.kyori.adventure.text.Component;

public class StatsMessageService {
    private final ConfigManager configManager;
    private final MessengerService messengerService;

    public StatsMessageService(TotemGuard plugin, MessengerService messengerService) {
        this.configManager = plugin.getConfigManager();
        this.messengerService = messengerService;
    }

    private Messages.CommandMessages.StatsCommand getStatsCommand() {
        return configManager.getMessages().getCommandMessages().getStatsCommand();
    }

    public Component statsLoading() {
        return messengerService.format(getStatsCommand().getLoadingStats().replace("%prefix%", messengerService.getPrefix()));
    }

    public Component stats(int punishmentCount, int alertCount, long punishmentsLast30Days, long punishmentsLast7Days, long punishmentsLastDay, long alertsLast30Days, long alertsLast7Days, long alertsLastDay) {
        Messages.CommandMessages.StatsCommand.StatsFormat statsFormat = getStatsCommand().getStatsFormat();

        String punishmentSummary = generateSummary(statsFormat.getPunishmentSummary(), punishmentCount, alertCount,
                punishmentsLast30Days, punishmentsLast7Days, punishmentsLastDay, alertsLast30Days, alertsLast7Days, alertsLastDay,
                statsFormat.getNoPunishmentsFound(), punishmentsLast30Days);

        String alertSummary = generateSummary(statsFormat.getAlertSummary(), punishmentCount, alertCount,
                punishmentsLast30Days, punishmentsLast7Days, punishmentsLastDay, alertsLast30Days, alertsLast7Days, alertsLastDay,
                statsFormat.getNoAlertsFound(), alertsLast30Days);

        String message = replacePlaceholders(statsFormat.getStatsFormat(), punishmentCount, alertCount, punishmentsLast30Days,
                punishmentsLast7Days, punishmentsLastDay, alertsLast30Days, alertsLast7Days, alertsLastDay)
                .replace("%punishment_summary%", punishmentSummary)
                .replace("%alert_summary%", alertSummary);

        return messengerService.format(message);
    }

    private String generateSummary(String template, int punishmentCount, int alertCount, long punishmentsLast30Days,
                                   long punishmentsLast7Days, long punishmentsLastDay, long alertsLast30Days,
                                   long alertsLast7Days, long alertsLastDay, String noDataMessage, long relevantCount) {
        if (relevantCount == 0) return noDataMessage;
        return replacePlaceholders(template, punishmentCount, alertCount, punishmentsLast30Days, punishmentsLast7Days,
                punishmentsLastDay, alertsLast30Days, alertsLast7Days, alertsLastDay);
    }

    private String replacePlaceholders(String template, int punishmentCount, int alertCount, long punishmentsLast30Days, long punishmentsLast7Days, long punishmentsLastDay, long alertsLast30Days, long alertsLast7Days, long alertsLastDay) {
        return template
                .replace("%total_punishments%", String.valueOf(punishmentCount))
                .replace("%total_alerts%", String.valueOf(alertCount))
                .replace("%punishment_last_30%", String.valueOf(punishmentsLast30Days))
                .replace("%punishment_last_7%", String.valueOf(punishmentsLast7Days))
                .replace("%punishment_last_24h%", String.valueOf(punishmentsLastDay))
                .replace("%alerts_last_30%", String.valueOf(alertsLast30Days))
                .replace("%alerts_last_7%", String.valueOf(alertsLast7Days))
                .replace("%alerts_last_24h%", String.valueOf(alertsLastDay));
    }
}

