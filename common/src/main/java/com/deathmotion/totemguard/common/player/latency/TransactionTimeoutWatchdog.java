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

package com.deathmotion.totemguard.common.player.latency;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.ScheduledTask;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Periodically scans every tracked player and disconnects anyone whose oldest
 * outstanding transaction packet hasn't been answered within the timeout window.
 * If a newer transaction id is later acknowledged, the older entries are drained
 * from the pending queue automatically (see {@code PendingTransactions}), so this
 * watchdog only fires when the channel is genuinely silent.
 */
public final class TransactionTimeoutWatchdog {

    private static final long TIMEOUT_MILLIS = 30_000L;
    private static final long PERIOD_SECONDS = 1L;

    private final PlayerRepositoryImpl playerRepository;
    private ScheduledTask task;

    public TransactionTimeoutWatchdog(PlayerRepositoryImpl playerRepository) {
        this.playerRepository = playerRepository;
    }

    public void start() {
        task = TGPlatform.getInstance().getScheduler().runAsyncTaskAtFixedRate(
                this::tick, PERIOD_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        try {
            for (TGPlayer player : playerRepository.getPlayers()) {
                long oldest = player.getPingData().getOldestPendingTransactionSentAt();
                if (oldest == 0L) continue;
                if (now - oldest < TIMEOUT_MILLIS) continue;
                player.timedOut();
            }
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING, "Transaction timeout watchdog tick failed", ex);
        }
    }
}
