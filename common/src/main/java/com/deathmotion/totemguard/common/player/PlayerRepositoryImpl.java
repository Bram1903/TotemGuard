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

package com.deathmotion.totemguard.common.player;

import com.deathmotion.totemguard.api3.user.TGUser;
import com.deathmotion.totemguard.api3.user.UserRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.event.api.impl.TGUserQuitEventImpl;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.deathmotion.totemguard.common.player.latency.TransactionTimeoutWatchdog;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerRepositoryImpl implements UserRepository {

    private static final String BYPASS_PERMISSION = "TotemGuardV3.Bypass";
    private static final Duration ALERTS_TOGGLE_TTL = Duration.ofMinutes(30);
    private final TGPlatform platform;
    private final CacheRepositoryImpl cacheRepository;
    private final ConcurrentMap<User, TGPlayer> players = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TGPlayer> playersByUuid = new ConcurrentHashMap<>();
    private final Collection<UUID> exemptUsers = ConcurrentHashMap.newKeySet();

    public PlayerRepositoryImpl() {
        platform = TGPlatform.getInstance();
        cacheRepository = platform.getCacheRepository();
        new TransactionTimeoutWatchdog(this).start();
    }

    public void onLoginPacket(final @NotNull User user) {
        if (!shouldCheck(user, null)) return;
        TGPlayer player = new TGPlayer(user);
        players.put(user, player);
        UUID uuid = user.getUUID();
        if (uuid != null) {
            playersByUuid.put(uuid, player);
        }
    }

    public void onLogin(final @NotNull User user) {
        final UUID uuid = user.getUUID();
        if (uuid == null) {
            removeUser(user);
            return;
        }

        final TGPlayer player = players.get(user);

        PlatformUser platformUser;
        if (player != null) {
            playersByUuid.putIfAbsent(uuid, player);
            player.onLogin();
            platformUser = player.getPlatformUser();
        } else {
            PlatformUserCreation platformUserCreation = platform.getPlatformUserFactory().create(uuid);
            if (platformUserCreation == null) return;
            platformUser = platformUserCreation.getPlatformUser();
        }

        enableAlerts(uuid, platformUser);
        platform.getUpdateCheckerRepository().notifyIfOutdated(platformUser);
    }

    private void enableAlerts(UUID uuid, PlatformUser platformUser) {
        if (!platformUser.hasPermission("TotemGuardV3.Alerts")) return;

        platform.getScheduler().runAsyncTask(() -> {
            boolean enabled = resolveAlertsEnabled(uuid);
            if (enabled) {
                platform.getAlertRepository().toggleAlerts(uuid);
            }
        });
    }

    /**
     * Cache → DB → first-time default (on).
     */
    private boolean resolveAlertsEnabled(UUID uuid) {
        Boolean cached = cacheRepository.getAndRefresh(
                CacheKeys.alertsToggle(uuid), CacheCodecs.BOOLEAN, ALERTS_TOGGLE_TTL);
        if (cached != null) return cached;

        if (platform.getDatabaseRepository().isConnected()) {
            try {
                Boolean stored = platform.getDatabaseRepository().findStaffAlertPref(uuid);
                if (stored != null) {
                    cacheRepository.put(CacheKeys.alertsToggle(uuid), stored,
                            CacheCodecs.BOOLEAN, ALERTS_TOGGLE_TTL);
                    return stored;
                }
            } catch (Exception ex) {
                platform.getLogger().warning(
                        "Failed to load staff alert preference for " + uuid + ": " + ex.getMessage());
            }
        }

        boolean firstTimeDefault = true;
        cacheRepository.put(CacheKeys.alertsToggle(uuid), firstTimeDefault,
                CacheCodecs.BOOLEAN, ALERTS_TOGGLE_TTL);
        if (platform.getDatabaseRepository().isConnected()) {
            try {
                platform.getDatabaseRepository().upsertStaffAlertPref(uuid, firstTimeDefault);
            } catch (Exception ex) {
                platform.getLogger().warning(
                        "Failed to persist default staff alert preference for " + uuid + ": " + ex.getMessage());
            }
        }
        return firstTimeDefault;
    }

    public void onPlayerDisconnect(final @NotNull User user) {
        UUID uuid = user.getUUID();
        if (uuid == null) return;

        clearExempt(uuid);
        platform.getAlertRepository().removeUser(uuid);

        final TGPlayer player = players.remove(user);
        playersByUuid.remove(uuid);
        if (player == null) return;
        platform.getEventRepository().post(new TGUserQuitEventImpl(player));
        player.onLogout();
    }

    public void removeUser(final @NotNull User user) {
        players.remove(user);
        UUID uuid = user.getUUID();
        if (uuid != null) {
            playersByUuid.remove(uuid);
        }
        clearExempt(uuid);
    }

    public @Nullable TGPlayer getPlayer(final @NotNull User user) {
        return players.get(user);
    }

    public boolean isExempt(final @NotNull UUID uuid) {
        return exemptUsers.contains(uuid);
    }

    public void setExempt(final @NotNull UUID uuid, final boolean exempt) {
        if (exempt) exemptUsers.add(uuid);
        else clearExempt(uuid);
    }

    private void clearExempt(final @NotNull UUID uuid) {
        exemptUsers.remove(uuid);
    }

    public boolean shouldCheck(final @NotNull User user, final @Nullable PlatformUser platformUser) {
        final UUID uuid = user.getUUID();
        if (uuid == null) return false;

        if (isExempt(uuid)) return false;
        if (!ChannelHelper.isOpen(user.getChannel())) return false;

        if (uuid.getMostSignificantBits() == 0L) {
            setExempt(uuid, true);
            return false;
        }

        if (platformUser != null && platformUser.hasPermission(BYPASS_PERMISSION)) {
            setExempt(uuid, true);
            return false;
        }

        return true;
    }

    @Override
    public @Nullable TGUser getUser(final @NotNull UUID uuid) {
        return playersByUuid.get(uuid);
    }

    public @Nullable TGPlayer getPlayer(final @NotNull UUID uuid) {
        return playersByUuid.get(uuid);
    }

    public @NotNull Collection<TGPlayer> getPlayers() {
        return players.values();
    }

    public void backfillDatabaseProfiles() {
        for (TGPlayer player : getPlayers()) {
            if (player.getDatabasePlayerId() > 0) continue;
            if (!player.isHasLoggedIn()) continue;
            player.resolveDatabaseProfile();
        }
    }
}
