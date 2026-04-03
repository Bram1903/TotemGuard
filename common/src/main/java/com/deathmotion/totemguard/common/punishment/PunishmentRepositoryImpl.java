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
import com.deathmotion.totemguard.api3.reload.Reloadable;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.api.impl.TGUserPunishEventImpl;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PunishmentRepositoryImpl implements PunishmentRepository, Reloadable {

    private final TGPlatform platform;
    private final CacheRepositoryImpl cacheRepository;
    private final EventRepositoryImpl eventRepository;
    private final ConfigRepositoryImpl configRepository;
    private final PlaceholderRepositoryImpl placeholderRepository;

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
        return cacheRepository.isPunishmentQueued(uuid);
    }

    public void punish(CheckImpl check, int violations, @Nullable String debug) {
        if (!canPunish(check, violations)) return;

        TGPlayer player = check.player;
        UUID playerUuid = player.getUuid();

        if (!cacheRepository.tryClaimPunishment(playerUuid)) return;

        boolean keepDistributedLock = false;
        try {
            TGUserPunishEvent event = eventRepository.post(new TGUserPunishEventImpl(
                    player,
                    check,
                    debug
            ));
            if (event.isCancelled()) return;

            if (!executePunishment(check)) {
                platform.getLogger().warning(
                        "Skipped punishment for " + player.getName() + " because no punishment commands could be executed for check " + check.getName() + "."
                );
                return;
            }

            check.clearViolations();
            keepDistributedLock = true;
        } finally {
            cacheRepository.finishPunishment(playerUuid, keepDistributedLock);
        }
    }

    private boolean canPunish(CheckImpl check, int violations) {
        return check.isPunishable() && violations >= check.getMaxViolations();
    }

    private boolean executePunishment(CheckImpl check) {
        List<String> commands = check.getPunishCommands().isEmpty()
                ? List.of("%default_punishment%")
                : check.getPunishCommands();
        int dispatchedCommands = 0;

        for (String command : commands) {
            String processedCommand = command.replace("%default_punishment%", defaultPunishment).trim();
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
}
