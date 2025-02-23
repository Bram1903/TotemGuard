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

package com.deathmotion.totemguard.database;

import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.database.repository.impl.AlertRepository;
import com.deathmotion.totemguard.database.repository.impl.PlayerRepository;
import com.deathmotion.totemguard.database.repository.impl.PunishmentRepository;
import com.deathmotion.totemguard.util.datastructure.Pair;
import org.jetbrains.annotations.Blocking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Blocking
public class DatabaseService {

    private final PlayerRepository playerRepository;
    private final AlertRepository alertRepository;
    private final PunishmentRepository punishmentRepository;

    public DatabaseService(DatabaseProvider databaseProvider) {
        this.playerRepository = databaseProvider.getPlayerRepository();
        this.alertRepository = databaseProvider.getAlertRepository();
        this.punishmentRepository = databaseProvider.getPunishmentRepository();
    }

    public Pair<List<DatabaseAlert>, List<DatabasePunishment>> retrieveLogs(UUID uuid) {
        DatabasePlayer player = playerRepository.fetchOrCreatePlayer(uuid);
        if (player == null) {
            return null;
        }

        List<DatabaseAlert> alerts = alertRepository.findAlertsByPlayer(player);
        List<DatabasePunishment> punishments = punishmentRepository.findPunishmentsByPlayer(player);

        return new Pair<>(alerts, punishments);
    }

    public int eraseLogs(UUID uuid) {
        DatabasePlayer player = playerRepository.fetchOrCreatePlayer(uuid);
        if (player == null) {
            return -1;
        }

        int removedAlerts = alertRepository.deleteAlertsByPlayer(player);
        int removedPunishments = punishmentRepository.deletePunishmentsByPlayer(player);
        return removedAlerts + removedPunishments;
    }

    public int optimizeDatabase() {
        Instant cutoff = Instant.now().minusSeconds(30L * 24L * 3600L);

        int oldAlerts = alertRepository.deleteAlertsOlderThan(cutoff);
        int oldPunishments = punishmentRepository.deletePunishmentsOlderThan(cutoff);

        return oldAlerts + oldPunishments;
    }

    @Blocking
    public int wipeDatabase() {
        int alertCount = alertRepository.deleteAllAlerts();
        int punishmentCount = punishmentRepository.deleteAllPunishments();
        int playersRemoved = playerRepository.deleteAllPlayers();

        return alertCount + punishmentCount + playersRemoved;
    }
}
