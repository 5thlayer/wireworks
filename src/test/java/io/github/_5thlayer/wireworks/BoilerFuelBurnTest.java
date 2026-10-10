// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Each fuel lasts in the Boiler as long as the same MJ does in Factorio's 1.8 MW boiler: vanilla's
 * burn time at {@link BoilerFuel#JOULES_PER_BURN_TICK}, run through {@link BoilerCycle} at the draw
 * the block entity uses.
 */
class BoilerFuelBurnTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "planks,      300,   8,    30000",
            "coal,        1600,  44,   40000",
            "blaze rod,   2400,  66,   60000",
            "lava bucket, 20000, 555,  50000",
    })
    void oneItemBoilsForFactoriosDuration(String fuel, int burnTicks, int wholeTicks, long banked) {
        long joules = BoilerFuel.joules(burnTicks);
        FuelBuffer buffer = new FuelBuffer();
        int[] items = {1};
        int ticks = 0;
        while (BoilerCycle.tick(1000, 1000, 1, BoilerSpec.JOULES_PER_TICK, buffer,
                () -> items[0]-- > 0 ? joules : 0L) > 0) {
            ticks++;
        }
        assertEquals(wholeTicks, ticks);
        assertEquals(banked, buffer.storedJoules());
    }

    @org.junit.jupiter.api.Test
    void coalIsFactoriosFourMegajoules() {
        assertEquals(4_000_000L, BoilerFuel.joules(1600));
    }

    @org.junit.jupiter.api.Test
    void whatDoesNotBurnIsWorthNothing() {
        assertEquals(0L, BoilerFuel.joules(0));
        assertEquals(0L, BoilerFuel.joules(-1));
    }
}
