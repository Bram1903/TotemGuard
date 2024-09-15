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
import com.deathmotion.totemguard.data.CheckDetails;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.deathmotion.totemguard.database.entities.impl.Alert;
import com.deathmotion.totemguard.database.entities.impl.Punishment;
import io.ebean.Database;
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

@Blocking
public class DatabaseService {
    private static final int SAVE_INTERVAL_SECONDS = 10;
    private static final String UUID_FIELD = "uuid";
    private static final String WHEN_CREATED_FIELD = "whenCreated";

    private final TotemGuard plugin;
    private final Database database;
    private final ZoneId zoneId;
    private final BlockingQueue<Alert> alertsToSave = new LinkedBlockingQueue<>();
    private final BlockingQueue<Punishment> punishmentsToSave = new LinkedBlockingQueue<>();

    public DatabaseService(TotemGuard plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager().getDatabase();
        this.zoneId = ZoneId.systemDefault();

        // Schedule the bulkSave task
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o -> bulkSave()), SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Periodically saves all queued alerts and punishments to the database.
     */
    private void bulkSave() {
        List<Alert> alertsSnapshot = new ArrayList<>();
        List<Punishment> punishmentsSnapshot = new ArrayList<>();

        // Drain the queues into the snapshots
        alertsToSave.drainTo(alertsSnapshot);
        punishmentsToSave.drainTo(punishmentsSnapshot);

        try {
            if (!alertsSnapshot.isEmpty()) {
                database.saveAll(alertsSnapshot);
            }
            if (!punishmentsSnapshot.isEmpty()) {
                database.saveAll(punishmentsSnapshot);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save data to the database: " + e.getMessage());
            // Ensure atomic re-adding
            synchronized (this) {
                alertsToSave.addAll(alertsSnapshot);
                punishmentsToSave.addAll(punishmentsSnapshot);
            }
        }
    }

    /**
     * Saves an alert to the queue for asynchronous saving.
     */
    public void saveAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        Alert alert = createAlert(totemPlayer, checkDetails);
        if (!alertsToSave.offer(alert)) {
            plugin.getLogger().severe("Failed to enqueue alert for player: " + totemPlayer.getUsername());
        }
    }

    /**
     * Saves a punishment to the queue for asynchronous saving.
     */
    public void savePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        Punishment punishment = createPunishment(totemPlayer, checkDetails);
        if (!punishmentsToSave.offer(punishment)) {
            plugin.getLogger().severe("Failed to enqueue punishment for player: " + totemPlayer.getUsername());
        }
    }

    /**
     * Retrieves all alerts from the database.
     */
    public List<Alert> getAlerts() {
        return database.find(Alert.class).findList();
    }

    /**
     * Retrieves alerts for a specific UUID from the database.
     */
    public List<Alert> getAlerts(UUID uuid) {
        return database.find(Alert.class).where().eq(UUID_FIELD, uuid).findList();
    }

    /**
     * Retrieves all punishments from the database.
     */
    public List<Punishment> getPunishments() {
        return database.find(Punishment.class).findList();
    }

    /**
     * Retrieves punishments for a specific UUID from the database.
     */
    public List<Punishment> getPunishments(UUID uuid) {
        return database.find(Punishment.class).where().eq(UUID_FIELD, uuid).findList();
    }

    /**
     * Deletes alerts for a specific UUID from the database.
     */
    public int clearAlerts(UUID uuid) {
        return database.find(Alert.class).where().eq(UUID_FIELD, uuid).delete();
    }

    /**
     * Deletes punishments for a specific UUID from the database.
     */
    public int clearPunishments(UUID uuid) {
        return database.find(Punishment.class).where().eq(UUID_FIELD, uuid).delete();
    }

    /**
     * Trims the database by removing alerts and punishments older than 30 days.
     */
    public int trimDatabase() {
        Instant thirtyDaysAgo = LocalDateTime.now().minusDays(30).atZone(zoneId).toInstant();
        int deletedAlerts = database.find(Alert.class).where().lt(WHEN_CREATED_FIELD, thirtyDaysAgo).delete();
        int deletedPunishments = database.find(Punishment.class).where().lt(WHEN_CREATED_FIELD, thirtyDaysAgo).delete();
        return deletedAlerts + deletedPunishments;
    }

    /**
     * Clears all alerts and punishments from the database.
     */
    public int clearDatabase() {
        int deletedAlerts = database.find(Alert.class).delete();
        int deletedPunishments = database.find(Punishment.class).delete();
        return deletedAlerts + deletedPunishments;
    }

    /**
     * Creates an Alert object based on the provided player and check details.
     */
    private Alert createAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        Alert alert = new Alert();
        alert.setUuid(totemPlayer.getUuid());
        alert.setCheckName(checkDetails.getCheckName());
        return alert;
    }

    /**
     * Creates a Punishment object based on the provided player and check details.
     */
    private Punishment createPunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        Punishment punishment = new Punishment();
        punishment.setUuid(totemPlayer.getUuid());
        punishment.setCheckName(checkDetails.getCheckName());
        return punishment;
    }
}
