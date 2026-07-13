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

package com.deathmotion.totemguard.common.player.debug.provider;

import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayFrame;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayProvider;
import com.deathmotion.totemguard.common.util.Palette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public final class PhysicsDebugProvider implements DebugOverlayProvider {

    private static Component field(String label, String value, TextColor valueColor) {
        return Component.text(label, Palette.LABEL).append(Component.text(value + " ", valueColor));
    }

    @Override
    public String getKey() {
        return "physics";
    }

    @Override
    public String getDisplayName() {
        return "Physics";
    }

    @Override
    public DebugOverlayFrame buildFrame(TGPlayer player) {
        PhysicsVerdict verdict = player.getPhysics().verdict();

        double horizontalExcess = Math.max(verdict.horizontalExcess(), verdict.phaseExcess());
        double verticalExcess = Math.max(verdict.ascentExcess(), verdict.descentExcess());
        boolean hOffense = horizontalExcess > 0.0;
        boolean vOffense = verticalExcess > 0.0;
        double cap = Math.hypot(verdict.boundCenterX(), verdict.boundCenterZ()) + verdict.boundRadius();

        Component horizontal = Component.empty()
                .append(field("H ", hOffense ? "over" : "ok", hOffense ? Palette.DANGER : Palette.SUCCESS))
                .append(field("sp", String.format("%.3f", verdict.observedSpeed()), Palette.BRAND))
                .append(field("<=", String.format("%.3f", cap), Palette.BRAND))
                .append(field("ex", String.format("%.4f", horizontalExcess), Palette.BRAND));

        String state = verdict.breach() != null
                ? verdict.breach().name().toLowerCase()
                : verdict.declineReason() != null
                  ? verdict.declineReason().name().toLowerCase()
                  : verdict.outcome().name().toLowerCase();
        Component vertical = Component.empty()
                .append(field("V ", vOffense ? "over" : "ok", vOffense ? Palette.DANGER : Palette.SUCCESS))
                .append(field("vy", String.format("%.3f", verdict.observedY()), Palette.BRAND))
                .append(field("ex", String.format("%.4f", verticalExcess), Palette.BRAND))
                .append(field("", verdict.medium().name().toLowerCase() + "/" + verdict.ground().name().toLowerCase(),
                        Palette.CAPTION))
                .append(field("", state, Palette.CAPTION));

        return DebugOverlayFrame.of(horizontal, vertical);
    }
}
