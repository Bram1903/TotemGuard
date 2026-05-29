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

package com.deathmotion.totemguard.common.features.punishment;

import com.deathmotion.totemguard.api.event.events.TGNetworkAlertEvent;
import com.deathmotion.totemguard.api.punishment.PunishmentRepository;
import com.deathmotion.totemguard.api.punishment.PunishmentType;
import com.deathmotion.totemguard.api.reload.Reloadable;
import com.deathmotion.totemguard.api.user.BanAnimation;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.database.util.DebugTemplate;
import com.deathmotion.totemguard.common.event.EventBusImpl;
import com.deathmotion.totemguard.common.features.alert.NetworkAlertBroadcaster;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.placeholder.engine.PlaceholderEngine;
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
    private final EventBusImpl eventBus;
    private final ConfigRepositoryImpl configRepository;
    private final PlaceholderRepositoryImpl placeholderRepository;

    private final Set<UUID> inFlightPunishments = ConcurrentHashMap.newKeySet();

    private PunishmentCommand defaultPunishmentCommand;

    public PunishmentRepositoryImpl() {
        this.platform = TGPlatform.getInstance();
        this.cacheRepository = platform.getCacheRepository();
        this.eventBus = platform.getEventBus();
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
        punish(check, violations, debug, null);
    }

    public void punish(CheckImpl check, int violations, @Nullable String debug,
                       @Nullable DebugTemplate.Compiled compiledDebug) {
        if (!canPunish(check, violations)) return;
        runPunishment(check, resolveCommands(check), debug, compiledDebug, Map.of(), true);
    }

    public void punishWith(CheckImpl check,
                           List<PunishmentCommand> commands,
                           @Nullable String debug,
                           Map<String, Object> placeholderExtras) {
        punishWith(check, commands, debug, null, placeholderExtras);
    }

    public void punishWith(CheckImpl check,
                           List<PunishmentCommand> commands,
                           @Nullable String debug,
                           @Nullable DebugTemplate.Compiled compiledDebug,
                           Map<String, Object> placeholderExtras) {
        if (commands.isEmpty()) return;
        runPunishment(check, commands, debug, compiledDebug, placeholderExtras, false);
    }

    private void runPunishment(CheckImpl check,
                               List<PunishmentCommand> commands,
                               @Nullable String debug,
                               @Nullable DebugTemplate.Compiled compiledDebug,
                               Map<String, Object> placeholderExtras,
                               boolean clearViolationsAfter) {
        TGPlayer player = check.player;
        UUID playerUuid = player.getUuid();

        boolean containsBan = containsBan(commands);

        if (!tryClaim(playerUuid, containsBan)) return;

        boolean handedOff = false;
        try {
            if (eventBus.getUserPunish().fire(player, check, debug)) return;

            Runnable executeAndCleanup = () -> {
                boolean keepDistributedLock = false;
                try {
                    if (!executePunishment(check, commands, debug, compiledDebug, placeholderExtras)) {
                        platform.getLogger().warning(
                                "Skipped punishment for " + player.getName() + " because no punishment commands could be executed for check " + check.getName() + "."
                        );
                        return;
                    }

                    platform.getDiscordWebhookService().sendPunishment(check, debug);

                    NetworkAlertBroadcaster.broadcast(platform, playerUuid, player.getName(), check.getName(),
                            check.getType(), check.getViolations(), debug, TGNetworkAlertEvent.Kind.PUNISHMENT);

                    if (clearViolationsAfter) player.getCheckManager().clearAllViolations();
                    keepDistributedLock = containsBan;
                } finally {
                    finishClaim(playerUuid, containsBan, keepDistributedLock);
                }
            };

            BanAnimation animation = player.getBanAnimation();
            if (configRepository.configView().banAnimationEnabled()
                    && containsRemoval(commands)
                    && animation.isSupported()) {
                animation.play();
                platform.getScheduler().runAsyncTaskDelayed(executeAndCleanup, animation.getDurationMs(), TimeUnit.MILLISECONDS);
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
                                      @Nullable DebugTemplate.Compiled compiledDebug,
                                      Map<String, Object> placeholderExtras) {
        int dispatchedCommands = 0;

        for (PunishmentCommand command : commands) {
            String processedCommand = command.raw().replace("%default_punishment%", defaultPunishmentCommand.raw()).trim();
            if (processedCommand.isEmpty()) {
                continue;
            }

            try {
                PlaceholderEngine.Capture capture = placeholderRepository.replaceCapturing(
                        processedCommand, check.player, check, placeholderExtras);
                String dispatched = capture.dispatched().trim();
                if (dispatched.isEmpty()) {
                    continue;
                }

                platform.dispatchCommand(dispatched);
                dispatchedCommands++;
                PunishmentType effectiveType = effectiveType(command);
                if (effectiveType != PunishmentType.GENERIC) {
                    recordPunishment(check, effectiveType, capture.template(), capture.args(), debug, compiledDebug);
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

    private void recordPunishment(CheckImpl check, PunishmentType type, String commandTemplate,
                                  @Nullable String commandArgs,
                                  @Nullable String debug, @Nullable DebugTemplate.Compiled compiledDebug) {
        TGPlayer player = check.player;
        platform.getDatabaseRepository().recordPunishment(
                player.getDatabaseProfileId(),
                player.getDatabasePlayerId(),
                check.getName(),
                type,
                commandTemplate,
                commandArgs,
                debug,
                compiledDebug,
                System.currentTimeMillis()
        );
    }
}
