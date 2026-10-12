// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The Steam Turbine's calibration: Factorio's 60 units a second for 5.82 MW, as whole millibuckets (ADR 0012). */
class SteamTurbineSpecTest {

    private static final SteamEngineSpec SPEC = SteamTurbineSpec.FACTORIO;

    @Test
    @DisplayName("a Turbine burns 3 mB of superheated steam each tick for 2,910 FE")
    void everyTick() {
        SteamEngineSpec.Carry carry = SteamEngineSpec.Carry.NONE;
        for (int t = 0; t < 20; t++) {
            SteamEngineSpec.Tick tick = SPEC.tick(carry, Long.MAX_VALUE);
            assertEquals(3, tick.steam());
            assertEquals(2_910L, tick.energy());
            carry = tick.carry();
        }
    }

    @Test
    @DisplayName("seven Turbines burn the Reactor's 21 mB/t")
    void sevenToAReactor() {
        assertEquals(21.0 * 20, 7 * SPEC.steamPerSecond(), 1e-9);
    }

    @Test
    @DisplayName("its buffer and tank are one tick of output and Factorio's 200-unit box")
    void buffers() {
        assertEquals(2_910L, SPEC.bufferCapacity());
        assertEquals(200, SPEC.portCapacity());
    }

    @Test
    @DisplayName("a rate off a whole millibucket is refused")
    void refusesAFractionalRate() {
        assertThrows(IllegalArgumentException.class, () -> SteamEngineSpec.wholeMillibuckets(0.5, 900_000.0, 200));
    }
}
