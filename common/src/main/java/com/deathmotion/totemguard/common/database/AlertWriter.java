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
import com.deathmotion.totemguard.common.database.model.PendingAlert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Single-threaded batched writer for alert rows.
 *
 * <p>Alert insertion is the hot database path: on a packed server it can fire
 * thousands of times per minute. Doing one INSERT per flag would saturate both
 * the connection pool and the DB itself, so we queue alerts from any thread
 * and flush them in batches from a dedicated worker thread.</p>
 *
 * <p>If the queue fills up (DB is wedged or slow), new alerts are dropped and
 * a warning is logged — this keeps the producer path lock-free and bounded.
 * Cache and chat notifications are unaffected.</p>
 */
public final class AlertWriter {

    private final AlertDao alertDao;
    private final LinkedBlockingQueue<PendingAlert> queue;
    private final int batchMaxSize;
    private final long flushIntervalNs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread worker;
    private volatile long lastDropWarnNs = 0L;

    public AlertWriter(AlertDao alertDao) {
        this.alertDao = alertDao;
        this.queue = new LinkedBlockingQueue<>(DatabaseOptions.BATCH_QUEUE_CAPACITY);
        this.batchMaxSize = DatabaseOptions.BATCH_MAX_SIZE;
        this.flushIntervalNs = TimeUnit.MILLISECONDS.toNanos(DatabaseOptions.BATCH_FLUSH_INTERVAL_MS);
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
        // Drain anything still buffered so no alert gets silently dropped on shutdown.
        drainAndFlush();
    }

    /**
     * Enqueues an alert for batched insertion. Returns {@code false} if the
     * queue was full and the alert was dropped.
     */
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
                    try {
                        alertDao.insertBatch(batch);
                    } catch (Exception ex) {
                        TGPlatform.getInstance().getLogger().log(Level.WARNING,
                                "Failed to flush " + batch.size() + " alert(s) to database", ex);
                    } finally {
                        batch.clear();
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                // Don't let an unexpected error kill the writer thread.
                TGPlatform.getInstance().getLogger().log(Level.SEVERE,
                        "Unexpected error in alert writer loop", t);
            }
        }
    }

    private void drainAndFlush() {
        if (queue.isEmpty()) return;
        List<PendingAlert> batch = new ArrayList<>();
        queue.drainTo(batch);
        if (batch.isEmpty()) return;
        try {
            alertDao.insertBatch(batch);
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Failed to drain " + batch.size() + " alert(s) on shutdown", ex);
        }
    }

    private void warnDrop() {
        long now = System.nanoTime();
        // Rate-limit the warning to one per 10s to avoid log flood.
        if (now - lastDropWarnNs < TimeUnit.SECONDS.toNanos(10)) return;
        lastDropWarnNs = now;
        TGPlatform.getInstance().getLogger().warning(
                "TotemGuard database queue is full — dropping alerts. " +
                        "Increase database.batching.queue-capacity or check DB health.");
    }
}
