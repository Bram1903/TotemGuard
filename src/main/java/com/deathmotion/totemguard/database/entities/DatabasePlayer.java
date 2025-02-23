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

package com.deathmotion.totemguard.database.entities;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@DatabaseTable(tableName = "totemguard_player")
public class DatabasePlayer {

    @DatabaseField(canBeNull = false, unique = true, width = 36, id = true)
    private UUID uuid;

    @DatabaseField(columnName = "client_brand", width = 63)
    private String clientBrand = "Unknown";

    @DatabaseField(canBeNull = false, columnName = "when_created", index = true)
    private long whenCreated;

    public Instant getWhenCreated() {
        return Instant.ofEpochMilli(whenCreated);
    }

    public void setWhenCreated(Instant whenCreated) {
        this.whenCreated = whenCreated.toEpochMilli();
    }
}

