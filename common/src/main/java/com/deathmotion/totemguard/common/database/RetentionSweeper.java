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
import com.deathmotion.totemguard.common.database.dao.AlertDao;
import com.deathmotion.totemguard.common.database.dao.VpnCacheDao;
import com.deathmotion.totemguard.common.util.ScheduledTask;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class RetentionSweeper {

    private static final long INITIAL_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(1);
    private static final long PERIOD_SECONDS = TimeUnit.HOURS.toSeconds(6);
    private static final int DELETE_CHUNK_SIZE = 10_000;

    private final AlertDao alertDao;
    private final VpnCacheDao vpnCacheDao;
    private final int alertRetentionDays;
    private final int vpnRetentionDays;

    private ScheduledTask task;

    public RetentionSweeper(AlertDao alertDao, VpnCacheDao vpnCacheDao, DatabaseOptions options) {
        this.alertDao = alertDao;
        this.vpnCacheDao = vpnCacheDao;
        this.alertRetentionDays = Math.max(0, options.retentionAlertDays());
        this.vpnRetentionDays = Math.max(0, options.retentionVpnDays());
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
        if (alertRetentionDays > 0) sweepAlerts();
        if (vpnRetentionDays > 0) sweepVpnCache();
    }

    private void sweepAlerts() {
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(alertRetentionDays);
        try {
            long removed = alertDao.deleteOlderThan(cutoff, DELETE_CHUNK_SIZE);
            if (removed > 0) {
                TGPlatform.getInstance().getLogger().info(
                        "Retention sweep removed " + removed + " alert row(s) older than "
                                + alertRetentionDays + " day(s)");
            }
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Alert retention sweep failed, will retry on next interval", ex);
        }
    }

    private void sweepVpnCache() {
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(vpnRetentionDays);
        try {
            long removed = vpnCacheDao.deleteOlderThan(cutoff, DELETE_CHUNK_SIZE);
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
}
