/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.util;

import com.deathmotion.totemguard.util.datastructure.Pair;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for various mathematical operations.
 */
@UtilityClass
public class MathUtil {

    /**
     * Constant used to expand values.
     */
    public final double expander = Math.pow(2, 24);

    /**
     * Sums the result of applying a function to each element in the provided dataset.
     *
     * @param function The function to apply to each element.
     * @param dataSet  The dataset of numbers.
     * @return The sum of the results.
     */
    public double sum(Function<Number, Double> function, Collection<? extends Number> dataSet) {
        return dataSet.stream()
                .map(function)
                .mapToDouble(i -> i)
                .sum();
    }

    public Collection<Long> calculateIntervals(final Collection<Long> collectionOne, final Collection<Long> collectionTwo) {
        int size = Math.min(collectionOne.size(), collectionTwo.size());
        List<Long> intervals = new ArrayList<>(size);

        var collectionOneIter = collectionOne.iterator();
        var collectionTwoIter = collectionTwo.iterator();

        while (collectionOneIter.hasNext() && collectionTwoIter.hasNext()) {
            Long collectionOneTime = collectionOneIter.next();
            Long collectionTwoTime = collectionTwoIter.next();

            if (collectionOneTime != null && collectionTwoTime != null) {
                long interval = collectionTwoTime - collectionOneTime;
                intervals.add(interval);
            }
        }

        return intervals;
    }


    /**
     * Finds the outliers in a dataset based on the Interquartile Range (IQR).
     *
     * @param collection The dataset to check for outliers.
     * @return A pair where the first list contains low outliers and the second list contains high outliers.
     */
    public Pair<List<Double>, List<Double>> getOutliers(final Collection<? extends Number> collection) {
        final List<Double> values = new ArrayList<>();

        for (final Number number : collection) {
            values.add(number.doubleValue());
        }

        final double q1 = getMedian(values.subList(0, values.size() / 2));
        final double q3 = getMedian(values.subList(values.size() / 2, values.size()));

        final double iqr = Math.abs(q1 - q3);
        final double lowThreshold = q1 - 1.5 * iqr, highThreshold = q3 + 1.5 * iqr;

        final Pair<List<Double>, List<Double>> tuple = new Pair<>(new ArrayList<>(), new ArrayList<>());

        for (final Double value : values) {
            if (value < lowThreshold) {
                tuple.getX().add(value);
            } else if (value > highThreshold) {
                tuple.getY().add(value);
            }
        }

        return tuple;
    }

    /**
     * Calculates the variance of a dataset.
     *
     * @param data The dataset of numbers.
     * @return The variance of the dataset.
     */
    public double getVariance(final Collection<? extends Number> data) {
        double mean = getMean(data);
        return sum(i -> Math.pow(i.doubleValue() - mean, 2), data) / (data.size() - 1);
    }

    /**
     * Calculates the standard deviation of a dataset.
     *
     * @param data The dataset of numbers.
     * @return The standard deviation of the dataset.
     */
    public double getStandardDeviation(final Collection<? extends Number> data) {
        final double variance = getVariance(data);
        return fastSqrt(variance);
    }

    /**
     * Calculates the median of a list of doubles.
     *
     * @param data The list of doubles.
     * @return The median value.
     */
    public double getMedian(final List<Double> data) {
        if (data.size() % 2 == 0) {
            return (data.get(data.size() / 2) + data.get(data.size() / 2 - 1)) / 2;
        } else {
            return data.get(data.size() / 2);
        }
    }

    /**
     * Calculates the mean (average) of a collection of numbers.
     *
     * @param iterable The collection of numbers.
     * @return The mean value.
     */
    public double getMean(Collection<? extends Number> iterable) {
        return sum(Number::doubleValue, iterable) / iterable.size();
    }

    /**
     * Calculates the kurtosis of a dataset.
     *
     * @param data The dataset of numbers.
     * @return The kurtosis value.
     */
    public double getKurtosis(final Collection<? extends Number> data) {
        double n = data.size();
        double mean = getMean(data);
        double variance = getVariance(data);

        double a = (n * (n + 1)) / ((n - 1) * (n - 2) * (n - 3));
        double b = sum(i -> Math.pow(i.doubleValue() - mean, 2), data) / Math.pow(variance, 2);

        return a * b;
    }

    /**
     * Calculates the excess kurtosis of a dataset.
     *
     * @param data The dataset of numbers.
     * @return The excess kurtosis value.
     */
    public double getExcessKurtosis(final Collection<? extends Number> data) {
        double n = data.size();
        double a = (3 * Math.pow(n - 1, 2)) / ((n - 2) * (n - 3));
        return getKurtosis(data) - a;
    }

    /**
     * Calculates the skewness of a dataset.
     *
     * @param data The dataset of numbers.
     * @return The skewness value.
     */
    public double getSkewness(final Collection<? extends Number> data) {
        double mean = getMean(data);
        double deviation = getStandardDeviation(data);

        return sum(i -> Math.pow(i.doubleValue() - mean, 3), data) / ((data.size() - 1) * Math.pow(deviation, 3));
    }

    /**
     * Finds the mode (the most frequent value) in a collection of numbers.
     *
     * @param array The collection of numbers.
     * @return The mode.
     */
    public Number getMode(Collection<? extends Number> array) {
        Number mode = (Number) array.toArray()[0];
        int maxCount = 0;
        for (Number value : array) {
            int count = 1;
            for (Number i : array) {
                if (i.equals(value))
                    count++;
                if (count > maxCount) {
                    mode = value;
                    maxCount = count;
                }
            }
        }
        return mode;
    }

    /**
     * Trims a double value to a specified number of decimal places.
     *
     * @param degree The number of decimal places.
     * @param d      The value to be trimmed.
     * @return The trimmed value.
     */
    public double trim(int degree, double d) {
        StringBuilder format = new StringBuilder("#.#");
        for (int i = 1; i < degree; ++i) {
            format.append("#");
        }
        DecimalFormat twoDForm = new DecimalFormat(format.toString());
        return Double.parseDouble(twoDForm.format(d).replaceAll(",", "."));
    }

    /**
     * Finds the greatest common divisor (gcd) of two numbers with a limit.
     *
     * @param limit The limit for the gcd calculation.
     * @param a     The first number.
     * @param b     The second number.
     * @return The greatest common divisor.
     */
    public long gcd(long limit, long a, long b) {
        return (b <= limit) ? a : gcd(limit, b, a % b);
    }

    /**
     * Checks if a number is between two other numbers.
     *
     * @param number The number to check.
     * @param d1     The lower bound.
     * @param d2     The upper bound.
     * @return True if the number is between d1 and d2, false otherwise.
     */
    public boolean isBetween(final double number, final double d1, final double d2) {
        return number > d1 && number < d2;
    }

    /**
     * Checks if two values are approximately equal within a given tolerance.
     *
     * @param amount The tolerance.
     * @param one    The first value.
     * @param two    The second value.
     * @return True if the values are roughly equal, false otherwise.
     */
    public boolean areRoughlyEqual(double amount, double one, double two) {
        return Math.abs(one - two) <= amount;
    }

    /**
     * Calculates the percentage of one value relative to another.
     *
     * @param one The numerator.
     * @param two The denominator.
     * @return The percentage.
     */
    public double getPercentage(double one, double two) {
        return (one / two) * 100;
    }

    /**
     * Calculates the absolute difference between two values.
     *
     * @param g The first value.
     * @param q The second value.
     * @return The absolute difference.
     */
    public double differenceBetween(final double g, final double q) {
        return (g > q) ? (g - q) : (q - g);
    }

    /**
     * Efficient square root approximation.
     *
     * @param n The value to calculate the square root for.
     * @return The square root.
     */
    public double fastSqrt(final double n) {
        return Double.longBitsToDouble((Double.doubleToLongBits(n) - 4503599627370496L >> 1) + 2305843009213693952L);
    }

    /**
     * Returns the value closest to the given test value.
     *
     * @param a         The first value.
     * @param b         The second value.
     * @param closeTest The test value to compare with.
     * @return The value closest to closeTest.
     */
    public double getClosest(double a, double b, double closeTest) {
        return Math.abs(closeTest - a) < Math.abs(closeTest - b) ? a : b;
    }

    /**
     * Calculates the yaw and pitch rotation between two locations.
     *
     * @param one The first location.
     * @param two The second location.
     * @return A vector representing the yaw and pitch rotation.
     */
    public Vector getRotation(Location one, Location two) {
        double dx = two.getX() - one.getX();
        double dy = two.getY() - one.getY();
        double dz = two.getZ() - one.getZ();
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0D / Math.PI);
        return new Vector(yaw, pitch, 0.0F);
    }

    /**
     * Clamps an angle within -180 to 180 degrees.
     *
     * @param theta The angle to clamp.
     * @return The clamped angle.
     */
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

    /**
     * Checks if a double value is in scientific notation.
     *
     * @param d The double value to check.
     * @return True if the value is in scientific notation, false otherwise.
     */
    public boolean isScientificNotation(double d) {
        return String.valueOf(d).contains("E");
    }

    /**
     * Calculates the direction vector given yaw and pitch angles.
     *
     * @param yaw   The yaw angle.
     * @param pitch The pitch angle.
     * @return A vector representing the direction.
     */
    public Vector getDirection(float yaw, float pitch) {
        Vector vector = new Vector();
        float rotX = (float) Math.toRadians(yaw);
        float rotY = (float) Math.toRadians(pitch);
        vector.setY(-Math.sin(rotY));
        double xz = Math.cos(rotY);
        vector.setX(-xz * Math.sin(rotX));
        vector.setZ(xz * Math.cos(rotX));
        return vector;
    }

    /**
     * Calculates the angle between two vectors.
     *
     * @param a The first vector.
     * @param b The second vector.
     * @return The angle in radians.
     */
    public double angle(Vector a, Vector b) {
        double dot = Math.min(Math.max(a.dot(b) / (a.length() * b.length()), -1), 1);
        return Math.acos(dot);
    }

}
