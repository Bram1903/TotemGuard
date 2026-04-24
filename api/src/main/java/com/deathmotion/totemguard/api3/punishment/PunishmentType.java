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

package com.deathmotion.totemguard.api3.punishment;

/**
 * The intent of a single punishment command. Authors declare this inline by
 * prefixing their {@code punishment-commands} entries with {@code [TYPE]}:
 *
 * <pre>
 * punishment-commands:
 *   - "[BAN]  ban %tg_player% Unfair Advantage"
 *   - "[KICK] kick %tg_player% Packet Manipulation"
 *   - "[GENERIC] say %tg_player% was caught by TotemGuard"
 * </pre>
 *
 * <p>Untagged commands default to {@link #GENERIC}. The type is persisted
 * with each executed command so history queries (and future mod-kick
 * integrations) can group and filter accurately.</p>
 */
public enum PunishmentType {

    /** Announcements, logging, webhooks — anything that doesn't remove the player. */
    GENERIC,

    /** Removes the player from this server only. */
    KICK,

    /** Bans the player from the server / network. */
    BAN
}
