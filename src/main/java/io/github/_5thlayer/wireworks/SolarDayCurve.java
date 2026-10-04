// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * A Solar Panel's output against the day, as Factorio's surface has it: {@code daytime} 0 is noon,
 * full to dusk at 0.25, linear to nothing at evening 0.45, nothing to morning 0.55, linear to full
 * at dawn 0.75 (Factorio Lua API, {@code LuaSurface.dusk/evening/morning/dawn}). The mean over a day
 * is 0.7.
 *
 * <p>Read against the fraction of the dimension's day, so it holds at any period. Pure: no Minecraft
 * types.
 */
public final class SolarDayCurve {

    public static final double DUSK = 0.25;
    public static final double EVENING = 0.45;
    public static final double MORNING = 0.55;
    public static final double DAWN = 0.75;

    /** Minecraft's noon is this far into its clock's day. */
    public static final double NOON_CLOCK_FRACTION = 0.25;

    private SolarDayCurve() {
    }

    /** The curve's daytime for a clock fraction of the day, where Minecraft's noon is 0. */
    public static double fromClockFraction(double clockFraction) {
        return wrap(clockFraction - NOON_CLOCK_FRACTION);
    }

    /** 0 to 1 for a daytime fraction; periodic. */
    public static double multiplier(double daytime) {
        double d = wrap(daytime);
        if (d <= DUSK) {
            return 1.0;
        }
        if (d < EVENING) {
            return (EVENING - d) / (EVENING - DUSK);
        }
        if (d <= MORNING) {
            return 0.0;
        }
        if (d < DAWN) {
            return (d - MORNING) / (DAWN - MORNING);
        }
        return 1.0;
    }

    private static double wrap(double x) {
        return x - Math.floor(x);
    }
}
