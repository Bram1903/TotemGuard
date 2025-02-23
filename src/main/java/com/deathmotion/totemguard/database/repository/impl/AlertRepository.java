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

package com.deathmotion.totemguard.database.repository.impl;

import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.repository.BaseRepository;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AlertRepository extends BaseRepository<DatabaseAlert, UUID> {
    private static final String PLAYER_COLUMN = "totemguard_player_uuid";
    private static final String CREATION_TIMESTAMP_COLUMN = "when_created";
    private final DatabaseProvider databaseProvider;

    public AlertRepository(DatabaseProvider databaseProvider) {
        super(databaseProvider.getConnectionSource(), DatabaseAlert.class);
        this.databaseProvider = databaseProvider;
    }

    public void storeAlert(Check check) {
        // Make sure we have a valid player in the database
        DatabasePlayer dbPlayer = check.getPlayer().databasePlayer;
        if (dbPlayer == null) {
            // If not present, retrieve or refresh
            dbPlayer = databaseProvider.getPlayerRepository().retrieveOrRefreshPlayer(check.getPlayer());
        }

        // Create and populate the new alert
        DatabaseAlert alert = new DatabaseAlert();
        alert.setCheckName(check.getCheckName());
        alert.setWhenCreated(Date.from(Instant.now()));
        alert.setPlayer(dbPlayer);

        // Save alert to the DB
        save(alert);
    }

    public List<DatabaseAlert> retrieveAlerts() {
        return findAll();
    }

    public List<DatabaseAlert> findAlertsByPlayer(DatabasePlayer player) {
        return execute(() -> {
            QueryBuilder<DatabaseAlert, UUID> qb = createQueryBuilder();
            qb.where().eq(PLAYER_COLUMN, player.getUuid());
            return executeQuery(qb);
        });
    }

    public int deleteAlertsByPlayer(DatabasePlayer player) {
        return execute(() -> {
            DeleteBuilder<DatabaseAlert, UUID> db = dao.deleteBuilder();
            db.where().eq(PLAYER_COLUMN, player.getUuid());
            return db.delete();
        });
    }

    public int deleteAlertsOlderThan(Instant cutoff) {
        return execute(() -> {
            DeleteBuilder<DatabaseAlert, UUID> db = dao.deleteBuilder();
            db.where().lt(CREATION_TIMESTAMP_COLUMN, Date.from(cutoff));
            return db.delete();
        });
    }

    public int deleteAllAlerts() {
        return execute(() -> dao.deleteBuilder().delete());
    }
}
