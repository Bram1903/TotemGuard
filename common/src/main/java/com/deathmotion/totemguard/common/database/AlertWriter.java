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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final long flushIntervalNs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread worker;
    private volatile long lastDropWarnNs = 0L;

    public AlertWriter(AlertDao alertDao, PlayerDao playerDao, StatsRollupDao statsRollupDao) {
        this.alertDao = alertDao;
        this.playerDao = playerDao;
        this.statsRollupDao = statsRollupDao;
        this.queue = new LinkedBlockingQueue<>(DatabaseTuning.BATCH_QUEUE_CAPACITY);
        this.batchMaxSize = DatabaseTuning.BATCH_MAX_SIZE;
        this.flushIntervalNs = TimeUnit.MILLISECONDS.toNanos(DatabaseTuning.BATCH_FLUSH_INTERVAL_MS);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        Thread t = new Thread(this::runLoop, "TotemGuard-DB-Writer");
        t.setDaemon(true);
        this.worker = t;
        t.start();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        Thread t = this.worker;
        this.worker = null;
        if (t != null) {
            t.interrupt();
            try {
                t.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        drainAndFlush();
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

    private void runLoop() {
        List<PendingAlert> batch = new ArrayList<>(batchMaxSize);
        while (running.get()) {
            try {
                PendingAlert head = queue.poll(flushIntervalNs, TimeUnit.NANOSECONDS);
                if (head != null) {
                    batch.add(head);
                    queue.drainTo(batch, batchMaxSize - batch.size());
                }
                if (!batch.isEmpty()) {
                    flush(batch);
                    batch.clear();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                TGPlatform.getInstance().getLogger().log(Level.SEVERE,
                        "Unexpected error in alert writer loop", t);
            }
        }
    }

    private void drainAndFlush() {
        if (queue.isEmpty()) return;
        List<PendingAlert> batch = new ArrayList<>();
        queue.drainTo(batch);
        if (!batch.isEmpty()) flush(batch);
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
            Map<Integer, Integer> latestFlaggedAtSeconds = new HashMap<>(batch.size() * 2);
            Map<Integer, Integer> alertsPerDay = new HashMap<>();
            for (PendingAlert alert : batch) {
                int seconds = alert.createdAtSeconds();
                latestFlaggedAtSeconds.merge(alert.playerId(), seconds, AlertWriter::maxUnsigned);
                int day = EpochSeconds.dayFromSeconds(seconds);
                alertsPerDay.merge(day, 1, Integer::sum);
            }

            playerDao.bumpLastFlaggedAt(latestFlaggedAtSeconds);
            for (Map.Entry<Integer, Integer> entry : alertsPerDay.entrySet()) {
                statsRollupDao.incrementAlerts(entry.getKey(), entry.getValue());
            }
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Alert batch persisted, but rollup updates failed for " + batch.size() + " row(s)", ex);
        }
    }

    private static int maxUnsigned(int a, int b) {
        return Integer.compareUnsigned(a, b) >= 0 ? a : b;
    }

    private void warnDrop() {
        long now = System.nanoTime();
        if (now - lastDropWarnNs < TimeUnit.SECONDS.toNanos(10)) return;
        lastDropWarnNs = now;
        TGPlatform.getInstance().getLogger().warning(
                "TotemGuard database queue is full, dropping alerts. Check DB health.");
    }
}
