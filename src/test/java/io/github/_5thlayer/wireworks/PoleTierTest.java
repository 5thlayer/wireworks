// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PoleTierTest {

    @AfterEach
    void restoreDefaults() {
        for (PoleTier tier : PoleTier.values()) {
            tier.configure(tier.defaultSupplySize(), tier.defaultWireReach());
        }
    }

    @Test
    void aConfiguredSupplySizeMovesTheAreasEdges() {
        PoleTier.MEDIUM.configure(10, 12.0);
        assertEquals(10, PoleTier.MEDIUM.supplySize());
        assertEquals(-5, PoleTier.MEDIUM.minOffset());
        assertEquals(4, PoleTier.MEDIUM.maxOffset());
        assertEquals(10 * 10 * 5, SupplyArea.volume(PoleTier.MEDIUM));
    }

    @Test
    void theLongestWireIsTheLongestConfiguredReach() {
        PoleTier.SMALL.configure(5, 40.0);
        assertEquals(40.0, PoleTier.maxWireReach());
    }

    @Test
    void aTierThatCoversNothingIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> PoleTier.SMALL.configure(0, 7.5));
        assertThrows(IllegalArgumentException.class, () -> PoleTier.SMALL.configure(5, 0.0));
    }
}
