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

package com.deathmotion.totemguard.common.replay.viewer.camera;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CameraTrack {

    public static final long STEP_NANOS = 50_000_000L;
    public static final int MAX_SAMPLES = 72_000;

    private static final int AIM_RADIUS = 4;
    private static final int BOOM_RADIUS = 14;
    private static final double PITCH_INFLUENCE = 0.55;
    private static final double SUBJECT_EYE_HEIGHT = 1.62;

    private final long startNanos;
    private final double[] aimX;
    private final double[] aimY;
    private final double[] aimZ;
    private final double[] boomX;
    private final double[] boomY;
    private final double[] boomZ;
    private final double[] boomYaw;
    private final double[] boomPitch;

    private CameraTrack(long startNanos, double[] aimX, double[] aimY, double[] aimZ,
                        double[] boomX, double[] boomY, double[] boomZ,
                        double[] boomYaw, double[] boomPitch) {
        this.startNanos = startNanos;
        this.aimX = aimX;
        this.aimY = aimY;
        this.aimZ = aimZ;
        this.boomX = boomX;
        this.boomY = boomY;
        this.boomZ = boomZ;
        this.boomYaw = boomYaw;
        this.boomPitch = boomPitch;
    }

    public static @Nullable CameraTrack of(List<Sample> samples) {
        int taken = samples.size();
        if (taken < 4) return null;

        long start = samples.get(0).nanos();
        long end = samples.get(taken - 1).nanos();
        long span = end - start;
        if (span < STEP_NANOS) return null;

        int steps = (int) Math.min(MAX_SAMPLES, span / STEP_NANOS + 1);
        double[] x = new double[steps];
        double[] y = new double[steps];
        double[] z = new double[steps];
        double[] yaw = new double[steps];
        double[] pitch = new double[steps];

        double[] unwrapped = unwrapYaw(samples);
        int cursor = 0;
        for (int i = 0; i < steps; i++) {
            long at = start + i * STEP_NANOS;
            while (cursor + 2 < taken && samples.get(cursor + 1).nanos() <= at) cursor++;
            Sample before = samples.get(cursor);
            Sample after = samples.get(Math.min(cursor + 1, taken - 1));
            long gap = after.nanos() - before.nanos();
            double blend = gap <= 0 ? 0.0 : Math.min(1.0, Math.max(0.0, (double) (at - before.nanos()) / gap));

            x[i] = lerp(before.x(), after.x(), blend);
            y[i] = lerp(before.y(), after.y(), blend);
            z[i] = lerp(before.z(), after.z(), blend);
            yaw[i] = lerp(unwrapped[cursor], unwrapped[Math.min(cursor + 1, taken - 1)], blend);
            pitch[i] = lerp(before.pitch(), after.pitch(), blend);
        }

        return new CameraTrack(start,
                smooth(x, AIM_RADIUS), smooth(y, AIM_RADIUS), smooth(z, AIM_RADIUS),
                smooth(x, BOOM_RADIUS), smooth(y, BOOM_RADIUS), smooth(z, BOOM_RADIUS),
                smooth(yaw, BOOM_RADIUS), smooth(pitch, BOOM_RADIUS));
    }

    public static CameraPose orbit(double x, double y, double z, float yaw, float pitch,
                                   double distance, double height) {
        return boom(x, y, z, yaw, pitch, x, y, z, distance, height);
    }

    private static CameraPose boom(double bx, double by, double bz, double yaw, double pitch,
                                   double ax, double ay, double az, double distance, double height) {
        double heading = Math.toRadians(yaw);
        double lift = Math.toRadians(pitch * PITCH_INFLUENCE);
        double flat = Math.cos(lift) * distance;

        double camX = bx + Math.sin(heading) * flat;
        double camZ = bz - Math.cos(heading) * flat;
        double camY = by + height + Math.sin(lift) * distance;

        return look(camX, camY, camZ, ax, ay + SUBJECT_EYE_HEIGHT, az);
    }

    private static CameraPose look(double fromX, double fromY, double fromZ,
                                   double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, flat));
        return new CameraPose(fromX, fromY, fromZ, yaw, pitch);
    }

    private static double[] unwrapYaw(List<Sample> samples) {
        int taken = samples.size();
        double[] out = new double[taken];
        double running = samples.get(0).yaw();
        double previous = running;
        out[0] = running;
        for (int i = 1; i < taken; i++) {
            double raw = samples.get(i).yaw();
            running += wrapDegrees(raw - previous);
            previous = raw;
            out[i] = running;
        }
        return out;
    }

    private static double[] smooth(double[] source, int radius) {
        if (radius <= 0 || source.length == 0) return source.clone();

        double sigma = radius / 2.0;
        double[] kernel = new double[radius * 2 + 1];
        for (int k = -radius; k <= radius; k++) {
            double step = k / sigma;
            kernel[k + radius] = Math.exp(-0.5 * step * step);
        }

        int last = source.length - 1;
        double[] out = new double[source.length];
        for (int i = 0; i < source.length; i++) {
            double sum = 0.0;
            double weight = 0.0;
            for (int k = -radius; k <= radius; k++) {
                int at = Math.min(last, Math.max(0, i + k));
                double share = kernel[k + radius];
                sum += source[at] * share;
                weight += share;
            }
            out[i] = sum / weight;
        }
        return out;
    }

    private static double lerp(double from, double to, double blend) {
        return from + (to - from) * blend;
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) wrapped -= 360.0;
        if (wrapped < -180.0) wrapped += 360.0;
        return wrapped;
    }

    public @Nullable CameraPose follow(long nanos, double distance, double height) {
        int last = aimX.length - 1;
        double index = (double) (nanos - startNanos) / STEP_NANOS;
        if (index < 0.0 || index > last) return null;

        int low = (int) Math.floor(index);
        int high = Math.min(low + 1, last);
        double blend = index - low;

        double bx = lerp(boomX[low], boomX[high], blend);
        double by = lerp(boomY[low], boomY[high], blend);
        double bz = lerp(boomZ[low], boomZ[high], blend);
        double bYaw = lerp(boomYaw[low], boomYaw[high], blend);
        double bPitch = lerp(boomPitch[low], boomPitch[high], blend);

        double ax = lerp(aimX[low], aimX[high], blend);
        double ay = lerp(aimY[low], aimY[high], blend);
        double az = lerp(aimZ[low], aimZ[high], blend);

        return boom(bx, by, bz, bYaw, bPitch, ax, ay, az, distance, height);
    }

    public record Sample(long nanos, double x, double y, double z, float yaw, float pitch) {
    }
}
