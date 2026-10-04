// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The Solar Panel in FE: Factorio's 60 kW peak at 100 J per FE. */
class SolarPanelSpecTest {

    @AfterEach
    void restoreDefaults() {
        SolarPanelSpec.configure(SolarPanelSpec.DEFAULT_PEAK_WATTS);
    }

    @Test
    void peaksAtThirtyFePerTick() {
        assertEquals(30L, SolarPanelSpec.peakFePerTick());
    }

    @Test
    void holdsOneTickOfItsOutput() {
        assertEquals(30L, SolarPanelSpec.bufferFe());
    }

    @Test
    void aConfiguredPeakMovesTheOutputAndTheBuffer() {
        SolarPanelSpec.configure(120_000L);
        assertEquals(60L, SolarPanelSpec.peakFePerTick());
        assertEquals(60L, SolarPanelSpec.bufferFe());
    }

    @Test
    void aNegativePeakIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> SolarPanelSpec.configure(-1L));
    }
}
