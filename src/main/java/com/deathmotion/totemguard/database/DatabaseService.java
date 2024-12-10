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
import com.deathmotion.totemguard.api.models.TotemPlayer;
import com.deathmotion.totemguard.database.entities.BaseDomain;
import com.deathmotion.totemguard.database.entities.Check;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.entities.impl.Alert;
import com.deathmotion.totemguard.database.entities.impl.Punishment;
import com.deathmotion.totemguard.models.checks.CheckDetails;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.ebean.Database;
import io.ebean.Transaction;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.jodah.expiringmap.ExpiringMap;
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
    private static final int CACHE_EXPIRY_MINUTES = 10;
    private static final String UUID_FIELD = "uuid";
    private static final String WHEN_CREATED_FIELD = "whenCreated";
    private static final String DATABASE_PLAYER_FIELD = "databasePlayer";

    private final TotemGuard plugin;
    private final Database database;
    private final ZoneId zoneId;
    private final BlockingQueue<BaseDomain> entitiesToSave = new LinkedBlockingQueue<>();
    private final ExpiringMap<UUID, DatabasePlayer> playerCache = ExpiringMap.builder()
            .expiration(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
            .build();

    public DatabaseService(TotemGuard plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager().getDatabase();
        this.zoneId = ZoneId.systemDefault();

        // Periodic bulk save
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o -> bulkSave()), SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Periodically saves all queued alerts and punishments to the database.
     */
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

    /**
     * Saves an alert to the queue for asynchronous saving.
     */
    public void saveAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(totemPlayer.uuid());
        Alert alert = createAlert(databasePlayer, checkDetails);
        saveEntity(alert);
    }

    /**
     * Saves a punishment to the queue for asynchronous saving.
     */
    public void savePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(totemPlayer.uuid());
        Punishment punishment = createPunishment(databasePlayer, checkDetails);
        saveEntity(punishment);
    }

    /**
     * Adds an entity to the saving queue.
     */
    private void saveEntity(BaseDomain entity) {
        if (!entitiesToSave.offer(entity)) {
            plugin.getLogger().severe("Failed to enqueue entity for player: " + entity.getDatabasePlayer().getUuid());
        }
    }

    /**
     * Creates and returns an Alert object, adding it to the player's alert list.
     */
    private Alert createAlert(DatabasePlayer databasePlayer, CheckDetails checkDetails) {
        Alert alert = new Alert();
        alert.setCheckName(Check.valueOf(checkDetails.getCheckName()));
        alert.setDatabasePlayer(databasePlayer);

        // Automatically handle bidirectional relationship by adding alert to player's alert list
        databasePlayer.getAlerts().add(alert);
        return alert;
    }

    /**
     * Creates and returns a Punishment object, adding it to the player's punishment list.
     */
    private Punishment createPunishment(DatabasePlayer databasePlayer, CheckDetails checkDetails) {
        Punishment punishment = new Punishment();
        punishment.setCheckName(Check.valueOf(checkDetails.getCheckName()));
        punishment.setDatabasePlayer(databasePlayer);

        // Automatically handle bidirectional relationship by adding punishment to player's punishment list
        databasePlayer.getPunishments().add(punishment);
        return punishment;
    }

    /**
     * Retrieves the DatabasePlayer by UUID, using a cache with a 10-minute expiration.
     * If the player is not found, it creates a new entry.
     */
    private DatabasePlayer getOrCreatePlayer(UUID uuid) {
        // Check cache first
        DatabasePlayer cachedPlayer = playerCache.get(uuid);
        if (cachedPlayer != null) {
            return cachedPlayer;
        }

        // If not in cache, retrieve or create from the database
        DatabasePlayer databasePlayer = database.find(DatabasePlayer.class)
                .where()
                .eq(UUID_FIELD, uuid)
                .findOneOrEmpty()
                .orElseGet(() -> {
                    DatabasePlayer newPlayer = new DatabasePlayer();
                    newPlayer.setUuid(uuid);
                    database.save(newPlayer);
                    return newPlayer;
                });

        // Cache the result
        playerCache.put(uuid, databasePlayer);
        return databasePlayer;
    }

    @Blocking
    public List<Alert> getAlerts() {
        return database.find(Alert.class).findList();
    }

    @Blocking
    public List<Punishment> getPunishments() {
        return database.find(Punishment.class).findList();
    }

    @Blocking
    public Pair<List<Alert>, List<Punishment>> getLogs(UUID uuid) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(uuid);

        List<Alert> alerts = database.find(Alert.class)
                .where()
                .eq(DATABASE_PLAYER_FIELD, databasePlayer)
                .findList();

        List<Punishment> punishments = database.find(Punishment.class)
                .where()
                .eq(DATABASE_PLAYER_FIELD, databasePlayer)
                .findList();

        return new Pair<>(alerts, punishments);
    }

    @Blocking
    public int clearLogs(UUID uuid) {
        DatabasePlayer databasePlayer = getOrCreatePlayer(uuid);

        int deletedAlerts = database.find(Alert.class)
                .where()
                .eq(DATABASE_PLAYER_FIELD, databasePlayer)
                .delete();
        int deletedPunishments = database.find(Punishment.class)
                .where()
                .eq(DATABASE_PLAYER_FIELD, databasePlayer)
                .delete();

        return deletedAlerts + deletedPunishments;
    }

    @Blocking
    public int trimDatabase() {
        Instant thirtyDaysAgo = LocalDateTime.now().minusDays(30).atZone(zoneId).toInstant();

        int deletedAlerts = database.find(Alert.class)
                .where()
                .lt(WHEN_CREATED_FIELD, thirtyDaysAgo)
                .delete();

        int deletedPunishments = database.find(Punishment.class)
                .where()
                .lt(WHEN_CREATED_FIELD, thirtyDaysAgo)
                .delete();

        return deletedAlerts + deletedPunishments;
    }

    @Blocking
    public int clearDatabase() {
        int totalAlerts = database.find(Alert.class).findCount();
        int totalPunishments = database.find(Punishment.class).findCount();
        int deletedPlayers = database.find(DatabasePlayer.class).delete();

        playerCache.clear();

        return totalAlerts + totalPunishments + deletedPlayers;
    }
}
