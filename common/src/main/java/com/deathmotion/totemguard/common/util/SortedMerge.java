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

package com.deathmotion.totemguard.common.util;

import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Linear merge of two individually sorted lists by a monotonic long key.
 * Stable: when keys are equal, elements from {@code a} win.
 * {@code null} lists are treated as empty.
 */
public final class SortedMerge {

    private SortedMerge() {
    }

    public static <T> void into(
            List<? super T> dest,
            List<? extends T> a,
            List<? extends T> b,
            ToLongFunction<? super T> keyFn
    ) {
        int aSize = a == null ? 0 : a.size();
        int bSize = b == null ? 0 : b.size();
        if (aSize == 0 && bSize == 0) return;
        if (aSize == 0) { dest.addAll(b); return; }
        if (bSize == 0) { dest.addAll(a); return; }

        int i = 0, j = 0;
        while (i < aSize && j < bSize) {
            T ai = a.get(i);
            T bj = b.get(j);
            if (keyFn.applyAsLong(ai) <= keyFn.applyAsLong(bj)) {
                dest.add(ai);
                i++;
            } else {
                dest.add(bj);
                j++;
            }
        }
        while (i < aSize) dest.add(a.get(i++));
        while (j < bSize) dest.add(b.get(j++));
    }
}
