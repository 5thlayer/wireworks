// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.function.LongSupplier;

/**
 * One tick of the Boiler, and the order its three questions are asked in.
 *
 * <p>A Boiler whose steam has backed up makes no steam, burns no fuel and voids none. Water and room
 * are asked <em>before</em> the fuel buffer is, so a blocked Boiler cannot light an item to pay for a
 * tick it will not run. A tick that cannot convert its whole {@code perTick} converts none of it.
 *
 * <p>Pure: no Minecraft types.
 */
public final class BoilerCycle {

    private BoilerCycle() {
    }

    /**
     * @param waterAvailable how much water is in the input tank, in millibuckets
     * @param outputRoom how much room the steam tank has left, in millibuckets
     * @param perTick what a whole tick converts
     * @param joulesPerTick what a whole tick costs
     * @param ignite consumes one fuel item and returns what it was worth, or 0 when there is nothing
     *     to light. Called only on a tick that already has both water and somewhere to put the steam.
     * @return how many millibuckets of water became steam: {@code perTick}, or zero
     */
    public static int tick(int waterAvailable, int outputRoom, int perTick, long joulesPerTick,
                           FuelBuffer fuel, LongSupplier ignite) {
        if (perTick <= 0 || waterAvailable < perTick || outputRoom < perTick) {
            return 0;
        }
        return pay(joulesPerTick, fuel, ignite) ? perTick : 0;
    }

    /**
     * The buffer first, an item only when it cannot cover the tick. Lighting an item to top up a
     * buffer that would already have paid throws away the remainder, and coal's 4 MJ would stop being
     * 44 whole ticks of boiling.
     */
    private static boolean pay(long joulesPerTick, FuelBuffer fuel, LongSupplier ignite) {
        if (fuel.drawTick(joulesPerTick)) {
            return true;
        }
        long lit = ignite.getAsLong();
        if (lit <= 0L) {
            return false;
        }
        fuel.light(lit);
        return fuel.drawTick(joulesPerTick);
    }
}
