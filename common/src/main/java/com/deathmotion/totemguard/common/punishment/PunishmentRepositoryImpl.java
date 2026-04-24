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

package com.deathmotion.totemguard.common.punishment;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.event.impl.TGUserPunishEvent;
import com.deathmotion.totemguard.api3.punishment.PunishmentRepository;
import com.deathmotion.totemguard.api3.punishment.PunishmentType;
import com.deathmotion.totemguard.api3.reload.Reloadable;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.api.impl.TGUserPunishEventImpl;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PunishmentRepositoryImpl implements PunishmentRepository, Reloadable {

    // Short TTL — punishment commands should complete well inside a minute.
    // The lock exists only to prevent the double-punish race between servers
    // sharing a Redis; if it outlives the actual command for some reason,
    // the Redis key falls off on its own.
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final TGPlatform platform;
    private final CacheRepositoryImpl cacheRepository;
    private final EventRepositoryImpl eventRepository;
    private final ConfigRepositoryImpl configRepository;
    private final PlaceholderRepositoryImpl placeholderRepository;

    // Fast in-process guard against two threads on the same JVM racing into
    // the same punishment. The distributed side lives behind the cache key.
    private final Set<UUID> inFlightPunishments = ConcurrentHashMap.newKeySet();

    private String defaultPunishment;

    public PunishmentRepositoryImpl() {
        this.platform = TGPlatform.getInstance();
        this.cacheRepository = platform.getCacheRepository();
        this.eventRepository = platform.getEventRepository();
        this.configRepository = platform.getConfigRepository();
        this.placeholderRepository = platform.getPlaceholderRepository();
        reload();
    }

    @Override
    public void reload() {
        Config config = configRepository.config(ConfigFile.CHECKS);
        this.defaultPunishment = config.getString("default-punishment").orElse("ban %tg_player% Unfair Advantage");
    }

    @Override
    public boolean isPunishmentQueued(@NotNull UUID uuid) {
        if (inFlightPunishments.contains(uuid)) return true;
        return cacheRepository.contains(CacheKeys.punishLock(uuid));
    }

    public void punish(CheckImpl check, int violations, @Nullable String debug) {
        if (!canPunish(check, violations)) return;

        TGPlayer player = check.player;
        UUID playerUuid = player.getUuid();

        if (!tryClaim(playerUuid)) return;

        boolean keepDistributedLock = false;
        try {
            TGUserPunishEvent event = eventRepository.post(new TGUserPunishEventImpl(
                    player,
                    check,
                    debug
            ));
            if (event.isCancelled()) return;

            if (!executePunishment(check, debug)) {
                platform.getLogger().warning(
                        "Skipped punishment for " + player.getName() + " because no punishment commands could be executed for check " + check.getName() + "."
                );
                return;
            }

            platform.getDiscordWebhookService().sendPunishment(check, debug);

            check.clearViolations();
            keepDistributedLock = true;
        } finally {
            finishClaim(playerUuid, keepDistributedLock);
        }
    }

    private boolean tryClaim(UUID uuid) {
        if (!inFlightPunishments.add(uuid)) return false;
        // When Redis is offline the cache falls back to in-process storage,
        // and in single-server mode the putIfAbsent on the local backend is
        // effectively the same guard the in-flight set provides. Either way
        // we only proceed if we actually got the key.
        boolean claimed = cacheRepository.putIfAbsent(
                CacheKeys.punishLock(uuid),
                System.currentTimeMillis(),
                CacheCodecs.LONG,
                LOCK_TTL
        );
        if (!claimed) inFlightPunishments.remove(uuid);
        return claimed;
    }

    private void finishClaim(UUID uuid, boolean keepDistributedLock) {
        inFlightPunishments.remove(uuid);
        if (!keepDistributedLock) {
            cacheRepository.remove(CacheKeys.punishLock(uuid));
        }
    }

    private boolean canPunish(CheckImpl check, int violations) {
        return check.isPunishable() && violations >= check.getMaxViolations();
    }

    private boolean executePunishment(CheckImpl check, @Nullable String debug) {
        List<PunishmentCommand> commands = check.getPunishCommands().isEmpty()
                ? List.of(PunishmentCommand.parse("%default_punishment%"))
                : check.getPunishCommands();
        int dispatchedCommands = 0;

        for (PunishmentCommand command : commands) {
            String processedCommand = command.raw().replace("%default_punishment%", defaultPunishment).trim();
            if (processedCommand.isEmpty()) {
                continue;
            }

            try {
                String processed = placeholderRepository.replace(processedCommand, check.player, check).trim();
                if (processed.isEmpty()) {
                    continue;
                }

                platform.dispatchCommand(processed);
                dispatchedCommands++;
                // Only KICK/BAN are persisted as punishments — GENERIC
                // commands (announcements, logging, webhooks) still run
                // but would pollute the punishment history with non-actions.
                if (command.type() != PunishmentType.GENERIC) {
                    recordPunishment(check, command.type(), processed, debug);
                }
            } catch (Exception exception) {
                platform.getLogger().log(
                        Level.WARNING,
                        "Failed to execute punishment command '" + processedCommand + "' for " + check.player.getName(),
                        exception
                );
            }
        }

        return dispatchedCommands > 0;
    }

    private void recordPunishment(CheckImpl check, PunishmentType type, String dispatched, @Nullable String debug) {
        TGPlayer player = check.player;
        platform.getDatabaseRepository().recordPunishment(
                player.getDatabaseSessionId(),
                player.getDatabasePlayerId(),
                check.getName(),
                type,
                dispatched,
                debug,
                System.currentTimeMillis()
        );
    }
}
