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

package com.deathmotion.totemguard.common.util;

import com.deathmotion.totemguard.common.util.datastructure.Pair;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3f;
import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;

@UtilityClass
public class MathUtil {

    public final double expander = Math.pow(2, 24);

    public double sum(Function<Number, Double> function, Collection<? extends Number> dataSet) {
        return dataSet.stream()
                .map(function)
                .mapToDouble(i -> i)
                .sum();
    }

    /**
     * Tukey IQR outliers.
     * Returns Pair(lowOutliers, highOutliers).
     */
    public Pair<List<Double>, List<Double>> getOutliers(final Collection<? extends Number> collection) {
        final List<Double> values = new ArrayList<>(collection.size());
        for (final Number number : collection) {
            values.add(number.doubleValue());
        }

        values.sort(Double::compareTo);

        // Not enough points to form meaningful quartiles
        if (values.size() < 4) {
            return new Pair<>(new ArrayList<>(), new ArrayList<>());
        }

        final int n = values.size();

        // Split around median; exclude median from both halves when n is odd
        final List<Double> lower = values.subList(0, n / 2);
        final List<Double> upper = values.subList((n + 1) / 2, n);

        final double q1 = getMedianSorted(lower);
        final double q3 = getMedianSorted(upper);

        final double iqr = q3 - q1;
        final double lowThreshold = q1 - 1.5 * iqr;
        final double highThreshold = q3 + 1.5 * iqr;

        final Pair<List<Double>, List<Double>> tuple = new Pair<>(new ArrayList<>(), new ArrayList<>());

        for (final double value : values) {
            if (value < lowThreshold) {
                tuple.getX().add(value);
            } else if (value > highThreshold) {
                tuple.getY().add(value);
            }
        }

        return tuple;
    }

    /**
     * Sample variance (unbiased): sum((x-mean)^2) / (n-1)
     */
    public double getVariance(final Collection<? extends Number> data) {
        final int n = data.size();
        if (n < 2) return Double.NaN;

        final double mean = getMean(data);
        final double ss = sum(x -> {
            final double d = x.doubleValue() - mean;
            return d * d;
        }, data);

        return ss / (n - 1.0);
    }

    /**
     * Sample standard deviation (sqrt of sample variance).
     */
    public double getStandardDeviation(final Collection<? extends Number> data) {
        return Math.sqrt(getVariance(data));
    }

    /**
     * Median of an arbitrary list (this method sorts a copy).
     */
    public double getMedian(final List<Double> data) {
        final List<Double> copy = new ArrayList<>(data);
        copy.sort(Double::compareTo);
        return getMedianSorted(copy);
    }

    /**
     * Median of a list that is already sorted ascending.
     */
    private double getMedianSorted(final List<Double> sorted) {
        final int n = sorted.size();
        if (n == 0) return Double.NaN;

        if (n % 2 == 0) {
            return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        } else {
            return sorted.get(n / 2);
        }
    }

    public double getMean(Collection<? extends Number> iterable) {
        final int n = iterable.size();
        if (n == 0) return Double.NaN;
        return sum(Number::doubleValue, iterable) / n;
    }

    /**
     * Kurtosis (non-excess). Equals excessKurtosis + 3.
     * Uses a small-sample corrected estimator; returns NaN for n < 4.
     */
    public double getKurtosis(final Collection<? extends Number> data) {
        final double excess = getExcessKurtosis(data);
        return Double.isNaN(excess) ? Double.NaN : (excess + 3.0);
    }

    /**
     * Excess kurtosis (Fisher) with small-sample correction.
     * Returns NaN for n < 4 or zero variance.
     */
    public double getExcessKurtosis(final Collection<? extends Number> data) {
        final int n = data.size();
        if (n < 4) return Double.NaN;

        final double mean = getMean(data);

        double m2 = 0.0;
        double m4 = 0.0;
        for (Number x : data) {
            final double d = x.doubleValue() - mean;
            final double d2 = d * d;
            m2 += d2;
            m4 += d2 * d2;
        }

        if (m2 == 0.0) return Double.NaN;

        final double a = (double) n * (n + 1.0) / ((n - 1.0) * (n - 2.0) * (n - 3.0));
        final double b = 3.0 * (n - 1.0) * (n - 1.0) / ((n - 2.0) * (n - 3.0));

        return a * (m4 / (m2 * m2)) - b;
    }

    /**
     * Sample skewness with Fisher-Pearson small-sample correction.
     * Returns NaN for n < 3 or zero variance.
     */
    public double getSkewness(final Collection<? extends Number> data) {
        final int n = data.size();
        if (n < 3) return Double.NaN;

        final double mean = getMean(data);

        double m2 = 0.0;
        double m3 = 0.0;
        for (Number x : data) {
            final double d = x.doubleValue() - mean;
            m2 += d * d;
            m3 += d * d * d;
        }

        if (m2 == 0.0) return Double.NaN;

        // sample standard deviation
        final double s = Math.sqrt(m2 / (n - 1.0));

        return (n * m3) / ((n - 1.0) * (n - 2.0) * s * s * s);
    }

    /**
     * Exact mode based on doubleValue() equality.
     * Returns null if empty.
     */
    public Number getMode(Collection<? extends Number> data) {
        if (data == null || data.isEmpty()) return null;

        Map<Double, Integer> freq = new HashMap<>();
        for (Number x : data) {
            double v = x.doubleValue();
            freq.compute(v, (k, prev) -> prev == null ? 1 : (prev + 1));
        }

        Double mode = null;
        int best = -1;

        for (Map.Entry<Double, Integer> e : freq.entrySet()) {
            int count = e.getValue();
            if (count > best) {
                best = count;
                mode = e.getKey();
            }
        }

        return mode;
    }

    public double trim(int degree, double d) {
        StringBuilder format = new StringBuilder("#.#");
        for (int i = 1; i < degree; ++i) {
            format.append("#");
        }
        DecimalFormat twoDForm = new DecimalFormat(format.toString());
        return Double.parseDouble(twoDForm.format(d).replaceAll(",", "."));
    }

    //0x4000
    public long gcd(long limit, long a, long b) {
        return (b <= limit) ? a : gcd(limit, b, a % b);
    }

    public boolean isBetween(final double number, final double d1, final double d2) {
        return number > d1 && number < d2;
    }

    public boolean areRoughlyEqual(double amount, double one, double two) {
        return Math.abs(one - two) <= amount;
    }

    public double getPercentage(double one, double two) {
        return (one / two) * 100;
    }

    public double differenceBetween(final double g, final double q) {
        return (g > q) ? (g - q) : (q - g);
    }

    public double fastSqrt(final double n) {
        return Math.sqrt(n);
    }

    public double getClosest(double a, double b, double closeTest) {
        return Math.abs(closeTest - a) < Math.abs(closeTest - b) ? a : b;
    }

    public Vector3f getRotation(Location one, Location two) {
        double dx = two.getX() - one.getX();
        double dy = two.getY() - one.getY();
        double dz = two.getZ() - one.getZ();
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0D / Math.PI);
        return new Vector3f(yaw, pitch, 0.0F);
    }

    public double clamp180(double theta) {
        theta %= 360.0D;
        if (theta >= 180.0D) {
            theta -= 360.0D;
        }
        if (theta < -180.0D) {
            theta += 360.0D;
        }
        return theta;
    }

    public boolean isScientificNotation(double d) {
        return String.valueOf(d).contains("E");
    }

    public Vector3f getDirection(float yaw, float pitch) {
        float rotX = (float) Math.toRadians(yaw);
        float rotY = (float) Math.toRadians(pitch);
        float y = (float) -Math.sin(rotY);
        float xz = (float) Math.cos(rotY);
        float x = (float) (-xz * Math.sin(rotX));
        float z = (float) (xz * Math.cos(rotX));
        return new Vector3f(x, y, z);
    }

    /**
     * Angle between vectors in radians.
     * Returns NaN if either vector is zero-length.
     */
    public double angle(Vector3f a, Vector3f b) {
        final double la = length(a);
        final double lb = length(b);
        if (la == 0.0 || lb == 0.0) return Double.NaN;

        double cos = a.dot(b) / (la * lb);
        cos = Math.max(-1.0, Math.min(1.0, cos));
        return Math.acos(cos);
    }

    private double length(Vector3f v) {
        return Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
    }
}
