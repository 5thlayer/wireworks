// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The Reactor's numbers (ADR 0012): Factorio's 40 MW would make 20.6 mB/t of superheated steam, which
 * is not a whole millibucket, so the Reactor makes the 21 mB/t of exactly seven Turbines, and a cell
 * lasts Factorio's 200 seconds.
 */
class ReactorSpecTest {

    @Test
    @DisplayName("the Reactor makes 21 mB of superheated steam a tick")
    void rate() {
        assertEquals(21, ReactorSpec.MILLIBUCKETS_PER_TICK);
    }

    @Test
    @DisplayName("Factorio's 40 MW needs seven 5.82 MW Turbines, rounded up to a whole one")
    void sevenTurbines() {
        assertEquals(7, ReactorSpec.TURBINES);
        assertEquals(7, ReactorSpec.turbinesFor(40_000_000.0, SteamTurbineSpec.FACTORIO));
        assertEquals(6, ReactorSpec.turbinesFor(5_820_000.0 * 6, SteamTurbineSpec.FACTORIO));
    }

    @Test
    @DisplayName("a cell lasts 4,000 ticks of running, Factorio's 200 seconds")
    void cellDuration() {
        assertEquals(4_000, ReactorSpec.CELL_TICKS);
    }

    @Test
    @DisplayName("the steam a whole number of Turbines burns lands on a whole millibucket")
    void wholeMillibuckets() {
        assertEquals(21, ReactorSpec.milliBucketsPerTick(7, SteamTurbineSpec.FACTORIO));
        // Two Steam Engines burn 1.5 mB/t each, three a tick between them.
        assertEquals(3, ReactorSpec.milliBucketsPerTick(2, SteamEngineSpec.FACTORIO));
    }

    @Test
    @DisplayName("a rate off a whole millibucket is refused")
    void refusesAFractionalRate() {
        assertThrows(IllegalArgumentException.class, () -> ReactorSpec.milliBucketsPerTick(7, SteamEngineSpec.FACTORIO));
    }

    @Test
    @DisplayName("each tank holds 10,000 mB, the Boiler's 2,000 scaled to the larger footprint")
    void tanks() {
        assertEquals(10_000, ReactorSpec.WATER_CAPACITY);
        assertEquals(10_000, ReactorSpec.STEAM_CAPACITY);
    }
}
