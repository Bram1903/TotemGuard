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

package com.deathmotion.totemguard.discord.alert;

import com.deathmotion.totemguard.api.event.events.TGDiagnosticEvent;
import com.deathmotion.totemguard.discord.ui.Colors;
import com.deathmotion.totemguard.discord.ui.Cv2;
import com.deathmotion.totemguard.discord.ui.Format;
import net.dv8tion.jda.api.components.container.Container;
import org.jetbrains.annotations.NotNull;

final class DiagnosticComponentFactory {
    private DiagnosticComponentFactory() {
    }

    static @NotNull Container build(@NotNull TGDiagnosticEvent event) {
        Cv2 card = Cv2.container(Colors.forSeverity(event.getSeverity()))
                .heading(headline(event.getSeverity()) + " " + event.getSubsystem())
                .text(event.getMessage())
                .field("Server", "`" + event.getServerName() + "`");

        if (event.getStackTrace() != null && !event.getStackTrace().isBlank()) {
            card.divider().codeBlock(event.getStackTrace());
        }

        return card.subtle(Format.dateTime(event.getTimestamp())).build();
    }

    private static String headline(TGDiagnosticEvent.Severity severity) {
        return switch (severity) {
            case INFO -> "Recovered:";
            case WARNING -> "Warning:";
            case ERROR -> "Error:";
            case CRITICAL -> "Critical:";
        };
    }
}
