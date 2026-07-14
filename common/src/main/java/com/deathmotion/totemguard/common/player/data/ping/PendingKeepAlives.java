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

package com.deathmotion.totemguard.common.player.data.ping;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

final class PendingKeepAlives {

    private final Map<Long, Long> pending = new LinkedHashMap<>();

    void send(long id, long timestamp) {
        pending.put(id, timestamp);
    }

    PingReplyResult receive(long id, long timestamp) {
        int skipped = 0;
        Long sentAt = null;

        for (Iterator<Map.Entry<Long, Long>> iterator = pending.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, Long> entry = iterator.next();
            iterator.remove();

            if (entry.getKey() == id) {
                sentAt = entry.getValue();
                break;
            }

            skipped++;
        }

        if (sentAt == null) {
            return PingReplyResult.invalid();
        }

        int ping = PingData.clampPing(timestamp - sentAt);
        return new PingReplyResult(true, skipped > 0, skipped, ping, false);
    }
}
