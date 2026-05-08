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

package com.deathmotion.totemguard.common.features.check;

import com.deathmotion.totemguard.common.platform.player.ManualCheckHandle;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class ActiveCheck {

    private final CheckService owner;
    private final ResultReporter reporter;
    private final TGPlayer target;
    private final int durationMs;
    private final String staffName;

    private final AtomicBoolean concluded = new AtomicBoolean();
    private final AtomicBoolean deadlineArmed = new AtomicBoolean();
    private final AtomicReference<ManualCheckHandle> handleRef = new AtomicReference<>();

    ActiveCheck(CheckService owner, ResultReporter reporter, TGPlayer target, int durationMs, String staffName) {
        this.owner = owner;
        this.reporter = reporter;
        this.target = target;
        this.durationMs = durationMs;
        this.staffName = staffName;
    }

    void installHandle(ManualCheckHandle handle) {
        handleRef.set(handle);
        if (concluded.get()) {
            ManualCheckHandle pending = handleRef.getAndSet(null);
            if (pending != null) pending.restore();
            return;
        }

        owner.platform.getScheduler().runAsyncTaskDelayed(() -> {
            if (deadlineArmed.get()) return;
            resolve(false, 0L);
        }, CheckService.FAILSAFE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    void armDeadline() {
        if (!deadlineArmed.compareAndSet(false, true)) return;
        owner.platform.getScheduler().runAsyncTaskDelayed(
                () -> resolve(false, durationMs),
                durationMs,
                TimeUnit.MILLISECONDS
        );
    }

    void observeReplenish(long elapsedMs) {
        if (elapsedMs < 0L) return;
        if (!target.getInventory().isTotemInSlot(InventoryConstants.SLOT_OFFHAND)) return;
        resolve(elapsedMs <= durationMs, elapsedMs);
    }

    void abort() {
        if (!concluded.compareAndSet(false, true)) return;
        cleanup();
        owner.cooldownUntil.remove(target.getUuid());
    }

    private void resolve(boolean flagged, long elapsedMs) {
        if (!concluded.compareAndSet(false, true)) return;

        cleanup();
        owner.cooldownUntil.put(target.getUuid(), System.currentTimeMillis() + CheckService.COOLDOWN_GRACE_MS);
        if (flagged) {
            reporter.reportFlagged(target, elapsedMs, durationMs, staffName);
        } else {
            reporter.reportPassed(target, elapsedMs, durationMs);
        }
    }

    private void cleanup() {
        owner.active.remove(target.getUuid(), this);
        ManualCheckHandle handle = handleRef.getAndSet(null);
        if (handle != null) handle.restore();
        target.setManualCheckActive(false);
    }
}
