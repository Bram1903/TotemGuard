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

package com.deathmotion.totemguard.common.alert;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class AlertBuilder {

    private AlertBuilder() {
    }

    public static String build(CheckImpl check, @Nullable String debug) {
        // We will pull this from the config later
        String format = "%tg_prefix% &e%tg_player%&7 failed &6%tg_check% &7VL[&6%tg_check_violations%/∞&7]";
        if (debug != null) format += " &7(&8%tg_check_debug%&7)";

        Map<String, Object> extras = Map.of(
                "tg_check_violations", check.getViolations(),
                "tg_check_debug", debug == null ? "" : debug
        );

        return TGPlatform.getInstance()
                .getPlaceholderRepository()
                .replace(format, check.player, check, extras);
    }

}
