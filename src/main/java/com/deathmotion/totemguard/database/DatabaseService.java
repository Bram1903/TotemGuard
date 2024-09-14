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

import java.util.List;
import java.util.UUID;

public class DatabaseService {

    private final TotemGuard plugin;
    private final Database database;

    public DatabaseService(TotemGuard plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager().getDatabase();
    }

    public void saveAlert(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        Alert alert = new Alert();
        alert.setUuid(totemPlayer.getUuid());
        alert.setUsername(totemPlayer.getUsername());
        alert.setCheckName(checkDetails.getCheckName());

        alert.save();
    }

    public List<Alert> getAlerts(UUID uuid) {
        return database.find(Alert.class).where().eq("uuid", uuid).findList();
    }

    public List<Punishment> getPunishments(UUID uuid) {
        return database.find(Punishment.class).where().eq("uuid", uuid).findList();
    }

    public int deleteAlerts(UUID uuid) {
        return database.find(Alert.class).where().eq("uuid", uuid).delete();
    }
}
