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

package com.deathmotion.totemguard.common.replay.format;

import java.io.IOException;
import java.util.*;

final class PayloadDictionary {

    private final Map<Key, Integer> indexes = new HashMap<>();
    private final List<byte[]> payloads = new ArrayList<>();
    private long bytes;

    static boolean eligible(byte[] payload) {
        return payload.length >= ReplayFormat.REPEAT_MIN_PAYLOAD;
    }

    int lookup(byte[] payload) {
        Integer index = indexes.get(new Key(payload));
        return index == null ? -1 : index;
    }

    void remember(byte[] payload) {
        if (payloads.size() >= ReplayFormat.REPEAT_MAX_ENTRIES) return;
        if (bytes + payload.length > ReplayFormat.REPEAT_MAX_BYTES) return;
        byte[] owned = payload.clone();
        indexes.putIfAbsent(new Key(owned), payloads.size());
        payloads.add(owned);
        bytes += owned.length;
    }

    byte[] resolve(int index) throws IOException {
        if (index < 0 || index >= payloads.size()) {
            throw new IOException("Recording references payload " + index + " of " + payloads.size());
        }
        return payloads.get(index).clone();
    }

    private static final class Key {

        private final byte[] payload;
        private final int hash;

        private Key(byte[] payload) {
            this.payload = payload;
            this.hash = Arrays.hashCode(payload);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && hash == key.hash && Arrays.equals(payload, key.payload);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
