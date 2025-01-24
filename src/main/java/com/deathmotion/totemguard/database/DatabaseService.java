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
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.ebean.Database;
import io.ebean.Transaction;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseService {
    private static final int AUTOSAVE_PERIOD = 10;
    private static final String PLAYER_UUID_COLUMN = "uuid";
    private static final String CREATION_TIMESTAMP_COLUMN = "whenCreated";
    private static final String PLAYER_COLUMN = "player";

    private final TotemGuard plugin;
    private final Database database;
    private final ZoneId systemZone;
    private final BlockingQueue<BaseDomain> pendingEntities;

    /**
     * Initializes the DatabaseService, schedules periodic bulk saves, and sets up required references.
     *
     * @param plugin The main plugin instance.
     */
    public DatabaseService(TotemGuard plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager().getDatabase();
        this.systemZone = ZoneId.systemDefault();
        this.pendingEntities = new LinkedBlockingQueue<>();

        // Schedule asynchronous periodic saves
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(
                plugin,
                (o) -> performBulkSave(),
                AUTOSAVE_PERIOD,
                AUTOSAVE_PERIOD,
                TimeUnit.SECONDS
        );
    }

    /**
     * Aggregates all pending entities and saves them in one transaction.
     */
    private void performBulkSave() {
        List<BaseDomain> snapshot = new ArrayList<>();
        pendingEntities.drainTo(snapshot);

        if (snapshot.isEmpty()) {
            return;
        }

        try (Transaction tx = database.beginTransaction()) {
            database.saveAll(snapshot);
            tx.commit();
        } catch (Exception ex) {
            plugin.getLogger().severe("Error during bulk save operation: " + ex.getMessage());
        }
    }

    /**
     * Persists an alert record related to the specified check with the provided details.
     *
     * @param check   The check that triggered this alert.
     * @param details Additional information describing the alert.
     */
    @Blocking
    public void storeAlert(Check check, Component details) {
        DatabaseAlert newAlert = createAlertFromCheck(check, details);
        queueEntity(newAlert);
    }

    /**
     * Persists a punishment record related to the specified check.
     *
     * @param check The check that triggered this punishment.
     */
    @Blocking
    public void storePunishment(Check check) {
        DatabasePunishment newPunishment = createPunishmentFromCheck(check);
        queueEntity(newPunishment);
    }

    /**
     * Submits an entity to the queue for a batched, asynchronous save.
     *
     * @param entity The entity to save.
     */
    private void queueEntity(BaseDomain entity) {
        boolean offered = pendingEntities.offer(entity);
        if (!offered) {
            plugin.getLogger().severe(
                    "Failed to queue entity for saving. Player UUID: " + entity.getPlayer().getUuid()
            );
        }
    }

    /**
     * Creates a new {@link DatabaseAlert} from the data in a check and the supplied message details.
     *
     * @param check   The check containing basic alert information.
     * @param details Additional information about the alert, in component form.
     * @return A newly populated {@link DatabaseAlert}.
     */
    private DatabaseAlert createAlertFromCheck(Check check, Component details) {
        DatabasePlayer dbPlayer = check.getPlayer().databasePlayer;
        if (dbPlayer == null) {
            dbPlayer = retrieveOrRefreshPlayer(check.getPlayer());
        }

        DatabaseAlert alertEntity = new DatabaseAlert();
        alertEntity.setCheckName(check.getCheckName());
        alertEntity.setPlayer(dbPlayer);
        alertEntity.setDetails(GsonComponentSerializer.gson().serialize(details));

        // Maintain relationship consistency
        dbPlayer.getAlerts().add(alertEntity);
        return alertEntity;
    }

    /**
     * Creates a new {@link DatabasePunishment} from the data in a check.
     *
     * @param check The check containing punishment details.
     * @return A newly populated {@link DatabasePunishment}.
     */
    private DatabasePunishment createPunishmentFromCheck(Check check) {
        DatabasePlayer dbPlayer = check.getPlayer().databasePlayer;
        if (dbPlayer == null) {
            dbPlayer = retrieveOrRefreshPlayer(check.getPlayer());
        }

        DatabasePunishment punishmentEntity = new DatabasePunishment();
        punishmentEntity.setCheckName(check.getCheckName());
        punishmentEntity.setPlayer(dbPlayer);

        // Maintain relationship consistency
        dbPlayer.getPunishments().add(punishmentEntity);
        return punishmentEntity;
    }

    /**
     * Finds an existing player record by UUID or creates one if none exists, and updates its data.
     *
     * @param totemPlayer The in-memory representation of the player to update or insert.
     * @return The updated or newly created {@link DatabasePlayer}.
     */
    @Blocking
    public DatabasePlayer retrieveOrRefreshPlayer(TotemPlayer totemPlayer) {
        Optional<DatabasePlayer> found = database.find(DatabasePlayer.class)
                .where()
                .eq(PLAYER_UUID_COLUMN, totemPlayer.getUniqueId())
                .findOneOrEmpty();

        if (found.isPresent()) {
            DatabasePlayer existing = found.get();
            existing.setClientBrand(totemPlayer.getBrand());
            existing.save();
            return existing;
        }

        DatabasePlayer newPlayer = new DatabasePlayer();
        newPlayer.setUuid(totemPlayer.getUniqueId());
        newPlayer.setClientBrand(totemPlayer.getBrand());
        database.save(newPlayer);

        return newPlayer;
    }

    /**
     * Fetches an existing player record by UUID or inserts a default record if none is found.
     *
     * @param uuid The unique identifier of the player to look for.
     * @return The found or newly created {@link DatabasePlayer}.
     */
    private DatabasePlayer fetchOrCreatePlayer(UUID uuid) {
        return database.find(DatabasePlayer.class)
                .where()
                .eq(PLAYER_UUID_COLUMN, uuid)
                .findOneOrEmpty()
                .orElse(null);
    }

    /**
     * Retrieves all existing alerts in the database.
     *
     * @return A list of all {@link DatabaseAlert} entities.
     */
    @Blocking
    public List<DatabaseAlert> retrieveAlerts() {
        return database.find(DatabaseAlert.class).findList();
    }

    /**
     * Retrieves all existing punishments in the database.
     *
     * @return A list of all {@link DatabasePunishment} entities.
     */
    @Blocking
    public List<DatabasePunishment> retrievePunishments() {
        return database.find(DatabasePunishment.class).findList();
    }

    /**
     * Returns a pair containing all alerts and punishments associated with a given player UUID.
     *
     * @param uuid The unique identifier of the player.
     * @return A pair where the first list is alerts, and the second list is punishments.
     */
    @Blocking
    public Pair<List<DatabaseAlert>, List<DatabasePunishment>> retrieveLogs(UUID uuid) {
        DatabasePlayer player = fetchOrCreatePlayer(uuid);
        if (player == null) return null;

        List<DatabaseAlert> alerts = database.find(DatabaseAlert.class)
                .where()
                .eq(PLAYER_COLUMN, player)
                .findList();

        List<DatabasePunishment> punishments = database.find(DatabasePunishment.class)
                .where()
                .eq(PLAYER_COLUMN, player)
                .findList();

        return new Pair<>(alerts, punishments);
    }

    /**
     * Deletes all alerts and punishments associated with a specific player UUID.
     *
     * @param uuid The unique identifier of the player.
     * @return The total count of deleted entities (alerts + punishments).
     */
    @Blocking
    public int eraseLogs(UUID uuid) {
        DatabasePlayer player = fetchOrCreatePlayer(uuid);
        if (player == null) return -1;

        int removedAlerts = database.find(DatabaseAlert.class)
                .where()
                .eq(PLAYER_COLUMN, player)
                .delete();

        int removedPunishments = database.find(DatabasePunishment.class)
                .where()
                .eq(PLAYER_COLUMN, player)
                .delete();

        return removedAlerts + removedPunishments;
    }

    /**
     * Deletes logs (alerts and punishments) older than 30 days from the current date.
     *
     * @return The total number of deleted records.
     */
    @Blocking
    public int optimizeDatabase() {
        Instant cutoff = LocalDateTime.now().minusDays(30).atZone(systemZone).toInstant();

        int oldAlerts = database.find(DatabaseAlert.class)
                .where()
                .lt(CREATION_TIMESTAMP_COLUMN, cutoff)
                .delete();

        int oldPunishments = database.find(DatabasePunishment.class)
                .where()
                .lt(CREATION_TIMESTAMP_COLUMN, cutoff)
                .delete();

        return oldAlerts + oldPunishments;
    }

    /**
     * Removes all player, alert, and punishment records from the database.
     *
     * @return The total number of entities deleted.
     */
    @Blocking
    public int wipeDatabase() {
        int alertCount = database.find(DatabaseAlert.class).findCount();
        int punishmentCount = database.find(DatabasePunishment.class).findCount();
        int playersRemoved = database.find(DatabasePlayer.class).delete();

        return alertCount + punishmentCount + playersRemoved;
    }
}
