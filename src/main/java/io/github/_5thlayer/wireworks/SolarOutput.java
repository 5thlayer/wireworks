// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * Whole FE a tick from a fractional rate: the part of an FE not yet paid is carried to the next
 * tick, so the total over a day is exact.
 *
 * <p>Pure: no Minecraft types.
 */
public final class SolarOutput {

    private static final double EPSILON = 1e-9;

    private SolarOutput() {
    }

    /** What one tick pays out, and the fraction of an FE still owed. */
    public record Tick(long fe, double carry) {
    }

    public static Tick tick(long peakFePerTick, double multiplier, double carry) {
        double exact = peakFePerTick * multiplier + carry;
        long whole = (long) Math.floor(exact + EPSILON);
        return new Tick(whole, Math.max(0.0, exact - whole));
    }
}
