/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

import com.deathmotion.totemguard.api.event.impl.TGUserJoinEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckManagerImpl;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.deathmotion.totemguard.common.player.processor.PreProcessor;
import com.deathmotion.totemguard.common.player.processor.impl.ActionProcessor;
import com.deathmotion.totemguard.common.player.processor.impl.BundleProcessor;
import com.deathmotion.totemguard.common.player.processor.impl.ClientBrandProcessor;
import com.deathmotion.totemguard.common.player.processor.impl.WindowProcessor;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

/**
 * Represents a player in TotemGuard. This object is bound to a single player and gets removed once the player leaves the server / proxy.
 */
@Getter
public class TGPlayer implements TGUser {

    private final UUID uuid;
    private final User user;
    private final CheckManagerImpl checkManager;
    private final PacketStateData packetStateData;

    private final List<PreProcessor> preProcessors;

    private boolean hasLoggedIn;
    private PlatformUser platformUser;
    private @Nullable PlatformPlayer platformPlayer;

    @Setter()
    private String clientBrand;

    public TGPlayer(@NotNull User user) {
        this.uuid = user.getUUID();
        this.user = user;
        this.checkManager = new CheckManagerImpl(this);
        this.packetStateData = new PacketStateData();

        this.preProcessors = new ArrayList<>() {{
            add(new BundleProcessor(TGPlayer.this));
            add(new ClientBrandProcessor(TGPlayer.this));
            add(new ActionProcessor(TGPlayer.this));
            add(new WindowProcessor(TGPlayer.this));
        }};
    }

    public void onLogin() {
        TGPlatform platform = TGPlatform.getInstance();
        PlayerRepositoryImpl playerRepository = platform.getPlayerRepository();

        PlatformUserCreation platformUserCreation = platform.getPlatformUserFactory().create(uuid);
        if (platformUserCreation == null) {
            playerRepository.removeUser(user);
            return;
        }

        platformUser = platformUserCreation.getPlatformUser();
        platformPlayer = platformUserCreation.getPlatformPlayer();

        if (!playerRepository.shouldCheck(user, platformUser)) {
            playerRepository.removeUser(user);
            return;
        }

        hasLoggedIn = true;
        platform.getEventRepository().post(new TGUserJoinEvent(this));
    }

    @Override
    public @NotNull String getName() {
        return user.getName();
    }

    public ClientVersion getClientVersion() {
        // If temporarily null, assume server version...
        return Objects.requireNonNullElseGet(user.getClientVersion(), () -> ClientVersion.getById(PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion()));
    }

    public boolean isTickEndPacket(PacketTypeCommon packetType) {
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && packetType == PacketType.Play.Client.CLIENT_TICK_END) {
            return true;
        }

        return isFlying(packetType);
    }
}
