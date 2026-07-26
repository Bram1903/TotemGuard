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

package com.deathmotion.totemguard.common.replay.viewer;

final class ViewerClock {

    private final long origin;
    private final long end;

    private long anchorRecording;
    private long anchorWall;
    private double speed = 1.0;
    private boolean paused;

    ViewerClock(long origin, long end) {
        this.origin = origin;
        this.end = end;
        this.anchorRecording = origin;
        this.anchorWall = System.nanoTime();
    }

    synchronized long position() {
        if (paused) return anchorRecording;
        long advanced = anchorRecording + (long) ((System.nanoTime() - anchorWall) * speed);
        return Math.min(advanced, end);
    }

    synchronized long elapsed() {
        return position() - origin;
    }

    synchronized boolean paused() {
        return paused;
    }

    synchronized double speed() {
        return speed;
    }

    synchronized boolean finished() {
        return position() >= end;
    }

    synchronized void pause(boolean value) {
        reanchor();
        this.paused = value;
    }

    synchronized void speed(double value) {
        reanchor();
        this.speed = value;
    }

    synchronized long moveTo(long recordingNanos) {
        this.anchorRecording = Math.max(origin, Math.min(end, recordingNanos));
        this.anchorWall = System.nanoTime();
        return this.anchorRecording;
    }

    private void reanchor() {
        this.anchorRecording = position();
        this.anchorWall = System.nanoTime();
    }
}
