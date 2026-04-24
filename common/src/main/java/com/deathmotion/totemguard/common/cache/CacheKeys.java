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

package com.deathmotion.totemguard.common.cache;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CacheKeys {

    private static final String PREFIX = "totemguard:";

    private CacheKeys() {
    }

    public static String vpn(String ip) {
        return PREFIX + "vpn:" + ip;
    }

    public static String checkSnapshots(UUID uuid) {
        return PREFIX + "checks:" + uuid;
    }

    public static String alertsToggle(UUID uuid) {
        return PREFIX + "alerts-toggle:" + uuid;
    }

    public static String punishLock(UUID uuid) {
        return PREFIX + "punish-lock:" + uuid;
    }

    public static String alertHistoryPage(UUID uuid, int page, @Nullable String checkFilter) {
        String f = checkFilter == null ? "*" : checkFilter;
        return PREFIX + "hist:alerts:" + uuid + ":" + f + ":p" + page;
    }

    public static String alertHistoryCount(UUID uuid, @Nullable String checkFilter) {
        String f = checkFilter == null ? "*" : checkFilter;
        return PREFIX + "hist:alerts-count:" + uuid + ":" + f;
    }

    public static String alertHistoryCheckSummaries(UUID uuid) {
        return PREFIX + "hist:alert-checks:" + uuid;
    }

    public static String punishmentHistoryPage(UUID uuid, int page) {
        return PREFIX + "hist:punishments:" + uuid + ":p" + page;
    }

    public static String punishmentHistoryCount(UUID uuid) {
        return PREFIX + "hist:punishments-count:" + uuid;
    }
}
