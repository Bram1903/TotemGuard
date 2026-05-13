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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.api.event.impl.TGFollowEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.api.impl.TGFollowEventImpl;
import com.deathmotion.totemguard.common.features.follow.FollowRepository;
import com.deathmotion.totemguard.common.features.follow.FollowState;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.github.retrooper.packetevents.protocol.world.Location;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.UUID;

public final class FollowCommand extends AbstractCommand {

    private static final long REQUEST_TTL_MILLIS = 30_000L;

    private final TGPlatform platform;

    public FollowCommand() {
        this.platform = TGPlatform.getInstance();
    }

    private static void applyLocalTeleport(@NotNull PlatformPlayer follower, @NotNull TGPlayer target) {
        MovementData movement = target.getData().getMovementData();
        Location loc = movement.getCurrent();
        PlatformPlayer targetPlatform = target.getPlatformPlayer();
        String world = targetPlatform != null ? targetPlatform.getWorldName() : null;
        follower.teleport(world == null ? "" : world,
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("follow")
                        .optional(
                                "tg_player",
                                StringParser.stringParser(),
                                TGPlayerSuggestionProvider.suggestionProviderExcludingSelf()
                        )
                        .permission(perm("follow"))
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) return;

        FollowRepository follow = platform.getFollowRepository();
        if (follow == null) return;

        String rawTarget = context.<String>optional("tg_player").orElse(null);
        UUID followerUuid = sender.getUniqueId();

        if (rawTarget == null) {
            if (follow.endFollow(followerUuid)) {
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOLLOW_DISABLED));
            } else {
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOLLOW_NONE_ACTIVE));
            }
            return;
        }

        PlatformPlayer followerPlatform = platform.getPlatformPlayerFactory().create(followerUuid);
        if (followerPlatform == null) {
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.GENERAL_PLAYER_DATA_MISSING));
            return;
        }

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        RemotePlayerEntry target = presence == null ? null : presence.findByName(rawTarget);
        if (target == null) {
            sender.sendMessage(platform.getMessageService().getComponent(
                    MessagesKeys.FOLLOW_NOT_FOUND,
                    Map.of("tg_input", rawTarget)
            ));
            return;
        }

        if (target.playerUuid().equals(followerUuid)) {
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOLLOW_SELF));
            return;
        }

        boolean local = presence.isLocal(target.serverInstanceId());
        TGPlayer localTarget = local ? platform.getPlayerRepository().getPlayer(target.playerUuid()) : null;
        if (local && localTarget == null) {
            sender.sendMessage(platform.getMessageService().getComponent(
                    MessagesKeys.FOLLOW_NOT_FOUND,
                    Map.of("tg_input", rawTarget)
            ));
            return;
        }

        if (!local && !platform.getRedisRepository().isConnected()) {
            sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOLLOW_NO_REDIS));
            return;
        }

        if (!local) {
            if (!platform.getProxyTopologyService().bridgeAvailable()) {
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.FOLLOW_NO_BRIDGE));
                return;
            }
            if (!platform.canRouteToInstance(target.serverInstanceId())) {
                sender.sendMessage(platform.getMessageService().getComponent(
                        MessagesKeys.FOLLOW_DIFFERENT_PROXY,
                        Map.of("tg_player", target.playerName(), "tg_server", target.serverName())
                ));
                return;
            }
        }

        TGFollowEvent event = platform.getEventRepository().post(new TGFollowEventImpl(
                followerUuid,
                target.playerUuid(),
                target.playerName(),
                localTarget,
                target.serverInstanceId(),
                target.serverName(),
                !local,
                false
        ));
        if (event.isCancelled()) return;

        FollowState state = new FollowState(followerUuid, target.playerUuid(), target.playerName(),
                target.serverInstanceId(), target.serverName());
        follow.beginFollow(state);

        if (local) {
            applyLocalTeleport(followerPlatform, localTarget);
        } else {
            SyncTeleportRequestPacket.Payload payload = new SyncTeleportRequestPacket.Payload(
                    UUID.randomUUID(),
                    followerUuid,
                    target.playerUuid(),
                    target.serverName(),
                    target.serverInstanceId(),
                    System.currentTimeMillis() + REQUEST_TTL_MILLIS
            );
            presence.publishTeleportRequest(payload);
            platform.getProxyTopologyService().connectToInstance(followerUuid, target.serverInstanceId());
        }

        sender.sendMessage(platform.getMessageService().getComponent(
                MessagesKeys.FOLLOW_ENABLED,
                Map.of("tg_player", target.playerName())
        ));
    }
}
