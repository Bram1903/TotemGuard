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

package com.deathmotion.totemguard.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ProtocolVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.impl.netty.BuildData;
import io.github.retrooper.packetevents.impl.netty.factory.NettyPacketEventsBuilder;
import io.github.retrooper.packetevents.impl.netty.manager.player.PlayerManagerAbstract;
import io.github.retrooper.packetevents.impl.netty.manager.protocol.ProtocolManagerAbstract;
import io.github.retrooper.packetevents.impl.netty.manager.server.ServerManagerAbstract;
import org.jetbrains.annotations.NotNull;

public final class HeadlessBootstrap {

    private static MutableServerManager serverManager;

    private HeadlessBootstrap() {
    }

    public static void load() {
        if (serverManager != null) return;
        serverManager = new MutableServerManager();

        PacketEventsAPI<BuildData> api = NettyPacketEventsBuilder.buildNoCache(
                new BuildData("TotemGuard-Replay"),
                new NoopInjector(),
                new HeadlessProtocolManager(),
                serverManager,
                new HeadlessPlayerManager());

        PacketEvents.setAPI(api);
        api.getSettings().checkForUpdates(false);
        api.init();
    }

    public static void serverVersion(ServerVersion version) {
        load();
        serverManager.version = version;
    }

    public static ServerVersion serverVersion() {
        load();
        return serverManager.version;
    }

    private static final class MutableServerManager extends ServerManagerAbstract {

        private volatile ServerVersion version = ServerVersion.getLatest();

        @Override
        public ServerVersion getVersion() {
            return version;
        }
    }

    private static final class HeadlessProtocolManager extends ProtocolManagerAbstract {

        @Override
        public ProtocolVersion getPlatformVersion() {
            return ProtocolVersion.UNKNOWN;
        }
    }

    private static final class HeadlessPlayerManager extends PlayerManagerAbstract {

        @Override
        public int getPing(@NotNull Object player) {
            return 0;
        }

        @Override
        public Object getChannel(@NotNull Object player) {
            return null;
        }

        @Override
        public User getUser(@NotNull Object player) {
            return null;
        }
    }

    private static final class NoopInjector implements ChannelInjector {

        @Override
        public void inject() {
        }

        @Override
        public void uninject() {
        }

        @Override
        public void updateUser(Object channel, User user) {
        }

        @Override
        public void setPlayer(Object channel, Object player) {
        }

        @Override
        public boolean isPlayerSet(Object channel) {
            return false;
        }

        @Override
        public boolean isProxy() {
            return false;
        }
    }
}
