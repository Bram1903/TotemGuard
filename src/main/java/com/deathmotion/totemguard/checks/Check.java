/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.checks;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.interfaces.ICheck;
import com.deathmotion.totemguard.interfaces.ICheckSettings;
import com.deathmotion.totemguard.models.TotemPlayer;
import lombok.Getter;
import net.kyori.adventure.text.Component;

// Class is heavily expired from https://github.com/Tecnio/AntiCheatBase/blob/master/src/main/java/me/tecnio/anticheat/check/Check.java
@Getter
public class Check implements ICheck {
    protected final TotemPlayer player;

    private String checkName;
    protected ICheckSettings config;
    private String description;
    private boolean experimental;

    private int violations;

    public Check(TotemPlayer player) {
        this.player = player;
        final Class<?> checkClass = this.getClass();

        if (checkClass.isAnnotationPresent(CheckData.class)) {
            final CheckData checkData = checkClass.getAnnotation(CheckData.class);
            this.checkName = checkData.name();
            this.config = TotemGuard.getInstance().getConfigManager().getChecks().getCheckSettings(checkName);
            this.description = checkData.description();
            this.experimental = checkData.experimental();
        }
    }

    public void flag(Component details) {
        violations++;
    }
}
