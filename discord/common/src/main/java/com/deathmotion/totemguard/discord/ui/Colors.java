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

package com.deathmotion.totemguard.discord.ui;

import com.deathmotion.totemguard.api.event.events.TGDiagnosticEvent.Severity;

public final class Colors {
    public static final int BRAND = 0xFBAF00;

    public static final int ACCENT = 0xFEE067;

    public static final int SUCCESS = 0x6FB04A;

    public static final int WARN = 0xE68A2E;

    public static final int ERROR = 0xD4452C;

    private Colors() {
    }

    public static int forSeverity(Severity severity) {
        return switch (severity) {
            case INFO -> SUCCESS;
            case WARNING -> WARN;
            case ERROR, CRITICAL -> ERROR;
        };
    }
}
