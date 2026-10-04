// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Accumulator's buffer and flow limit in FE, from the joules and watts the config states. It
 * charges and discharges at the same rate.
 *
 * <p>Set by {@link WireworksConfig} when the server config loads or reloads. Pure: no Minecraft types.
 */
public final class AccumulatorSpec {

    static final long DEFAULT_CAPACITY_JOULES = 5_000_000L;
    static final long DEFAULT_MAX_WATTS = 300_000L;

    private static volatile long capacityJoules = DEFAULT_CAPACITY_JOULES;
    private static volatile long maxWatts = DEFAULT_MAX_WATTS;

    private AccumulatorSpec() {
    }

    static void configure(long capacityJoules, long maxWatts) {
        if (capacityJoules < 0L || maxWatts < 0L) {
            throw new IllegalArgumentException("capacity " + capacityJoules + " J, max " + maxWatts + " W");
        }
        AccumulatorSpec.capacityJoules = capacityJoules;
        AccumulatorSpec.maxWatts = maxWatts;
    }

    public static long capacityFe() {
        return capacityJoules / ForgeEnergy.JOULES_PER_FE;
    }

    public static long inputFePerTick() {
        return maxWatts / ForgeEnergy.TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE;
    }

    public static long outputFePerTick() {
        return inputFePerTick();
    }
}
