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

import com.deathmotion.totemguard.api.event.events.TGModDetectionResolvedEvent;
import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModAction;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.event.channel.EventChannelImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class TGModDetectionResolvedChannel extends EventChannelImpl<TGModDetectionResolvedEvent> {

    public TGModDetectionResolvedChannel() {
        super(TGModDetectionResolvedEvent.class);
    }

    public boolean fire(@NotNull TGUser user, @NotNull Set<DetectedMod> detectedMods,
                        @NotNull ModAction action, boolean late) {
        if (isEmpty()) return false;
        Holder event = new Holder(user, detectedMods, action, late);
        dispatch(event);
        return event.cancelled;
    }

    private static final class Holder implements TGModDetectionResolvedEvent {
        private final TGUser user;
        private final Set<DetectedMod> detectedMods;
        private final ModAction action;
        private final boolean late;
        boolean cancelled;

        Holder(TGUser user, Set<DetectedMod> detectedMods, ModAction action, boolean late) {
            this.user = user;
            this.detectedMods = detectedMods;
            this.action = action;
            this.late = late;
        }

        @Override
        public @NotNull TGUser getUser() {
            return user;
        }

        @Override
        public @NotNull Set<DetectedMod> getDetectedMods() {
            return detectedMods;
        }

        @Override
        public @NotNull ModAction getAction() {
            return action;
        }

        @Override
        public boolean isLate() {
            return late;
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
