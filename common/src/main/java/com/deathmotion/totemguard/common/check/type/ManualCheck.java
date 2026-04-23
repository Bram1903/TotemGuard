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

package com.deathmotion.totemguard.common.check.type;

import com.deathmotion.totemguard.api3.check.Check;

/**
 * Marker for checks that are never driven by packets or events and only fire
 * through an explicit staff-triggered entry point — today, {@code /tg check}.
 * They still live in {@link com.deathmotion.totemguard.common.check.CheckManagerImpl}
 * so they pick up config, snapshot/restore, and the alert/punishment pipeline.
 */
public interface ManualCheck extends Check {
}
