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

package com.deathmotion.totemguard.common.database;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.database.dao.AlertDao;
import com.deathmotion.totemguard.common.database.dao.PlayerDao;
import com.deathmotion.totemguard.common.database.dao.StatsRollupDao;
import com.deathmotion.totemguard.common.database.model.PendingAlert;
import com.deathmotion.totemguard.common.database.util.EpochSeconds;
import com.deathmotion.totemguard.common.util.ScheduledTask;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class AlertWriter {

    private final AlertDao alertDao;
    private final PlayerDao playerDao;
    private final StatsRollupDao statsRollupDao;
    private final LinkedBlockingQueue<PendingAlert> queue;
    private final int batchMaxSize;
    private final long flushIntervalMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    private volatile ScheduledTask task;
    private volatile long lastDropWarnNs = 0L;

    public AlertWriter(AlertDao alertDao, PlayerDao playerDao, StatsRollupDao statsRollupDao) {
        this.alertDao = alertDao;
        this.playerDao = playerDao;
        this.statsRollupDao = statsRollupDao;
        this.queue = new LinkedBlockingQueue<>(DatabaseTuning.BATCH_QUEUE_CAPACITY);
        this.batchMaxSize = DatabaseTuning.BATCH_MAX_SIZE;
        this.flushIntervalMs = DatabaseTuning.BATCH_FLUSH_INTERVAL_MS;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        this.task = TGPlatform.getInstance().getScheduler().runAsyncTaskAtFixedRate(
                this::flushTick, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        ScheduledTask current = this.task;
        this.task = null;
        if (current != null) current.cancel();

        // The platform scheduler's cancel() doesn't wait for an in-flight tick.
        // Spin briefly until any concurrent flush completes, then drain ourselves.
        // Without this wait the synchronous flushTick() would hit the re-entrancy
        // guard and skip pending alerts on /tg reload.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (flushing.get() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        flushTick();
    }

    public boolean submit(PendingAlert alert) {
        if (!running.get()) return false;
        if (!queue.offer(alert)) {
            warnDrop();
            return false;
        }
        return true;
    }

    public int queueDepth() {
        return queue.size();
    }

    private void flushTick() {
        if (!flushing.compareAndSet(false, true)) return;
        try {
            while (true) {
                List<PendingAlert> batch = new ArrayList<>(Math.min(batchMaxSize, queue.size()));
                queue.drainTo(batch, batchMaxSize);
                if (batch.isEmpty()) return;
                flush(batch);
                if (batch.size() < batchMaxSize) return;
            }
        } finally {
            flushing.set(false);
        }
    }

    private void flush(List<PendingAlert> batch) {
        try {
            alertDao.insertBatch(batch);
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Failed to flush " + batch.size() + " alert(s) to database", ex);
            return;
        }

        try {
            Set<Integer> uniquePlayers = new HashSet<>(batch.size() * 2);
            Map<Integer, Integer> alertsPerDay = new HashMap<>();
            long maxFlaggedAtMs = 0L;
            for (PendingAlert alert : batch) {
                uniquePlayers.add(alert.playerId());
                int day = EpochSeconds.dayFromSeconds(alert.createdAtSeconds());
                alertsPerDay.merge(day, 1, Integer::sum);
                long ms = EpochSeconds.toMillis(alert.createdAtSeconds() & 0xFFFFFFFFL);
                if (ms > maxFlaggedAtMs) maxFlaggedAtMs = ms;
            }

            if (!uniquePlayers.isEmpty()) {
                playerDao.bumpLastFlaggedAt(uniquePlayers, maxFlaggedAtMs);
            }
            for (Map.Entry<Integer, Integer> entry : alertsPerDay.entrySet()) {
                statsRollupDao.incrementAlerts(entry.getKey(), entry.getValue());
            }
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Alert batch persisted, but rollup updates failed for " + batch.size() + " row(s)", ex);
        }
    }

    private void warnDrop() {
        long now = System.nanoTime();
        if (now - lastDropWarnNs < TimeUnit.SECONDS.toNanos(10)) return;
        lastDropWarnNs = now;
        TGPlatform.getInstance().getLogger().warning(
                "TotemGuard database queue is full, dropping alerts. Check DB health.");
    }
}
