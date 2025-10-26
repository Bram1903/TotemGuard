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
import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.models.impl.SafetyStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfileMessageService {

    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
    private final TotemGuard plugin;
    private final MessengerService messengerService;

    public ProfileMessageService(TotemGuard plugin, MessengerService messengerService) {
        this.plugin = plugin;
        this.messengerService = messengerService;
    }

    public Component createProfileMessage(String username,
                                          String clientBrand,
                                          int totalAlerts,
                                          int totalPunishments,
                                          long loadTime,
                                          SafetyStatus safetyStatus,
                                          List<DatabaseAlert> alerts,
                                          List<DatabasePunishment> punishments) {

        Messages messages = plugin.getConfigManager().getMessages();
        Messages.CommandMessages.ProfileCommand.ProfileFormat format = messages.getCommandMessages().getProfileCommand().getProfileFormat();

        String profileTemplate = format.getProfileFormat();
        Component alertSummary = buildAlertSummary(alerts, format.getAlertSummary(), format.getNoAlertsFound());
        Component punishmentSummary = buildPunishmentSummary(punishments, format.getPunishmentSummary(), format.getNoPunishmentsFound(), format.getShowingLastPunishments(), format.getAndMoreToBeDisplayed());

        String filledTemplate = replacePlaceholders(
                profileTemplate,
                username,
                clientBrand,
                totalAlerts,
                totalPunishments,
                loadTime,
                safetyStatus
        );

        Component profileComponent = messengerService.format(filledTemplate);
        profileComponent = profileComponent.replaceText(builder -> builder.match("%alert_summary%").replacement(alertSummary));
        profileComponent = profileComponent.replaceText(builder -> builder.match("%punishment_summary%").replacement(punishmentSummary));

        return profileComponent;
    }

    public String replacePlaceholders(String text,
                                      String username,
                                      String clientBrand,
                                      int totalAlerts,
                                      int totalPunishments,
                                      long loadTime,
                                      SafetyStatus safetyStatus) {

        return text
                .replace("%player%", username)
                .replace("%client_brand%", clientBrand)
                .replace("%safety_status%", messengerService.unformat(safetyStatus.toComponent()))
                .replace("%total_alerts%", String.valueOf(totalAlerts))
                .replace("%total_punishments%", String.valueOf(totalPunishments))
                .replace("%load_time%", String.valueOf(loadTime));
    }

    private Component buildAlertSummary(List<DatabaseAlert> alerts,
                                        String alertSummaryTemplate,
                                        String noAlertsTemplate) {

        if (alerts.isEmpty()) {
            return messengerService.format(noAlertsTemplate);
        }

        // Group alerts by check name
        Map<String, Long> groupedAlerts = alerts.stream().collect(Collectors.groupingBy(DatabaseAlert::getCheckName, Collectors.counting()));

        // Sort descending by count
        List<Map.Entry<String, Long>> sortedEntries = groupedAlerts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        StringBuilder summaryBuilder = new StringBuilder();
        int currentIndex = 0;
        int totalEntries = sortedEntries.size();

        for (Map.Entry<String, Long> entry : sortedEntries) {
            summaryBuilder.append(alertSummaryTemplate.replace("%check_name%", entry.getKey()).replace("%violations%", String.valueOf(entry.getValue())));
            currentIndex++;
            if (currentIndex < totalEntries) {
                summaryBuilder.append("\n");
            }
        }

        return messengerService.format(summaryBuilder.toString());
    }

    private Component buildPunishmentSummary(List<DatabasePunishment> punishments,
                                             String punishmentSummaryTemplate,
                                             String noPunishmentsTemplate,
                                             String showingLastPunishments,
                                             String andMoreToBeDisplayed) {

        if (punishments.isEmpty()) {
            return messengerService.format(noPunishmentsTemplate);
        }

        Component summary = Component.empty();
        if (punishments.size() > 3) {
            summary = summary
                    .append(messengerService.format(showingLastPunishments))
                    .append(Component.newline());
        }


        List<DatabasePunishment> recentPunishments = punishments.stream()
                .sorted(Comparator.comparing(DatabasePunishment::getWhenCreated).reversed())
                .limit(3)
                .toList();

        List<Component> punishmentComponents = recentPunishments.stream()
                .map(punishment -> {
                    ZonedDateTime time = ZonedDateTime.ofInstant(punishment.getWhenCreated(), DEFAULT_ZONE_ID);
                    Component message = messengerService.format(punishmentSummaryTemplate.replace("%check_name%", punishment.getCheckName()).replace("%date%", time.format(SHORT_DATE_FORMATTER)));
                    return message.hoverEvent(HoverEvent.showText(Component.text("Occurred " + getRelativeTime(punishment.getWhenCreated()), NamedTextColor.GRAY)));
                })
                .toList();

        summary = summary.append(Component.join(JoinConfiguration.separator(Component.newline()), punishmentComponents));

        if (punishments.size() > 3) {
            summary = summary.append(Component.newline());
            summary = summary.append(messengerService.format(andMoreToBeDisplayed));
        }

        return summary;
    }

    /**
     * Determines the relative time (e.g., "3 days ago") from the given Instant to now.
     *
     * @param past the Instant representing the event in the past
     * @return a human-readable string for the elapsed time
     */
    public String getRelativeTime(Instant past) {
        Duration duration = Duration.between(past, Instant.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "just now";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes ago";
        } else if (seconds < 86_400) {
            return (seconds / 3600) + " hours ago";
        } else {
            return (seconds / 86_400) + " days ago";
        }
    }
}
