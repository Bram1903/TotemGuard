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
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.database.repository.BaseRepository;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PunishmentRepository extends BaseRepository<DatabasePunishment, UUID> {

    private static final String PLAYER_COLUMN = "totemguard_player_uuid";
    private static final String CREATION_TIMESTAMP_COLUMN = "when_created";
    private final DatabaseProvider databaseProvider;

    public PunishmentRepository(DatabaseProvider databaseProvider) {
        super(databaseProvider.getConnectionSource(), DatabasePunishment.class);
        this.databaseProvider = databaseProvider;
    }

    public void storePunishment(Check check) {
        DatabasePlayer dbPlayer = check.getPlayer().databasePlayer;
        if (dbPlayer == null) {
            dbPlayer = databaseProvider.getPlayerRepository().retrieveOrRefreshPlayer(check.getPlayer());
        }

        DatabasePunishment punishment = new DatabasePunishment();
        punishment.setCheckName(check.getCheckName());
        punishment.setWhenCreated(Date.from(Instant.now()));
        punishment.setPlayer(dbPlayer);

        save(punishment);
    }

    public List<DatabasePunishment> retrievePunishments() {
        return findAll();
    }

    public List<DatabasePunishment> findPunishmentsByPlayer(DatabasePlayer player) {
        return execute(() -> {
            QueryBuilder<DatabasePunishment, UUID> qb = createQueryBuilder();
            qb.where().eq(PLAYER_COLUMN, player.getUuid());
            return executeQuery(qb);
        });
    }

    public int deletePunishmentsByPlayer(DatabasePlayer player) {
        return execute(() -> {
            DeleteBuilder<DatabasePunishment, UUID> db = dao.deleteBuilder();
            db.where().eq(PLAYER_COLUMN, player.getUuid());
            return db.delete();
        });
    }

    public int deletePunishmentsOlderThan(Instant cutoff) {
        return execute(() -> {
            DeleteBuilder<DatabasePunishment, UUID> db = dao.deleteBuilder();
            db.where().lt(CREATION_TIMESTAMP_COLUMN, Date.from(cutoff));
            return db.delete();
        });
    }

    public int deleteAllPunishments() {
        return execute(() -> dao.deleteBuilder().delete());
    }
}
