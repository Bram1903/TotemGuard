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
import com.deathmotion.totemguard.common.config.schema.DatabaseOptions;
import com.deathmotion.totemguard.common.database.dao.VpnCacheDao;
import com.deathmotion.totemguard.common.util.ScheduledTask;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class RetentionSweeper {

    private static final long INITIAL_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(1);
    private static final long PERIOD_SECONDS = TimeUnit.HOURS.toSeconds(6);
    private static final int VPN_DELETE_CHUNK_SIZE = 10_000;

    private static final int FUTURE_PARTITION_HORIZON_MONTHS = 6;

    private static final String FUTURE_PARTITION_NAME = "p_future";

    private final DatabaseConnectionManager connection;
    private final VpnCacheDao vpnCacheDao;
    private final int alertRetentionDays;
    private final int vpnRetentionDays;

    private ScheduledTask task;

    public RetentionSweeper(DatabaseConnectionManager connection, VpnCacheDao vpnCacheDao, DatabaseOptions options) {
        this.connection = connection;
        this.vpnCacheDao = vpnCacheDao;
        this.alertRetentionDays = Math.max(0, options.retentionAlertDays());
        this.vpnRetentionDays = Math.max(0, options.retentionVpnDays());
    }

    private static String partitionName(YearMonth ym) {
        return String.format("p%04d%02d", ym.getYear(), ym.getMonthValue());
    }

    private static YearMonth parsePartitionName(String name) {
        if (name.length() != 7) return null;
        try {
            int year = Integer.parseInt(name.substring(1, 5));
            int month = Integer.parseInt(name.substring(5, 7));
            return YearMonth.of(year, month);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Synchronous one-shot called right after the schema is applied, so the very
     * first alert insert lands in a real monthly partition rather than {@code p_future}.
     * Throws on failure: an unpartitioned tg_alerts is a startup-blocking issue,
     * not something to silently fall back from.
     */
    public void bootstrap() throws SQLException {
        ensureFuturePartitions();
    }

    public void start() {
        if (task != null) return;
        task = TGPlatform.getInstance().getScheduler().runAsyncTaskAtFixedRate(
                this::sweep, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        ScheduledTask current = this.task;
        this.task = null;
        if (current != null) current.cancel();
    }

    private void sweep() {
        try {
            ensureFuturePartitions();
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Failed to ensure future tg_alerts partitions, will retry on next interval", ex);
        }

        if (alertRetentionDays > 0) {
            try {
                dropExpiredAlertPartitions();
            } catch (Exception ex) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING,
                        "Failed to drop expired tg_alerts partitions, will retry on next interval", ex);
            }
        }

        if (vpnRetentionDays > 0) sweepVpnCache();
    }

    private void sweepVpnCache() {
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(vpnRetentionDays);
        try {
            long removed = vpnCacheDao.deleteOlderThan(cutoff, VPN_DELETE_CHUNK_SIZE);
            if (removed > 0) {
                TGPlatform.getInstance().getLogger().info(
                        "Retention sweep removed " + removed + " VPN cache row(s) older than "
                                + vpnRetentionDays + " day(s)");
            }
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "VPN cache retention sweep failed, will retry on next interval", ex);
        }
    }

    private void dropExpiredAlertPartitions() throws SQLException {
        long cutoffSeconds = TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(alertRetentionDays));

        List<String> toDrop = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_ALERT_PARTITIONS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1);
                String description = rs.getString(2);
                if (name == null || FUTURE_PARTITION_NAME.equals(name)) continue;
                if (description == null || "MAXVALUE".equalsIgnoreCase(description)) continue;
                long upperBoundSeconds;
                try {
                    upperBoundSeconds = Long.parseLong(description.trim());
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (upperBoundSeconds <= cutoffSeconds) toDrop.add(name);
            }
        }

        if (toDrop.isEmpty()) return;

        try (Connection c = connection.borrow();
             Statement stmt = c.createStatement()) {
            for (String name : toDrop) {
                stmt.execute("ALTER TABLE tg_alerts DROP PARTITION " + name);
                TGPlatform.getInstance().getLogger().info(
                        "Dropped tg_alerts partition " + name + " (older than "
                                + alertRetentionDays + " day retention)");
            }
        }
    }

    private void ensureFuturePartitions() throws SQLException {
        List<YearMonth> existing = listMonthlyPartitions();
        YearMonth now = YearMonth.from(ZonedDateTime.now(ZoneOffset.UTC));
        List<YearMonth> needed = new ArrayList<>();
        for (int i = 0; i <= FUTURE_PARTITION_HORIZON_MONTHS; i++) {
            YearMonth ym = now.plusMonths(i);
            if (!existing.contains(ym)) needed.add(ym);
        }
        if (needed.isEmpty()) return;

        StringBuilder sql = new StringBuilder("ALTER TABLE tg_alerts REORGANIZE PARTITION ")
                .append(FUTURE_PARTITION_NAME).append(" INTO (");
        for (int i = 0; i < needed.size(); i++) {
            YearMonth ym = needed.get(i);
            YearMonth next = ym.plusMonths(1);
            long upper = LocalDate.of(next.getYear(), next.getMonthValue(), 1)
                    .atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            if (i > 0) sql.append(',');
            sql.append("PARTITION ").append(partitionName(ym))
                    .append(" VALUES LESS THAN (").append(upper).append(')');
        }
        sql.append(",PARTITION ").append(FUTURE_PARTITION_NAME).append(" VALUES LESS THAN MAXVALUE)");

        try (Connection c = connection.borrow();
             Statement stmt = c.createStatement()) {
            stmt.execute(sql.toString());
        }
    }

    private List<YearMonth> listMonthlyPartitions() throws SQLException {
        List<YearMonth> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_ALERT_PARTITIONS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1);
                if (name == null || !name.startsWith("p2")) continue;
                YearMonth ym = parsePartitionName(name);
                if (ym != null) out.add(ym);
            }
        }
        return out;
    }
}
