// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * What a fuel item is worth to the Boiler, from the burn time vanilla gives it.
 *
 * <p>The default of 2,500 J per burn tick makes coal's 1,600 ticks Factorio's 4 MJ, so a Boiler
 * burns an item for as long as Factorio's does (ADR-0011). Set by {@link WireworksConfig}. Pure: no
 * Minecraft types.
 */
public final class BoilerFuel {

    static final long DEFAULT_JOULES_PER_BURN_TICK = 2_500L;

    private static volatile long joulesPerBurnTick = DEFAULT_JOULES_PER_BURN_TICK;

    private BoilerFuel() {
    }

    static void configure(long joulesPerBurnTick) {
        if (joulesPerBurnTick <= 0L) {
            throw new IllegalArgumentException("joules per burn tick " + joulesPerBurnTick);
        }
        BoilerFuel.joulesPerBurnTick = joulesPerBurnTick;
    }

    public static long joulesPerBurnTick() {
        return joulesPerBurnTick;
    }

    /** Joules a fuel of this many burn ticks banks, effectivity applied; 0 for a stack that is not fuel. */
    public static long joules(int burnTicks) {
        return burnTicks <= 0 ? 0L : Math.round(burnTicks * joulesPerBurnTick * BoilerSpec.EFFECTIVITY);
    }
}
