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

package com.deathmotion.totemguard.common.config.view;

import com.deathmotion.totemguard.api.config.Config;
import com.deathmotion.totemguard.common.config.key.ConfigKeys;
import com.deathmotion.totemguard.common.config.schema.*;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugLevel;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConfigView {

    private final int version;
    private final String server;
    private final CommandsOptions commands;
    private final RedisOptions redis;
    private final DatabaseOptions database;
    private final UpdateCheckerOptions updateChecker;
    private final boolean unsupportedClientKick;
    private final EntitySpoofingOptions entitySpoofing;
    private final boolean banAnimationEnabled;
    private final boolean tickSkipKeepAliveValidation;
    private final boolean physicsEngineEnabled;
    private final PhysicsPreset physicsPreset;
    private final PhysicsDebugLevel physicsDebugLevel;
    private final boolean physicsEngineSetback;
    private final boolean physicsEngineCloseInventory;
    private final boolean physicsEngineFallDamage;
    private final boolean physicsEngineTimerPacketCancel;
    private final boolean debugModifierKickEnabled;
    private final Set<String> developerOverrides;

    public ConfigView(Config config) {
        this.version = config.version();
        this.server = config.getString(ConfigKeys.SERVER);
        this.commands = new CommandsOptions(
                config.getString(ConfigKeys.COMMANDS_BASE),
                config.getStringList(ConfigKeys.COMMAND_ALIASES)
        );
        this.redis = new RedisOptions(
                config.getBoolean(ConfigKeys.REDIS_ENABLED),
                config.getBoolean(ConfigKeys.REDIS_CLUSTER),
                config.getString(ConfigKeys.REDIS_HOST),
                config.getInt(ConfigKeys.REDIS_PORT),
                config.getString(ConfigKeys.REDIS_USERNAME),
                config.getString(ConfigKeys.REDIS_PASSWORD),
                new RedisOptions.MessagingOptions(
                        new RedisOptions.AlertsOptions(
                                config.getString(ConfigKeys.REDIS_MESSAGING_ALERTS_CHANNEL),
                                config.getBoolean(ConfigKeys.REDIS_MESSAGING_ALERTS_SEND),
                                config.getBoolean(ConfigKeys.REDIS_MESSAGING_ALERTS_RECEIVE)
                        )
                )
        );
        this.database = new DatabaseOptions(
                config.getBoolean(ConfigKeys.DATABASE_ENABLED),
                this.server,
                config.getString(ConfigKeys.DATABASE_HOST),
                config.getInt(ConfigKeys.DATABASE_PORT),
                config.getString(ConfigKeys.DATABASE_DATABASE),
                config.getString(ConfigKeys.DATABASE_USERNAME),
                config.getString(ConfigKeys.DATABASE_PASSWORD),
                config.getString(ConfigKeys.DATABASE_PARAMETERS),
                config.getInt(ConfigKeys.DATABASE_RETENTION_ALERT_DAYS)
        );
        this.updateChecker = new UpdateCheckerOptions(
                config.getBoolean(ConfigKeys.UPDATE_CHECKER_ENABLED),
                config.getBoolean(ConfigKeys.UPDATE_CHECKER_NOTIFY_ON_JOIN)
        );
        this.unsupportedClientKick = config.getBoolean(ConfigKeys.UNSUPPORTED_CLIENT_KICK);
        this.entitySpoofing = new EntitySpoofingOptions(
                config.getBoolean(ConfigKeys.ENTITY_SPOOFING_HEALTH),
                config.getBoolean(ConfigKeys.ENTITY_SPOOFING_ABSORPTION)
        );
        this.banAnimationEnabled = config.getBoolean(ConfigKeys.BAN_ANIMATION_ENABLED);
        this.tickSkipKeepAliveValidation = config.getBoolean(ConfigKeys.TICK_SKIP_KEEP_ALIVE_VALIDATION);
        this.physicsEngineEnabled = config.getBoolean(ConfigKeys.PHYSICS_ENGINE_ENABLED);
        this.physicsPreset = PhysicsPreset.parse(config.getString(ConfigKeys.PHYSICS_ENGINE_PRESET));
        this.physicsDebugLevel = PhysicsDebugLevel.parse(config.getString(ConfigKeys.PHYSICS_ENGINE_DEBUG_LEVEL));
        this.physicsEngineSetback = config.getBoolean(ConfigKeys.PHYSICS_ENGINE_SETBACK);
        this.physicsEngineCloseInventory = config.getBoolean(ConfigKeys.PHYSICS_ENGINE_CLOSE_INVENTORY);
        this.physicsEngineFallDamage = config.getBoolean(ConfigKeys.PHYSICS_ENGINE_FALL_DAMAGE);
        this.physicsEngineTimerPacketCancel = config.getBoolean(ConfigKeys.PHYSICS_ENGINE_TIMER_PACKET_CANCEL);
        this.debugModifierKickEnabled = config.getBoolean(ConfigKeys.DEBUG_MODIFIER_KICK_ENABLED);
        List<String> overrides = config.getStringList(ConfigKeys.DEVELOPER_OVERRIDES);
        this.developerOverrides = overrides.isEmpty() ? Set.of() : Set.copyOf(new HashSet<>(overrides));
    }

    public int version() {
        return version;
    }

    public @NotNull String server() {
        return server;
    }

    public @NotNull CommandsOptions commands() {
        return commands;
    }

    public @NotNull RedisOptions redis() {
        return redis;
    }

    public @NotNull DatabaseOptions database() {
        return database;
    }

    public @NotNull UpdateCheckerOptions updateChecker() {
        return updateChecker;
    }

    public boolean unsupportedClientKick() {
        return unsupportedClientKick;
    }

    public @NotNull EntitySpoofingOptions entitySpoofing() {
        return entitySpoofing;
    }

    public boolean banAnimationEnabled() {
        return banAnimationEnabled;
    }

    public boolean tickSkipKeepAliveValidation() {
        return tickSkipKeepAliveValidation;
    }

    public boolean physicsEngineEnabled() {
        return physicsEngineEnabled;
    }

    public @NotNull PhysicsPreset physicsPreset() {
        return physicsPreset;
    }

    public @NotNull PhysicsDebugLevel physicsDebugLevel() {
        return physicsDebugLevel;
    }

    public boolean physicsEngineSetback() {
        return physicsEngineSetback;
    }

    public boolean physicsEngineCloseInventory() {
        return physicsEngineCloseInventory;
    }

    public boolean physicsEngineFallDamage() {
        return physicsEngineFallDamage;
    }

    public boolean physicsEngineTimerPacketCancel() {
        return physicsEngineTimerPacketCancel;
    }

    public boolean debugModifierKickEnabled() {
        return debugModifierKickEnabled;
    }

    public boolean hasDeveloperOverride(@NotNull String flag) {
        return developerOverrides.contains(flag);
    }
}
