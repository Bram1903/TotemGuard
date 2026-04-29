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

package com.deathmotion.totemguard.common.placeholder.holder.impl;

import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.placeholder.engine.InternalContext;
import com.deathmotion.totemguard.common.placeholder.holder.MapResolverHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public final class CheckPlaceholders extends MapResolverHolder<CheckImpl> {

    private static final Map<String, Function<CheckImpl, String>> RESOLVERS = Map.of(
            "tg_check_name", CheckImpl::getName,
            "tg_check_description", CheckImpl::getDescription,
            "tg_check_type", c -> c.getType().name(),
            "tg_check_experimental", c -> String.valueOf(c.isExperimental()),
            "tg_check_requires_tick_end", c -> String.valueOf(c.requiresTickEnd()),
            "tg_check_enabled", c -> String.valueOf(c.isEnabled()),
            "tg_check_punishable", c -> String.valueOf(c.isPunishable()),
            "tg_check_violations", c -> String.valueOf(c.getViolations()),
            "tg_check_max_violations", c -> c.isPunishable() ? String.valueOf(c.getMaxViolations()) : "∞"
    );

    public CheckPlaceholders() {
        super(RESOLVERS);
    }

    @Override
    protected @Nullable CheckImpl subject(@NotNull InternalContext ctx) {
        return ctx.check();
    }
}
