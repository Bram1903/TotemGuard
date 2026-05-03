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

import com.deathmotion.totemguard.api.event.impl.TGFocusEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.api.user.UserRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.alert.AlertFilter;
import com.deathmotion.totemguard.common.alert.RealtimeAlertRoster;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.FocusTarget;
import com.deathmotion.totemguard.common.commands.impl.FocusCommand;
import com.deathmotion.totemguard.common.commands.impl.TesterCommand;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.api.impl.TGFocusEventImpl;
import com.deathmotion.totemguard.common.event.api.impl.TGUserQuitEventImpl;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.latency.TransactionTimeoutWatchdog;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerRepositoryImpl implements UserRepository {

    private static final String BYPASS_PERMISSION = "TotemGuard.Bypass";
    private static final Duration ALERTS_TOGGLE_TTL = Duration.ofMinutes(30);
    private final TGPlatform platform;
    private final CacheRepositoryImpl cacheRepository;
    private final ConcurrentMap<User, TGPlayer> players = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TGPlayer> playersByUuid = new ConcurrentHashMap<>();
    private final Collection<UUID> exemptUsers = ConcurrentHashMap.newKeySet();
    private final TransactionTimeoutWatchdog transactionTimeoutWatchdog;

    public PlayerRepositoryImpl() {
        platform = TGPlatform.getInstance();
        cacheRepository = platform.getCacheRepository();
        transactionTimeoutWatchdog = new TransactionTimeoutWatchdog(this);
        transactionTimeoutWatchdog.start();
    }

    public void shutdown() {
        transactionTimeoutWatchdog.stop();
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

        PlatformPlayer platformPlayer;
        if (player != null) {
            playersByUuid.putIfAbsent(uuid, player);
            player.onLogin();
            platformPlayer = player.getPlatformPlayer();
        } else {
            platformPlayer = platform.getPlatformPlayerFactory().create(uuid);
        }
        if (platformPlayer == null) return;

        restoreSubscriptions(uuid, platformPlayer);
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence != null) {
            presence.onLocalPlayerJoin(uuid, user.getName(), user.getProfile());
        }
        platform.getUpdateCheckerRepository().notifyIfOutdated(platformPlayer);
    }

    private void restoreSubscriptions(UUID uuid, PlatformPlayer platformPlayer) {
        boolean wantsAlerts = platformPlayer.hasPermission("TotemGuard.Alerts");
        boolean canFocus = platformPlayer.hasPermission("TotemGuard.Focus");
        boolean wantsTester = TGVersions.CURRENT.snapshot()
                && platformPlayer.hasPermission("TotemGuard.Tester");
        if (!wantsAlerts && !wantsTester && !canFocus) return;

        platform.getScheduler().runAsyncTask(() -> {
            if (wantsAlerts && resolveAlertsEnabled(uuid)) {
                platform.getAlertRepository().toggleAlerts(uuid);
            }

            RealtimeAlertRoster roster = platform.getAlertRepository().getRealtimeRoster();
            boolean focusRestored = canFocus && tryRestoreFocus(uuid, platformPlayer, roster);

            if (!focusRestored && wantsTester && resolveTesterEnabled(uuid)) {
                if (roster.get(uuid) == null) {
                    roster.put(uuid, platformPlayer, new AlertFilter.Self(uuid), null);
                    platformPlayer.sendMessage(platform.getMessageService().getComponent(MessagesKeys.TESTER_ENABLED));
                }
            }
        });
    }

    private boolean tryRestoreFocus(UUID viewerUuid, PlatformPlayer viewer, RealtimeAlertRoster roster) {
        FocusTarget cached = cacheRepository.getAndRefresh(
                CacheKeys.focusTarget(viewerUuid), CacheCodecs.FOCUS_TARGET, FocusCommand.FOCUS_TTL);
        if (cached == null) return false;

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        RemotePlayerEntry target = presence == null ? null : presence.findByUuid(cached.targetUuid());
        if (target == null) {
            cacheRepository.remove(CacheKeys.focusTarget(viewerUuid));
            return false;
        }

        TGPlayer localTarget = platform.getPlayerRepository().getPlayer(target.playerUuid());
        String proxyServerId = platform.resolveProxyServerId(target.serverName());
        TGFocusEvent event = platform.getEventRepository().post(TGFocusEventImpl.enabling(
                viewerUuid, target.playerUuid(), target.playerName(), localTarget,
                target.serverInstanceId(), target.serverName(), proxyServerId, true));
        if (event.isCancelled()) {
            cacheRepository.remove(CacheKeys.focusTarget(viewerUuid));
            return false;
        }

        roster.put(viewerUuid, viewer, new AlertFilter.Violator(cached.targetUuid()), target.playerName());
        viewer.sendMessage(platform.getMessageService().getComponent(
                MessagesKeys.FOCUS_ENABLED,
                Map.of("tg_player", target.playerName())
        ));
        return true;
    }

    private boolean resolveTesterEnabled(UUID uuid) {
        Boolean cached = cacheRepository.getAndRefresh(
                CacheKeys.testerToggle(uuid), CacheCodecs.BOOLEAN, TesterCommand.TESTER_TOGGLE_TTL);
        if (cached != null) return cached;

        boolean firstTimeDefault = true;
        cacheRepository.put(CacheKeys.testerToggle(uuid), firstTimeDefault,
                CacheCodecs.BOOLEAN, TesterCommand.TESTER_TOGGLE_TTL);
        return firstTimeDefault;
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

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        String name = user.getName();
        if (presence != null && name != null) {
            presence.onLocalPlayerQuit(uuid, name);
        }

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

    public boolean shouldCheck(final @NotNull User user, final @Nullable PlatformPlayer platformPlayer) {
        final UUID uuid = user.getUUID();
        if (uuid == null) return false;

        if (isExempt(uuid)) return false;
        if (!ChannelHelper.isOpen(user.getChannel())) return false;

        if (uuid.getMostSignificantBits() == 0L) {
            setExempt(uuid, true);
            return false;
        }

        if (platformPlayer != null && platformPlayer.hasPermission(BYPASS_PERMISSION)) {
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
