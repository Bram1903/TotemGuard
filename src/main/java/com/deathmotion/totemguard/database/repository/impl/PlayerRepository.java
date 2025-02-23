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

import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.repository.BaseRepository;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;

import java.util.Optional;
import java.util.UUID;

public class PlayerRepository extends BaseRepository<DatabasePlayer, UUID> {

    private static final String PLAYER_UUID_COLUMN = "uuid";
    private final DatabaseProvider databaseProvider;

    public PlayerRepository(DatabaseProvider databaseProvider) {
        super(databaseProvider.getConnectionSource(), DatabasePlayer.class);
        this.databaseProvider = databaseProvider;
    }

    /**
     * Finds a player by their UUID.
     */
    public Optional<DatabasePlayer> findByUuid(UUID uuid) {
        return execute(() -> {
            QueryBuilder<DatabasePlayer, UUID> qb = createQueryBuilder();
            qb.where().eq(PLAYER_UUID_COLUMN, uuid);
            return executeQuery(qb).stream().findFirst();
        });
    }

    public DatabasePlayer retrieveOrRefreshPlayer(TotemPlayer totemPlayer) {
        // Attempt to find the player by UUID
        Optional<DatabasePlayer> found = findByUuid(totemPlayer.getUniqueId());
        if (found.isPresent()) {
            // Update existing record
            DatabasePlayer existing = found.get();
            existing.setClientBrand(totemPlayer.getBrand());
            save(existing); // (Will do createOrUpdate)
            return existing;
        }

        // Otherwise, create new record
        DatabasePlayer newPlayer = new DatabasePlayer();
        newPlayer.setUuid(totemPlayer.getUniqueId());
        newPlayer.setClientBrand(totemPlayer.getBrand());
        save(newPlayer);
        return newPlayer;
    }

    public DatabasePlayer fetchOrCreatePlayer(UUID uuid) {
        return findByUuid(uuid).orElse(null);
    }

    public int deleteAllPlayers() {
        return execute(() -> {
            DeleteBuilder<DatabasePlayer, UUID> db = dao.deleteBuilder();
            return db.delete();
        });
    }
}
