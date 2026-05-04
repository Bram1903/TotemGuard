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

package com.deathmotion.totemguard.common.event.api.impl;

import com.deathmotion.totemguard.api.event.impl.TGModDetectionResolvedEvent;
import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModAction;
import com.deathmotion.totemguard.api.user.TGUser;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Getter
public final class TGModDetectionResolvedEventImpl extends TGUserEventImpl implements TGModDetectionResolvedEvent {

    private final Set<DetectedMod> detectedMods;
    private final ModAction action;
    private final boolean late;

    @Setter
    private boolean cancelled;

    public TGModDetectionResolvedEventImpl(@NotNull TGUser user,
                                           @NotNull Set<DetectedMod> detectedMods,
                                           @NotNull ModAction action,
                                           boolean late) {
        super(user);
        this.detectedMods = Set.copyOf(detectedMods);
        this.action = action;
        this.late = late;
    }
}
