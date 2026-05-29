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

package com.deathmotion.totemguard.common.redis.broker;

public enum MessagingTopic {

    ALERTS("alerts", true),
    FOCUS("focus", false),
    UPDATES("updates", false),
    PRESENCE("presence", false),
    EVENTS("events", false);

    public static final String PREFIX = "totemguard";

    private final String defaultName;
    private final boolean overridable;

    MessagingTopic(String defaultName, boolean overridable) {
        this.defaultName = defaultName;
        this.overridable = overridable;
    }

    public String channelName(String override) {
        String name = (overridable && override != null && !override.isBlank())
                ? override.trim()
                : defaultName;
        return PREFIX + ":" + name;
    }
}
