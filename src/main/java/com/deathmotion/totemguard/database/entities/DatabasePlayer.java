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

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "totemguard_player", indexes = @Index(columnList = "uuid", name = "idx_player_uuid"))
public class DatabasePlayer extends Model {
    @Id
    @Column(nullable = false, unique = true, length = 36)
    private UUID uuid;

    @Column(name = "client_brand", length = 63)
    private String clientBrand = "Unknown";

    @Column(nullable = false, updatable = false)
    @WhenCreated
    private Instant whenCreated;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatabaseAlert> alerts = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatabasePunishment> punishments = new ArrayList<>();
}

