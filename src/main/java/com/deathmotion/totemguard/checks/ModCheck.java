/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.checks;

import com.deathmotion.totemguard.checks.type.SignCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.SignUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;

import java.util.List;
import java.util.UUID;

public abstract class ModCheck extends Check implements SignCheck {

    private final UUID packetSecret = UUID.randomUUID();
    private final List<String> keys;

    public ModCheck(TotemPlayer player, List<String> keys) {
        super(player);

        final Class<?> checkClass = this.getClass();
        if (!checkClass.isAnnotationPresent(CheckData.class)) {
            throw new IllegalArgumentException("Check class must be annotated with @CheckData");
        }

        final CheckData checkData = checkClass.getAnnotation(CheckData.class);

        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("The check " + checkData.name() + " doesn't specify any detections");
        }

        if (keys.size() > 3) {
            throw new IllegalArgumentException("The check " + checkData.name() + " specifies more than 3 detections");
        }

        this.keys = keys;
    }

    @Override
    public void placeSign() {
        SignUtil.placeSign(player, packetSecret, keys);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) return;
        String[] lines;

        try {
            lines = new WrapperPlayClientUpdateSign(event).getTextLines();
        } catch (Exception e) {
            return;
        }

        if (SignUtil.isSignContentValid(packetSecret, lines, keys)) {
            fail();
        }
    }
}
