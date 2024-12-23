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

package com.deathmotion.totemguard.bootstrap;

import lombok.Getter;

@Getter
public enum Library {
    CONFIGLIB("de.exlll", "configlib-yaml", "4.5.0"),
    DISCORD_WEBHOOK("club.minnced", "discord-webhooks", "0.8.0");

    private final String group;
    private final String name;
    private final String version;

    Library(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public String getMavenDependency() {
        return String.format("%s:%s:%s", group, name, version);
    }
}