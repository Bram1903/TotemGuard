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

import com.deathmotion.totemguard.api.event.impl.TGUserPunishEvent;
import com.deathmotion.totemguard.api.punishment.PunishmentRepository;
import com.deathmotion.totemguard.api.punishment.PunishmentType;
import com.deathmotion.totemguard.api.reload.Reloadable;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PunishmentRepositoryImpl implements PunishmentRepository, Reloadable {

    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final TGPlatform platform;
    private final CacheRepositoryImpl cacheRepository;
    private final EventRepositoryImpl eventRepository;
    private final ConfigRepositoryImpl configRepository;
    private final PlaceholderRepositoryImpl placeholderRepository;

    private final Set<UUID> inFlightPunishments = ConcurrentHashMap.newKeySet();

    private PunishmentCommand defaultPunishmentCommand;

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
        this.defaultPunishmentCommand = PunishmentCommand.parse(configRepository.checks().defaultPunishment());
    }

    @Override
    public boolean isPunishmentQueued(@NotNull UUID uuid) {
        return cacheRepository.contains(CacheKeys.punishLock(uuid));
    }

    public void punish(CheckImpl check, int violations, @Nullable String debug) {
        if (!canPunish(check, violations)) return;
        runPunishment(check, resolveCommands(check), debug, Map.of(), true);
    }

    public void punishWith(CheckImpl check,
                           List<PunishmentCommand> commands,
                           @Nullable String debug,
                           Map<String, Object> placeholderExtras) {
        if (commands.isEmpty()) return;
        runPunishment(check, commands, debug, placeholderExtras, false);
    }

    private void runPunishment(CheckImpl check,
                               List<PunishmentCommand> commands,
                               @Nullable String debug,
                               Map<String, Object> placeholderExtras,
                               boolean clearViolationsAfter) {
        TGPlayer player = check.player;
        UUID playerUuid = player.getUuid();

        boolean containsBan = containsBan(commands);

        if (!tryClaim(playerUuid, containsBan)) return;

        boolean handedOff = false;
        try {
            TGUserPunishEvent event = eventRepository.post(new TGUserPunishEventImpl(
                    player,
                    check,
                    debug
            ));
            if (event.isCancelled()) return;

            Runnable executeAndCleanup = () -> {
                boolean keepDistributedLock = false;
                try {
                    if (!executePunishment(check, commands, debug, placeholderExtras)) {
                        platform.getLogger().warning(
                                "Skipped punishment for " + player.getName() + " because no punishment commands could be executed for check " + check.getName() + "."
                        );
                        return;
                    }

                    platform.getDiscordWebhookService().sendPunishment(check, debug);

                    if (clearViolationsAfter) player.getCheckManager().clearAllViolations();
                    keepDistributedLock = containsBan;
                } finally {
                    finishClaim(playerUuid, containsBan, keepDistributedLock);
                }
            };

            if (configRepository.configView().banAnimationEnabled()
                    && containsRemoval(commands)
                    && BanAnimation.isSupported(player)) {
                BanAnimation.play(player);
                platform.getScheduler().runAsyncTaskDelayed(executeAndCleanup, BanAnimation.ANIMATION_DURATION_MS, TimeUnit.MILLISECONDS);
            } else {
                executeAndCleanup.run();
            }
            handedOff = true;
        } finally {
            if (!handedOff) finishClaim(playerUuid, containsBan, false);
        }
    }

    private boolean tryClaim(UUID uuid, boolean distributed) {
        if (!inFlightPunishments.add(uuid)) return false;
        if (!distributed) return true;
        boolean claimed = cacheRepository.putIfAbsent(
                CacheKeys.punishLock(uuid),
                System.currentTimeMillis(),
                CacheCodecs.LONG,
                LOCK_TTL
        );
        if (!claimed) inFlightPunishments.remove(uuid);
        return claimed;
    }

    private void finishClaim(UUID uuid, boolean distributed, boolean keepDistributedLock) {
        inFlightPunishments.remove(uuid);
        if (distributed && !keepDistributedLock) {
            cacheRepository.remove(CacheKeys.punishLock(uuid));
        }
    }

    private boolean canPunish(CheckImpl check, int violations) {
        return check.isPunishable() && violations >= check.getMaxViolations();
    }

    private List<PunishmentCommand> resolveCommands(CheckImpl check) {
        return check.getPunishCommands().isEmpty()
                ? List.of(defaultPunishmentCommand)
                : check.getPunishCommands();
    }

    private PunishmentType effectiveType(PunishmentCommand command) {
        if (command.type() != PunishmentType.GENERIC) return command.type();
        if (command.raw().contains("%default_punishment%")) return defaultPunishmentCommand.type();
        return PunishmentType.GENERIC;
    }

    private boolean containsBan(List<PunishmentCommand> commands) {
        for (PunishmentCommand command : commands) {
            if (command.type() == PunishmentType.BAN) return true;
            // User-defined commands may inline `%default_punishment%`; if the default
            // resolves to a BAN, treat the whole batch as ban-bearing.
            if (command.type() == PunishmentType.GENERIC
                    && defaultPunishmentCommand.type() == PunishmentType.BAN
                    && command.raw().contains("%default_punishment%")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRemoval(List<PunishmentCommand> commands) {
        for (PunishmentCommand command : commands) {
            PunishmentType type = command.type();
            if (type == PunishmentType.BAN || type == PunishmentType.KICK) return true;
            if (type == PunishmentType.GENERIC
                    && (defaultPunishmentCommand.type() == PunishmentType.BAN
                    || defaultPunishmentCommand.type() == PunishmentType.KICK)
                    && command.raw().contains("%default_punishment%")) {
                return true;
            }
        }
        return false;
    }

    private boolean executePunishment(CheckImpl check,
                                      List<PunishmentCommand> commands,
                                      @Nullable String debug,
                                      Map<String, Object> placeholderExtras) {
        int dispatchedCommands = 0;

        for (PunishmentCommand command : commands) {
            String processedCommand = command.raw().replace("%default_punishment%", defaultPunishmentCommand.raw()).trim();
            if (processedCommand.isEmpty()) {
                continue;
            }

            try {
                String processed = placeholderRepository
                        .replace(processedCommand, check.player, check, placeholderExtras)
                        .trim();
                if (processed.isEmpty()) {
                    continue;
                }

                platform.dispatchCommand(processed);
                dispatchedCommands++;
                PunishmentType effectiveType = effectiveType(command);
                if (effectiveType != PunishmentType.GENERIC) {
                    recordPunishment(check, effectiveType, processed, debug);
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
                player.getDatabaseProfileId(),
                player.getDatabasePlayerId(),
                check.getName(),
                type,
                dispatched,
                debug,
                System.currentTimeMillis()
        );
    }
}
