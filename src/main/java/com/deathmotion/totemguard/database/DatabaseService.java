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

package com.deathmotion.totemguard.database;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.database.entities.BaseDomain;
import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.ebean.Database;
import io.ebean.Transaction;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.jetbrains.annotations.Blocking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseService {
    private static final int SAVE_INTERVAL_SECONDS = 10;
    private static final String UUID_FIELD = "uuid";
    private static final String WHEN_CREATED_FIELD = "whenCreated";
    private static final String PLAYER_FIELD = "player";

    private final TotemGuard plugin;
    private final Database database;
    private final ZoneId zoneId;
    private final BlockingQueue<BaseDomain> entitiesToSave = new LinkedBlockingQueue<>();

    public DatabaseService(TotemGuard plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager().getDatabase();
        this.zoneId = ZoneId.systemDefault();

        // Periodic bulk save
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o -> bulkSave()), SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void bulkSave() {
        List<BaseDomain> entitiesSnapshot = new ArrayList<>();
        entitiesToSave.drainTo(entitiesSnapshot);

        try (Transaction transaction = database.beginTransaction()) {
            if (!entitiesSnapshot.isEmpty()) {
                database.saveAll(entitiesSnapshot);
            }
            transaction.commit();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save entities to the database: " + e.getMessage());
        }
    }

    @Blocking
    public void saveAlert(Check check) {
        DatabaseAlert alert = createAlert(check);
        saveEntity(alert);
    }

    @Blocking
    public void savePunishment(Check check) {
        DatabasePunishment punishment = createPunishment(check);
        saveEntity(punishment);
    }

    private void saveEntity(BaseDomain entity) {
        if (!entitiesToSave.offer(entity)) {
            plugin.getLogger().severe("Failed to enqueue entity for player: " + entity.getPlayer().getUuid());
        }
    }

    private DatabaseAlert createAlert(Check check) {
        DatabasePlayer databasePlayer = check.getPlayer().databasePlayer;

        DatabaseAlert alert = new DatabaseAlert();
        alert.setCheckName(check.getCheckName());
        alert.setPlayer(databasePlayer);

        // Automatically handle bidirectional relationship by adding alert to player's alert list
        databasePlayer.getAlerts().add(alert);
        return alert;
    }

    private DatabasePunishment createPunishment(Check check) {
        DatabasePlayer databasePlayer = check.getPlayer().databasePlayer;

        DatabasePunishment punishment = new DatabasePunishment();
        punishment.setCheckName(check.getCheckName());
        punishment.setPlayer(databasePlayer);

        // Automatically handle bidirectional relationship by adding punishment to player's punishment list
        databasePlayer.getPunishments().add(punishment);
        return punishment;
    }

    @Blocking
    public DatabasePlayer getOrCreatePlayer(UUID uuid) {
        return database.find(DatabasePlayer.class)
                .where()
                .eq(UUID_FIELD, uuid)
                .findOneOrEmpty()
                .orElseGet(() -> {
                    DatabasePlayer newPlayer = new DatabasePlayer();
                    newPlayer.setUuid(uuid);
                    database.save(newPlayer);
                    return newPlayer;
                });
    }

    @Blocking
    public List<DatabaseAlert> getAlerts() {
        return database.find(DatabaseAlert.class).findList();
    }

    @Blocking
    public List<DatabasePunishment> getPunishments() {
        return database.find(DatabasePunishment.class).findList();
    }

    @Blocking
    public Pair<List<DatabaseAlert>, List<DatabasePunishment>> getLogs(UUID uuid) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(uuid);

        List<DatabaseAlert> alerts = database.find(DatabaseAlert.class)
                .where()
                .eq(PLAYER_FIELD, databasePlayer)
                .findList();

        List<DatabasePunishment> punishments = database.find(DatabasePunishment.class)
                .where()
                .eq(PLAYER_FIELD, databasePlayer)
                .findList();

        return new Pair<>(alerts, punishments);
    }

    @Blocking
    public int clearLogs(UUID uuid) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(uuid);

        int deletedAlerts = database.find(DatabaseAlert.class)
                .where()
                .eq(PLAYER_FIELD, databasePlayer)
                .delete();
        int deletedPunishments = database.find(DatabasePunishment.class)
                .where()
                .eq(PLAYER_FIELD, databasePlayer)
                .delete();

        return deletedAlerts + deletedPunishments;
    }

    @Blocking
    public int trimDatabase() {
        Instant thirtyDaysAgo = LocalDateTime.now().minusDays(30).atZone(zoneId).toInstant();

        int deletedAlerts = database.find(DatabaseAlert.class)
                .where()
                .lt(WHEN_CREATED_FIELD, thirtyDaysAgo)
                .delete();

        int deletedPunishments = database.find(DatabasePunishment.class)
                .where()
                .lt(WHEN_CREATED_FIELD, thirtyDaysAgo)
                .delete();

        return deletedAlerts + deletedPunishments;
    }

    @Blocking
    public int clearDatabase() {
        int totalAlerts = database.find(DatabaseAlert.class).findCount();
        int totalPunishments = database.find(DatabasePunishment.class).findCount();
        int deletedPlayers = database.find(DatabasePlayer.class).delete();

        return totalAlerts + totalPunishments + deletedPlayers;
    }
}
