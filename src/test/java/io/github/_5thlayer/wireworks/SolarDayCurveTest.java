// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SolarDayCurveTest {

    private static final double EPS = 1e-9;

    @Test
    void fullAtNoonAndThroughToDusk() {
        assertEquals(1.0, SolarDayCurve.multiplier(0.0), EPS);
        assertEquals(1.0, SolarDayCurve.multiplier(0.25), EPS);
    }

    @Test
    void nothingThroughTheNight() {
        for (double d = 0.45; d <= 0.55; d += 0.01) {
            assertEquals(0.0, SolarDayCurve.multiplier(Math.min(d, 0.55)), EPS);
        }
        assertEquals(0.0, SolarDayCurve.multiplier(0.5), EPS);
        assertEquals(0.0, SolarDayCurve.multiplier(0.45), EPS);
        assertEquals(0.0, SolarDayCurve.multiplier(0.55), EPS);
    }

    @Test
    void linearOnBothRamps() {
        assertEquals(0.5, SolarDayCurve.multiplier(0.35), EPS);
        assertEquals(0.75, SolarDayCurve.multiplier(0.30), EPS);
        assertEquals(0.5, SolarDayCurve.multiplier(0.65), EPS);
        assertEquals(0.25, SolarDayCurve.multiplier(0.60), EPS);
        assertEquals(1.0, SolarDayCurve.multiplier(0.75), 1e-6);
    }

    @Test
    void meanOverADayIsSevenTenths() {
        int n = 100_000;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += SolarDayCurve.multiplier((i + 0.5) / n);
        }
        assertEquals(0.7, sum / n, 1e-4);
    }

    @Test
    void periodic() {
        for (double d : new double[] {0.0, 0.1, 0.3, 0.5, 0.6, 0.9}) {
            assertEquals(SolarDayCurve.multiplier(d), SolarDayCurve.multiplier(d + 1.0), 1e-9);
            assertEquals(SolarDayCurve.multiplier(d), SolarDayCurve.multiplier(d - 3.0), 1e-9);
        }
    }

    @Test
    void minecraftNoonIsTheCurvesZero() {
        assertEquals(0.0, SolarDayCurve.fromClockFraction(0.25), EPS);
        assertEquals(0.5, SolarDayCurve.fromClockFraction(0.75), EPS);
    }
}
