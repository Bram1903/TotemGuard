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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayFrame;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayProvider;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;
import net.kyori.adventure.text.Component;

public final class WorldDebugProvider implements DebugOverlayProvider {

    private static Component label(String text) {
        return Component.text(text, Palette.LABEL);
    }

    private static Component value(String text) {
        return Component.text(text, Palette.BRAND);
    }

    private static Component separator() {
        return Component.text(" | ", Palette.SEPARATOR);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static String fluidLevel(WrappedBlockState state) {
        StateType type = state.getType();
        if (type == StateTypes.WATER || type == StateTypes.LAVA) {
            return Integer.toString(state.getLevel());
        }
        return "-";
    }

    private static boolean waterlogged(WrappedBlockState state) {
        return state.hasProperty(StateValue.WATERLOGGED) && state.isWaterlogged();
    }

    @Override
    public String getKey() {
        return "world";
    }

    @Override
    public String getDisplayName() {
        return "World";
    }

    @Override
    public DebugOverlayFrame buildFrame(TGPlayer player) {
        Data data = player.getData();
        BlockReader reader = player.getWorldMirror().reader();
        Location current = data.getMovementData().getCurrent();

        int x = floor(current.getX());
        int y = floor(current.getY());
        int z = floor(current.getZ());

        boolean loaded = reader.columnLoaded(x >> 4, z >> 4);
        WrappedBlockState feet = reader.state(x, y, z);
        WrappedBlockState below = reader.state(x, y - 1, z);

        Component line = Component.empty()
                .append(label("Chunk "))
                .append(Component.text(loaded ? "loaded" : "missing", loaded ? Palette.SUCCESS : Palette.DANGER))
                .append(separator())
                .append(label("feet "))
                .append(value(feet.getType().getName()))
                .append(separator())
                .append(label("lvl "))
                .append(value(fluidLevel(feet)))
                .append(separator())
                .append(label("wet "))
                .append(Component.text(waterlogged(feet) ? "yes" : "no", waterlogged(feet) ? Palette.WARN : Palette.SUCCESS))
                .append(separator())
                .append(label("pend "))
                .append(Component.text(reader.uncertain(x, y, z) || reader.uncertain(x, y - 1, z) ? "yes" : "no",
                        Palette.CAPTION))
                .append(separator())
                .append(label("below "))
                .append(value(below.getType().getName()))
                .append(separator())
                .append(label("at "))
                .append(Component.text(x + " " + y + " " + z, Palette.CAPTION));

        return DebugOverlayFrame.of(line);
    }
}
