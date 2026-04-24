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

import com.deathmotion.totemguard.api3.database.DatabaseRepository;
import com.deathmotion.totemguard.api3.punishment.PunishmentType;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.database.dao.AlertDao;
import com.deathmotion.totemguard.common.database.dao.CatalogDao;
import com.deathmotion.totemguard.common.database.dao.PlayerDao;
import com.deathmotion.totemguard.common.database.dao.PunishmentDao;
import com.deathmotion.totemguard.common.database.dao.SessionDao;
import com.deathmotion.totemguard.common.database.model.AlertCheckSummary;
import com.deathmotion.totemguard.common.database.model.AlertRecord;
import com.deathmotion.totemguard.common.database.model.PendingAlert;
import com.deathmotion.totemguard.common.database.model.PunishmentRecord;
import com.deathmotion.totemguard.common.database.schema.SchemaInitializer;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Entry point into the TotemGuard database layer.
 *
 * <p>Lifecycle mirrors {@code RedisRepositoryImpl}: constructed during
 * {@code commonOnEnable()}, {@link #start()} opens the pool and applies the
 * schema, {@link #restart()} re-reads config + reopens everything, and
 * {@link #stop()} drains pending writes and closes the pool. All database I/O
 * lives on either the writer thread or an async worker — callers on the main
 * or netty thread only ever hit in-memory queues.</p>
 */
public final class DatabaseRepositoryImpl implements DatabaseRepository {

    private static final long RECONNECT_INTERVAL_SECONDS = 60L;

    private final DatabaseConnectionManager connection = new DatabaseConnectionManager();

    private volatile @Nullable DatabaseOptions options;
    private volatile @Nullable ScheduledExecutorService reconnectExecutor;
    private volatile @Nullable CatalogDao catalogDao;
    private volatile @Nullable PlayerDao playerDao;
    private volatile @Nullable SessionDao sessionDao;
    private volatile @Nullable AlertDao alertDao;
    private volatile @Nullable PunishmentDao punishmentDao;
    private volatile @Nullable AlertWriter alertWriter;
    private volatile @Nullable RetentionSweeper retentionSweeper;

    public DatabaseRepositoryImpl() {
        start();
    }

    @Override
    public boolean isEnabled() {
        DatabaseOptions current = this.options;
        return current != null && current.isEnabled();
    }

    @Override
    public boolean isConnected() {
        return isEnabled() && connection.isConnected();
    }

    @Blocking
    public synchronized void start() {
        DatabaseOptions newOptions = new DatabaseOptions();
        this.options = newOptions;

        if (!newOptions.isEnabled()) {
            return;
        }

        if (!attemptInitialize(newOptions)) {
            scheduleReconnect();
        }
    }

    @Blocking
    public synchronized void stop() {
        stopReconnectScheduler();
        tearDown();
        this.options = null;
    }

    @Blocking
    public synchronized void restart() {
        stopReconnectScheduler();
        tearDown();
        start();
    }

    /**
     * Attempts to bring the pool + DAOs online. On failure, leaves state torn
     * down and returns {@code false} so the caller can schedule a retry.
     * Never throws — connection failures are expected and handled.
     */
    private boolean attemptInitialize(DatabaseOptions opts) {
        try {
            connection.start(opts);
            applySchema();

            CatalogDao catalog = new CatalogDao(connection);
            PlayerDao players = new PlayerDao(connection);
            SessionDao sessions = new SessionDao(connection);
            AlertDao alerts = new AlertDao(connection);
            PunishmentDao punishments = new PunishmentDao(connection);
            AlertWriter writer = new AlertWriter(alerts);
            RetentionSweeper sweeper = new RetentionSweeper(alerts, opts);

            int serverId = catalog.resolveAndCacheThisServerId(opts.getServerName());
            sessions.closeOrphanSessions(serverId, System.currentTimeMillis());

            this.catalogDao = catalog;
            this.playerDao = players;
            this.sessionDao = sessions;
            this.alertDao = alerts;
            this.punishmentDao = punishments;
            this.alertWriter = writer;
            this.retentionSweeper = sweeper;
            writer.start();
            sweeper.start();

            TGPlatform.getInstance().getLogger().info(
                    "Database ready (MySQL, serverId=" + serverId + ")");
            return true;
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Database initialization failed — will retry every "
                            + RECONNECT_INTERVAL_SECONDS + "s until reachable ("
                            + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
            tearDown();
            return false;
        }
    }

    private void scheduleReconnect() {
        if (reconnectExecutor != null) return;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TotemGuard-DB-Reconnect");
            t.setDaemon(true);
            return t;
        });
        this.reconnectExecutor = executor;
        executor.scheduleAtFixedRate(this::reconnectTick,
                RECONNECT_INTERVAL_SECONDS, RECONNECT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopReconnectScheduler() {
        ScheduledExecutorService current = this.reconnectExecutor;
        this.reconnectExecutor = null;
        if (current != null) current.shutdownNow();
    }

    private synchronized void reconnectTick() {
        DatabaseOptions opts = this.options;
        if (opts == null || !opts.isEnabled()) {
            stopReconnectScheduler();
            return;
        }
        if (connection.isConnected()) {
            stopReconnectScheduler();
            return;
        }
        if (attemptInitialize(opts)) {
            stopReconnectScheduler();
        }
    }

    /**
     * Records a session and returns {@code [playerId, sessionId]} as a long
     * pair — callers typically want both. Callers must only invoke this off
     * the main / netty thread.
     */
    @Blocking
    public long[] startSession(
            UUID uuid,
            String name,
            @Nullable String clientBrand,
            @Nullable Integer clientVersion,
            long startedAt
    ) throws SQLException {
        requireEnabled();
        PlayerDao players = this.playerDao;
        SessionDao sessions = this.sessionDao;
        CatalogDao catalog = this.catalogDao;
        if (players == null || sessions == null || catalog == null) {
            throw new SQLException("Database not ready");
        }
        int playerId = players.upsertAndResolveId(uuid, name, startedAt);
        long sessionId = sessions.insert(playerId, catalog.thisServerIdOrThrow(),
                name, clientBrand, clientVersion, startedAt);
        return new long[]{playerId, sessionId};
    }

    @Blocking
    public void endSession(long sessionId, long endedAt) throws SQLException {
        SessionDao sessions = this.sessionDao;
        if (sessions == null) return;
        sessions.markEnded(sessionId, endedAt);
    }

    /**
     * Non-blocking enqueue of an alert. Safe to call from any thread including
     * main/netty — it only touches in-memory structures. Resolution of the
     * check id (which can touch the DB on first sight) happens on the
     * scheduler, not the caller.
     *
     * @param playerId  surrogate tg_players.id from the login handshake, or
     *                  {@code 0} if the session has not finished opening —
     *                  in which case the alert is silently dropped (the
     *                  session insert is racing the flag, a rare early-login
     *                  edge case)
     */
    public void recordAlert(
            @Nullable Long sessionId,
            int playerId,
            String checkName,
            int violations,
            @Nullable String debug,
            @Nullable Integer keepalivePing,
            @Nullable Integer transactionPing,
            long createdAt
    ) {
        if (!isConnected()) return;
        if (playerId <= 0) return;
        AlertWriter writer = this.alertWriter;
        CatalogDao catalog = this.catalogDao;
        if (writer == null || catalog == null) return;

        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            try {
                int checkId = catalog.resolveCheckId(checkName);
                int serverId = catalog.thisServerIdOrThrow();
                writer.submit(new PendingAlert(
                        sessionId, playerId, serverId, checkId, violations, debug,
                        keepalivePing, transactionPing, createdAt));
            } catch (Exception ex) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING,
                        "Failed to enqueue alert for " + checkName, ex);
            }
        });
    }

    /**
     * Non-blocking enqueue of an executed punishment. Offloads the single
     * INSERT to the async scheduler — unlike alerts, punishments are rare
     * enough that dedicated batching would be overkill.
     */
    public void recordPunishment(
            @Nullable Long sessionId,
            int playerId,
            String checkName,
            PunishmentType type,
            String expandedCommand,
            @Nullable String debug,
            long createdAt
    ) {
        if (!isConnected()) return;
        if (playerId <= 0) return;
        PunishmentDao punishments = this.punishmentDao;
        CatalogDao catalog = this.catalogDao;
        if (punishments == null || catalog == null) return;

        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            try {
                int checkId = catalog.resolveCheckId(checkName);
                int serverId = catalog.thisServerIdOrThrow();
                punishments.insert(sessionId, playerId, serverId, checkId,
                        type, expandedCommand, debug, createdAt);
            } catch (Exception ex) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING,
                        "Failed to persist punishment for " + checkName, ex);
            }
        });
    }

    // ------------------------------------------------------------------
    // Read accessors used by the history screens. These run on whatever
    // thread the caller picked — GUI screens must dispatch them to the
    // scheduler's async executor before touching them.
    // ------------------------------------------------------------------

    @Blocking
    public List<AlertRecord> findAlertsByPlayer(UUID uuid, int limit, int offset) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.findByPlayer(uuid, limit, offset);
    }

    @Blocking
    public int countAlertsByPlayer(UUID uuid) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.countByPlayer(uuid);
    }

    @Blocking
    public List<AlertCheckSummary> findAlertCheckSummariesByPlayer(UUID uuid) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.findCheckSummariesByPlayer(uuid);
    }

    @Blocking
    public List<AlertRecord> findAlertsByPlayerAndCheck(UUID uuid, String checkName, int limit, int offset) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.findByPlayerAndCheck(uuid, checkName, limit, offset);
    }

    @Blocking
    public int countAlertsByPlayerAndCheck(UUID uuid, String checkName) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.countByPlayerAndCheck(uuid, checkName);
    }

    @Blocking
    public List<PunishmentRecord> findPunishmentsByPlayer(UUID uuid, int limit, int offset) throws SQLException {
        requireEnabled();
        PunishmentDao punishments = this.punishmentDao;
        if (punishments == null) throw new SQLException("Database not ready");
        return punishments.findByPlayer(uuid, limit, offset);
    }

    @Blocking
    public int countPunishmentsByPlayer(UUID uuid) throws SQLException {
        requireEnabled();
        PunishmentDao punishments = this.punishmentDao;
        if (punishments == null) throw new SQLException("Database not ready");
        return punishments.countByPlayer(uuid);
    }

    private void applySchema() throws SQLException {
        SchemaInitializer init = new SchemaInitializer();
        try (Connection c = connection.borrow()) {
            init.apply(c);
        }
    }

    private void tearDown() {
        RetentionSweeper sweeper = this.retentionSweeper;
        this.retentionSweeper = null;
        if (sweeper != null) sweeper.stop();

        AlertWriter writer = this.alertWriter;
        this.alertWriter = null;
        if (writer != null) writer.stop();

        CatalogDao catalog = this.catalogDao;
        if (catalog != null) catalog.resetCache();
        PlayerDao players = this.playerDao;
        if (players != null) players.resetCache();

        this.punishmentDao = null;
        this.alertDao = null;
        this.sessionDao = null;
        this.playerDao = null;
        this.catalogDao = null;

        connection.stop();
    }

    private void requireEnabled() throws SQLException {
        if (!isConnected()) throw new SQLException("Database is not connected");
    }
}
