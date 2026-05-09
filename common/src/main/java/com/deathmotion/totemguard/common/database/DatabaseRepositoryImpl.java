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

import com.deathmotion.totemguard.api.database.DatabaseRepository;
import com.deathmotion.totemguard.api.punishment.PunishmentType;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.DatabaseOptions;
import com.deathmotion.totemguard.common.database.dao.*;
import com.deathmotion.totemguard.common.database.model.*;
import com.deathmotion.totemguard.common.database.schema.SchemaInitializer;
import com.deathmotion.totemguard.common.database.util.DebugTemplate;
import com.deathmotion.totemguard.common.database.util.EpochSeconds;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class DatabaseRepositoryImpl implements DatabaseRepository {

    private static final long RECONNECT_INTERVAL_SECONDS = 60L;

    private final DatabaseConnectionManager connection = new DatabaseConnectionManager();

    private volatile @Nullable DatabaseOptions options;
    private volatile @Nullable ScheduledTask reconnectTask;
    private volatile @Nullable CatalogDao catalogDao;
    private volatile @Nullable PlayerDao playerDao;
    private volatile @Nullable ProfileDao profileDao;
    private volatile @Nullable AlertDao alertDao;
    private volatile @Nullable PunishmentDao punishmentDao;
    private volatile @Nullable StaffAlertPrefDao staffAlertPrefDao;
    private volatile @Nullable SchemaInfoDao schemaInfoDao;
    private volatile @Nullable StatsRollupDao statsRollupDao;
    private volatile @Nullable AlertWriter alertWriter;
    private volatile @Nullable RetentionSweeper retentionSweeper;

    public DatabaseRepositoryImpl() {
        start();
    }

    @Override
    public boolean isEnabled() {
        DatabaseOptions current = this.options;
        return current != null && current.enabled();
    }

    @Override
    public boolean isConnected() {
        return isEnabled() && connection.isConnected();
    }

    @Blocking
    public synchronized void start() {
        DatabaseOptions newOptions = TGPlatform.getInstance().getConfigRepository().configView().database();
        this.options = newOptions;

        if (!newOptions.enabled()) {
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

    private boolean attemptInitialize(DatabaseOptions opts) {
        try {
            connection.start(opts);
            applySchema();

            CatalogDao catalog = new CatalogDao(connection);
            PlayerDao players = new PlayerDao(connection);
            ProfileDao profiles = new ProfileDao(connection);
            AlertDao alerts = new AlertDao(connection);
            PunishmentDao punishments = new PunishmentDao(connection);
            StaffAlertPrefDao staffPrefs = new StaffAlertPrefDao(connection);
            SchemaInfoDao schemaInfo = new SchemaInfoDao(connection);
            StatsRollupDao statsRollup = new StatsRollupDao(connection);
            AlertWriter writer = new AlertWriter(alerts, players, statsRollup);
            RetentionSweeper sweeper = new RetentionSweeper(alerts, opts);

            this.catalogDao = catalog;
            this.playerDao = players;
            this.profileDao = profiles;
            this.alertDao = alerts;
            this.punishmentDao = punishments;
            this.staffAlertPrefDao = staffPrefs;
            this.schemaInfoDao = schemaInfo;
            this.statsRollupDao = statsRollup;
            this.alertWriter = writer;
            this.retentionSweeper = sweeper;
            writer.start();
            sweeper.start();

            // Always register a tg_servers row up front so alerts have somewhere to land
            // even if the configured placeholder never resolves. If the placeholder is
            // already resolved (static config, or db is restarting after the listener
            // fired) we use the real name; otherwise we fall back to the raw config value
            // (e.g. literal "%mcpvp_server_id%") — stable across restarts so it doesn't
            // spam new rows. The listener wired in TGPlatform swaps to the real name once
            // resolution succeeds.
            NetworkPresenceRepository presence = TGPlatform.getInstance().getNetworkPresenceRepository();
            String dbName = (presence != null && presence.isServerNameResolved())
                    ? presence.getLocalServerName()
                    : opts.serverName();
            assignThisServerName(dbName);
            return true;
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Database initialization failed, will retry every "
                            + RECONNECT_INTERVAL_SECONDS + "s until reachable ("
                            + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
            tearDown();
            return false;
        }
    }

    /**
     * Assigns (and persists) the server name to {@code tg_servers}, caching the resolved
     * id for subsequent profile/alert writes. Safe to call multiple times — idempotent.
     * Invoked by {@link NetworkPresenceRepository}'s server-name-resolved listener so the
     * fallback {@code tg-<hex>} name is never written to the table.
     */
    public synchronized void assignThisServerName(@NotNull String serverName) {
        CatalogDao catalog = this.catalogDao;
        if (catalog == null) return;
        try {
            int serverId = catalog.resolveAndCacheThisServerId(serverName);
            TGPlatform.getInstance().getLogger().info(
                    "Database server registered (name=\"" + serverName + "\", serverId=" + serverId + ")");
            backfillOnlineProfiles();
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Failed to register database server name \"" + serverName + "\"", ex);
        }
    }

    private void scheduleReconnect() {
        if (reconnectTask != null) return;
        this.reconnectTask = TGPlatform.getInstance().getScheduler().runAsyncTaskAtFixedRate(
                this::reconnectTick, RECONNECT_INTERVAL_SECONDS, RECONNECT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopReconnectScheduler() {
        ScheduledTask current = this.reconnectTask;
        this.reconnectTask = null;
        if (current != null) current.cancel();
    }

    private void backfillOnlineProfiles() {
        TGPlatform platform = TGPlatform.getInstance();
        if (platform.getPlayerRepository() == null) return;
        platform.getScheduler().runAsyncTask(() -> platform.getPlayerRepository().backfillDatabaseProfiles());
    }

    private synchronized void reconnectTick() {
        DatabaseOptions opts = this.options;
        if (opts == null || !opts.enabled()) {
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

    @Blocking
    public long[] resolveProfile(UUID uuid, String name, String clientBrand, int clientVersion, long nowEpochMs)
            throws SQLException {
        requireEnabled();
        PlayerDao players = this.playerDao;
        ProfileDao profiles = this.profileDao;
        CatalogDao catalog = this.catalogDao;
        if (players == null || profiles == null || catalog == null) {
            throw new SQLException("Database not ready");
        }
        int playerId = players.upsertAndResolveId(uuid, name, nowEpochMs);
        int brandId = catalog.resolveBrandId(clientBrand);
        long profileId = profiles.resolveOrCreate(
                playerId, catalog.thisServerIdOrThrow(), brandId, clientVersion);
        return new long[]{playerId, profileId};
    }

    @Blocking
    public @Nullable PlayerRecord findPlayerByName(String name) throws SQLException {
        requireEnabled();
        PlayerDao players = this.playerDao;
        if (players == null) throw new SQLException("Database not ready");
        return players.findByName(name);
    }

    @Blocking
    public @Nullable PlayerRecord findPlayerByUuid(UUID uuid) throws SQLException {
        requireEnabled();
        PlayerDao players = this.playerDao;
        if (players == null) throw new SQLException("Database not ready");
        return players.findByUuid(uuid);
    }

    public void recordAlert(@Nullable Long profileId,
                            int playerId,
                            String checkName,
                            @Nullable String debug,
                            long createdAt) {
        recordAlert(profileId, playerId, checkName, debug, null, createdAt);
    }

    /**
     * @param compiledDebug optional precompiled (template, args). When non-null the
     *                      auto numeric extractor is skipped and the supplied template
     *                      is interned directly.
     */
    public void recordAlert(@Nullable Long profileId,
                            int playerId,
                            String checkName,
                            @Nullable String debug,
                            @Nullable DebugTemplate.Compiled compiledDebug,
                            long createdAt) {
        if (!isConnected()) return;
        if (playerId <= 0 || profileId == null) return;
        AlertWriter writer = this.alertWriter;
        CatalogDao catalog = this.catalogDao;
        if (writer == null || catalog == null) return;

        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            try {
                int checkId = catalog.resolveCheckId(checkName);
                DebugTemplate.Compiled compiled = compiledDebug != null ? compiledDebug : DebugTemplate.compile(debug);
                Integer debugId = compiled == null ? null : catalog.resolveDebugTemplateId(compiled.template());
                String debugArgs = compiled == null ? null : compiled.args();
                writer.submit(new PendingAlert(
                        profileId, playerId, checkId,
                        debugId, debugArgs,
                        EpochSeconds.fromMillis(createdAt)));
            } catch (Exception ex) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING,
                        "Failed to enqueue alert for " + checkName, ex);
            }
        });
    }

    public void recordPunishment(@Nullable Long profileId,
                                 int playerId,
                                 String checkName,
                                 PunishmentType type,
                                 String commandTemplate,
                                 @Nullable String commandArgs,
                                 @Nullable String debug,
                                 @Nullable DebugTemplate.Compiled compiledDebug,
                                 long createdAt) {
        if (!isConnected()) return;
        if (playerId <= 0 || profileId == null) return;
        PunishmentDao punishments = this.punishmentDao;
        CatalogDao catalog = this.catalogDao;
        StatsRollupDao rollup = this.statsRollupDao;
        if (punishments == null || catalog == null || rollup == null) return;

        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            try {
                int checkId = catalog.resolveCheckId(checkName);
                int commandId = catalog.resolvePunishmentCommandId(commandTemplate);
                DebugTemplate.Compiled compiled = compiledDebug != null ? compiledDebug : DebugTemplate.compile(debug);
                Integer debugId = compiled == null ? null : catalog.resolveDebugTemplateId(compiled.template());
                String debugArgs = compiled == null ? null : compiled.args();
                punishments.insert(profileId, playerId, checkId, type,
                        commandId, commandArgs, debugId, debugArgs, createdAt);
                rollup.incrementPunishments(EpochSeconds.dayFromMillis(createdAt), 1);
            } catch (Exception ex) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING,
                        "Failed to persist punishment for " + checkName, ex);
            }
        });
    }

    @Blocking
    public @Nullable StaffAlertPref findStaffAlertPref(UUID uuid) throws SQLException {
        requireEnabled();
        StaffAlertPrefDao prefs = this.staffAlertPrefDao;
        if (prefs == null) throw new SQLException("Database not ready");
        return prefs.find(uuid);
    }

    @Blocking
    public void upsertStaffAlertPref(UUID uuid, boolean enabled, boolean localOnly) throws SQLException {
        requireEnabled();
        StaffAlertPrefDao prefs = this.staffAlertPrefDao;
        if (prefs == null) throw new SQLException("Database not ready");
        prefs.upsert(uuid, enabled, localOnly, System.currentTimeMillis());
    }

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
    public long[] deleteHistory(UUID uuid) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        PunishmentDao punishments = this.punishmentDao;
        if (alerts == null || punishments == null) throw new SQLException("Database not ready");

        long alertsRemoved = alerts.deleteByPlayer(uuid, 10_000);
        long punishmentsRemoved = punishments.deleteByPlayer(uuid, 10_000);
        return new long[]{alertsRemoved, punishmentsRemoved};
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

    @Blocking
    public StatsRollupDao.Totals statsTotalsAllTime() throws SQLException {
        requireEnabled();
        StatsRollupDao rollup = this.statsRollupDao;
        if (rollup == null) throw new SQLException("Database not ready");
        return rollup.sumAllTime();
    }

    @Blocking
    public StatsRollupDao.Totals statsTotalsSince(long sinceEpochMs) throws SQLException {
        requireEnabled();
        StatsRollupDao rollup = this.statsRollupDao;
        if (rollup == null) throw new SQLException("Database not ready");
        return rollup.sumSince(sinceEpochMs);
    }

    @Blocking
    public int countPlayersTotal() throws SQLException {
        requireEnabled();
        PlayerDao players = this.playerDao;
        if (players == null) throw new SQLException("Database not ready");
        return players.countAll();
    }

    @Blocking
    public int countPlayersActiveSince(long sinceEpochMs) throws SQLException {
        requireEnabled();
        PlayerDao players = this.playerDao;
        if (players == null) throw new SQLException("Database not ready");
        return players.countActiveSince(sinceEpochMs);
    }

    @Blocking
    public int countPlayersFlaggedTotal() throws SQLException {
        requireEnabled();
        PlayerDao players = this.playerDao;
        if (players == null) throw new SQLException("Database not ready");
        return players.countFlaggedTotal();
    }

    @Blocking
    public int countPlayersFlaggedSince(long sinceEpochMs) throws SQLException {
        requireEnabled();
        PlayerDao players = this.playerDao;
        if (players == null) throw new SQLException("Database not ready");
        return players.countFlaggedSince(sinceEpochMs);
    }

    @Blocking
    public java.util.Map<String, SchemaInfoDao.TableSize> tableSizes() throws SQLException {
        requireEnabled();
        SchemaInfoDao schema = this.schemaInfoDao;
        if (schema == null) throw new SQLException("Database not ready");
        return schema.tableSizes();
    }

    @Blocking
    public int countAlertsByPlayerSince(UUID uuid, long sinceEpochMs) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.countByPlayerSince(uuid, sinceEpochMs);
    }

    @Blocking
    public int countAlertsByPlayerAndCheckSince(UUID uuid, String checkName, long sinceEpochMs) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.countByPlayerAndCheckSince(uuid, checkName, sinceEpochMs);
    }

    @Blocking
    public List<PunishmentRecord> findPunishmentsByPlayerSince(UUID uuid, long sinceEpochMs, int limit, int offset) throws SQLException {
        requireEnabled();
        PunishmentDao punishments = this.punishmentDao;
        if (punishments == null) throw new SQLException("Database not ready");
        return punishments.findByPlayerSince(uuid, sinceEpochMs, limit, offset);
    }

    @Blocking
    public int countPunishmentsByPlayerSince(UUID uuid, long sinceEpochMs) throws SQLException {
        requireEnabled();
        PunishmentDao punishments = this.punishmentDao;
        if (punishments == null) throw new SQLException("Database not ready");
        return punishments.countByPlayerSince(uuid, sinceEpochMs);
    }

    @Blocking
    public List<AlertRecord> findAlertsByPlayerSince(UUID uuid, long sinceEpochMs, int limit, int offset) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.findByPlayerSince(uuid, sinceEpochMs, limit, offset);
    }

    @Blocking
    public List<AlertRecord> findAlertsByPlayerAndCheckSince(UUID uuid, String checkName, long sinceEpochMs, int limit, int offset) throws SQLException {
        requireEnabled();
        AlertDao alerts = this.alertDao;
        if (alerts == null) throw new SQLException("Database not ready");
        return alerts.findByPlayerAndCheckSince(uuid, checkName, sinceEpochMs, limit, offset);
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

        this.staffAlertPrefDao = null;
        this.schemaInfoDao = null;
        this.statsRollupDao = null;
        this.punishmentDao = null;
        this.alertDao = null;
        this.profileDao = null;
        this.playerDao = null;
        this.catalogDao = null;

        connection.stop();
    }

    private void requireEnabled() throws SQLException {
        if (!isConnected()) throw new SQLException("Database is not connected");
    }
}
