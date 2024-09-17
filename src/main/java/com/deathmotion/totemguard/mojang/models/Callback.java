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

package com.deathmotion.totemguard.mojang.models;


import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

@Getter
public class Callback {
    private final String username;
    private final UUID uuid;
    private final Component message;

    public Callback(String username, UUID uuid) {
        this.username = username;
        this.uuid = uuid;
        this.message = null;
    }

    public Callback(int responseStatus, String message) {
        switch (responseStatus) {
            case 400:
                this.username = null;
                this.uuid = null;
                this.message = Component.text(message, NamedTextColor.RED);
                break;
            case 404:
                this.username = null;
                this.uuid = null;
                this.message = Component.text("This user does not exist.", NamedTextColor.RED);
                break;
            case 429:
                this.username = null;
                this.uuid = null;
                this.message = Component.text(message, NamedTextColor.YELLOW);
                break;
            default:
                this.username = null;
                this.uuid = null;
                this.message = Component.text("An error occurred while processing your request.", NamedTextColor.RED);
                break;
        }
    }


}
