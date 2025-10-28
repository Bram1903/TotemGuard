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

package com.deathmotion.totemguard.commands.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.AbstractCommand;
import com.deathmotion.totemguard.commands.arguments.PlayerSuggestion;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.models.impl.SafetyStatus;
import com.deathmotion.totemguard.util.MessageUtil;
import com.deathmotion.totemguard.util.PlayerUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public final class ProfileCommand extends AbstractCommand {

    private final TotemGuard plugin;
    private final DatabaseProvider db;
    private final MessengerService messenger;
    private final CommandMessengerService cms;
    private final ZoneId zoneId;

    public ProfileCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseProvider();
        this.messenger = plugin.getMessengerService();
        this.cms = messenger.getCommandMessengerService();
        this.zoneId = ZoneId.systemDefault();
    }

    @Override
    public void register(final LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(root(commandManager)
                .literal("profile", Description.of("Gets the profile of a player"))
                .required("target", StringParser.stringParser(), PlayerSuggestion.onlinePlayerSuggestions())
                .permission(perm("Profile"))
                .handler(this::handle)
        );
    }

    private void handle(@NonNull final CommandContext<CommandSender> ctx) {
        final CommandSender sender = ctx.sender();
        final String username = ctx.get("target");
        sender.sendMessage(cms.loadingProfile(username));

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            try {
                long startTime = System.currentTimeMillis();

                OfflinePlayer offlinePlayer = PlayerUtil.getOfflinePlayer(username);
                UUID uuid = offlinePlayer.getUniqueId();

                DatabasePlayer databasePlayer = db.getPlayerRepository().findByUuid(uuid).orElse(null);
                if (databasePlayer == null) {
                    sender.sendMessage(MessageUtil.getPrefix().append(Component.text(" Player not found", NamedTextColor.RED)));
                    return;
                }

                Instant dayStart = LocalDate.now(zoneId)
                        .atStartOfDay(zoneId)
                        .toInstant();

                String brand = databasePlayer.getClientBrand() != null ? databasePlayer.getClientBrand() : "";

                long totalAlerts = db.getAlertRepository().countAlertsForPlayer(uuid);
                long totalPunishments = db.getPunishmentRepository().countPunishmentsForPlayer(uuid);

                long alertsToday = db.getAlertRepository().countAlertsSinceForPlayer(uuid, dayStart);

                List<DatabaseAlert> alerts = db.getAlertRepository().findRecentAlertsForPlayer(uuid, 20);
                List<DatabasePunishment> punishments = db.getPunishmentRepository().findRecentPunishmentsForPlayer(uuid, 20);

                long loadTime = System.currentTimeMillis() - startTime;
                SafetyStatus status = SafetyStatus.getSafetyStatus(
                        (int) alertsToday,
                        (int) totalPunishments
                );

                sender.sendMessage(messenger
                        .getProfileMessageService()
                        .createProfileMessage(
                                username,
                                brand,
                                (int) totalAlerts,
                                (int) totalPunishments,
                                loadTime,
                                status,
                                alerts,
                                punishments
                        )
                );
            } catch (Exception e) {
                sender.sendMessage("§cFailed to load profile: " + e.getMessage());
            }
        });
    }
}
