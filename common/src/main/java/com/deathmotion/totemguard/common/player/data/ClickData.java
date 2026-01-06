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

package com.deathmotion.totemguard.common.player.data;

import com.deathmotion.totemguard.common.TGPlatform;
import lombok.Getter;

@Getter
public class ClickData {

    private int ticks;
    private boolean cpsUpdated;

    private int leftClicksPerSecond;
    private int rightClicksPerSecond;

    private int leftClicks;
    private int rightClicks;

    public void recordLeftClick() {
        leftClicks++;
    }

    public void recordRightClick() {
        rightClicks++;
    }

    public void tick() {
        ticks++;

        if (ticks < 20) return;

        cpsUpdated = true;
        leftClicksPerSecond = leftClicks;
        rightClicksPerSecond = rightClicks;

        ticks = 0;
        leftClicks = 0;
        rightClicks = 0;

        if (leftClicksPerSecond == 0 && rightClicksPerSecond == 0) return;
        //TGPlatform.getInstance().getLogger().info("CPS Updated: L=" + leftClicksPerSecond + " R=" + rightClicksPerSecond);
    }

    public void checkPost() {
        cpsUpdated = false;
    }
}

