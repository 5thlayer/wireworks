// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The Accumulator in FE: Factorio's 5 MJ and 300 kW at 100 J per FE. */
class AccumulatorSpecTest {

    @AfterEach
    void restoreDefaults() {
        AccumulatorSpec.configure(AccumulatorSpec.DEFAULT_CAPACITY_JOULES, AccumulatorSpec.DEFAULT_MAX_WATTS);
    }

    @Test
    void holdsFiftyThousandFe() {
        assertEquals(50_000L, AccumulatorSpec.capacityFe());
    }

    @Test
    void movesOneHundredFiftyFePerTickEachWay() {
        assertEquals(150L, AccumulatorSpec.inputFePerTick());
        assertEquals(150L, AccumulatorSpec.outputFePerTick());
    }

    @Test
    void aConfiguredCapacityAndFlowMoveTheLimits() {
        AccumulatorSpec.configure(10_000_000L, 600_000L);
        assertEquals(100_000L, AccumulatorSpec.capacityFe());
        assertEquals(300L, AccumulatorSpec.inputFePerTick());
        assertEquals(300L, AccumulatorSpec.outputFePerTick());
    }

    @Test
    void aNegativeFigureIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> AccumulatorSpec.configure(-1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> AccumulatorSpec.configure(1L, -1L));
    }
}
