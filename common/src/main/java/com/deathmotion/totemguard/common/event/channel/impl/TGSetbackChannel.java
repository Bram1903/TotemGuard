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

package com.deathmotion.totemguard.common.event.channel.impl;

import com.deathmotion.totemguard.api.event.events.TGSetbackEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;

public final class TGSetbackChannel extends EventChannelImpl<TGSetbackEvent> {

    public TGSetbackChannel() {
        super(TGSetbackEvent.class);
    }

    public boolean fire(@NotNull TGUser user, double fromX, double fromY, double fromZ,
                        double toX, double toY, double toZ, boolean packetCorrection) {
        if (isEmpty()) return false;
        Holder event = new Holder(user, fromX, fromY, fromZ, toX, toY, toZ, packetCorrection);
        dispatch(event);
        return event.cancelled;
    }

    private static final class Holder implements TGSetbackEvent {
        private final TGUser user;
        private final double fromX;
        private final double fromY;
        private final double fromZ;
        private final double toX;
        private final double toY;
        private final double toZ;
        private final boolean packetCorrection;
        boolean cancelled;

        Holder(TGUser user, double fromX, double fromY, double fromZ,
               double toX, double toY, double toZ, boolean packetCorrection) {
            this.user = user;
            this.fromX = fromX;
            this.fromY = fromY;
            this.fromZ = fromZ;
            this.toX = toX;
            this.toY = toY;
            this.toZ = toZ;
            this.packetCorrection = packetCorrection;
        }

        @Override
        public @NotNull TGUser getUser() {
            return user;
        }

        @Override
        public double getFromX() {
            return fromX;
        }

        @Override
        public double getFromY() {
            return fromY;
        }

        @Override
        public double getFromZ() {
            return fromZ;
        }

        @Override
        public double getToX() {
            return toX;
        }

        @Override
        public double getToY() {
            return toY;
        }

        @Override
        public double getToZ() {
            return toZ;
        }

        @Override
        public boolean isPacketCorrection() {
            return packetCorrection;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}
