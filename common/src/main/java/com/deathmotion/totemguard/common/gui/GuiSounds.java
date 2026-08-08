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

package com.deathmotion.totemguard.common.gui;

import com.deathmotion.totemguard.common.TGPlatform;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;

public final class GuiSounds {

    public static final Effect ENTER = new Effect(Sounds.BLOCK_CHERRY_WOOD_BUTTON_CLICK_ON, 0.5f, 1.0f);
    public static final Effect BACK = new Effect(Sounds.BLOCK_CHERRY_WOOD_BUTTON_CLICK_OFF, 0.5f, 1.0f);
    public static final Effect CLOSE = new Effect(Sounds.UI_TOAST_OUT, 0.5f, 1.0f);
    public static final Effect FILTER = new Effect(Sounds.ITEM_SPYGLASS_USE, 0.5f, 1.0f);
    public static final Effect DENIED = new Effect(Sounds.ENTITY_VILLAGER_NO, 0.5f, 1.0f);

    private GuiSounds() {
    }

    public static void play(GuiSession session, Effect effect) {
        if (session == null || effect == null) return;
        play(session.user(), effect);
    }

    public static void play(User user, Effect effect) {
        if (user == null || effect == null) return;

        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> user.sendPacket(
                new WrapperPlayServerEntitySoundEffect(
                        effect.sound(),
                        SoundCategory.MASTER,
                        user.getEntityId(),
                        effect.volume(),
                        effect.pitch()
                )
        ));
    }

    public record Effect(Sound sound, float volume, float pitch) {
    }
}
