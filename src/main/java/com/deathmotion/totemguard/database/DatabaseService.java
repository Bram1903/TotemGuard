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
import org.jetbrains.annotations.Blocking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Blocking
public class DatabaseService {
    private final Database database;
    private final ZoneId zoneId;

    public DatabaseService(TotemGuard plugin) {
        this.database = plugin.getDatabaseManager().getDatabase();
        this.zoneId = ZoneId.systemDefault();
    }

    public void saveAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        Alert alert = new Alert();
        alert.setUuid(totemPlayer.getUuid());
        alert.setUsername(totemPlayer.getUsername());
        alert.setCheckName(checkDetails.getCheckName());

        alert.save();
    }

    public void savePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        Punishment punishment = new Punishment();
        punishment.setUuid(totemPlayer.getUuid());
        punishment.setUsername(totemPlayer.getUsername());
        punishment.setCheckName(checkDetails.getCheckName());

        punishment.save();
    }

    public List<Alert> getAlerts() {
        return database.find(Alert.class).findList();
    }

    public List<Alert> getAlerts(UUID uuid) {
        return database.find(Alert.class).where().eq("uuid", uuid).findList();
    }

    public List<Punishment> getPunishments() {
        return database.find(Punishment.class).findList();
    }

    public List<Punishment> getPunishments(UUID uuid) {
        return database.find(Punishment.class).where().eq("uuid", uuid).findList();
    }

    public int clearAlerts(UUID uuid) {
        return database.find(Alert.class).where().eq("uuid", uuid).delete();
    }

    public int clearPunishments(UUID uuid) {
        return database.find(Punishment.class).where().eq("uuid", uuid).delete();
    }

    public int trimDatabase() {
        // Convert the LocalDateTime to an Instant using the system's default time zone
        Instant thirtyDaysAgo = LocalDateTime.now().minusDays(30).atZone(zoneId).toInstant();

        // Delete alerts older than 30 days
        int deletedAlerts = database.find(Alert.class)
                .where()
                .lt("whenCreated", thirtyDaysAgo)
                .delete();

        // Delete punishments older than 30 days
        int deletedPunishments = database.find(Punishment.class)
                .where()
                .lt("whenCreated", thirtyDaysAgo)
                .delete();

        return deletedAlerts + deletedPunishments;
    }

    public int clearDatabase() {
        int deletedAlerts = database.find(Alert.class).delete();
        int deletedPunishments = database.find(Punishment.class).delete();

        return deletedAlerts + deletedPunishments;
    }

}
