// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * What a fuel item is worth to the Boiler, from the burn time vanilla gives it.
 *
 * <p>2,500 J per burn tick makes coal's 1,600 ticks Factorio's 4 MJ, so a Boiler burns an item for
 * as long as Factorio's does, and what burns in a furnace burns here. Pure: no Minecraft types.
 */
public final class BoilerFuel {

    public static final long JOULES_PER_BURN_TICK = 2_500L;

    private BoilerFuel() {
    }

    /** Joules a fuel of this many burn ticks banks, effectivity applied; 0 for a stack that is not fuel. */
    public static long joules(int burnTicks) {
        return burnTicks <= 0 ? 0L : Math.round(burnTicks * JOULES_PER_BURN_TICK * BoilerSpec.EFFECTIVITY);
    }
}
