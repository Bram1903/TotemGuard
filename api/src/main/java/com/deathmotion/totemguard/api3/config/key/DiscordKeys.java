/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.api3.config.key;

/**
 * Constants for {@code discord.yml}.
 * <p>
 * Per-channel settings (alerts/punishments) are read as nested sections under each prefix:
 * <pre>
 *   ConfigSection alerts = repo.config(ConfigFile.DISCORD)
 *           .getSection(DiscordKeys.ALERTS_PREFIX)
 *           .orElseThrow();
 *   boolean enabled = alerts.getBoolean("enabled").orElse(false);
 * </pre>
 * Defaults live in the bundled {@code discord.yml} resource.
 */
public final class DiscordKeys {

    public static final String ALERTS_PREFIX = "alerts";
    public static final String PUNISHMENTS_PREFIX = "punishments";

    private DiscordKeys() {
    }
}
