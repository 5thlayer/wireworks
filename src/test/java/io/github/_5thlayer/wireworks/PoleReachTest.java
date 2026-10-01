// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether two poles are within Wire Reach (ADR 0004): reach decides which wires may exist, measured
 * between block centres in three dimensions, and the shorter of the two ends' reaches decides.
 */
class PoleReachTest {

    private static PoleNetworks.Pole small(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, PoleTier.SMALL);
    }

    @Test
    void polesSevenApartAreWithinASmallPolesReach() {
        assertTrue(PoleNetworks.withinReach(small(0, 0, 0), small(7, 0, 0)));
    }

    @Test
    void polesEightApartAreBeyondASmallPolesReach() {
        assertFalse(PoleNetworks.withinReach(small(0, 0, 0), small(8, 0, 0)));
    }

    @Test
    void theShorterReachOfTheTwoDecides() {
        // A large pole reaches 18, a small pole 7.5: at 12 apart neither end may be the long one.
        assertFalse(PoleNetworks.withinReach(new PoleNetworks.Pole(0, 0, 0, PoleTier.LARGE), small(12, 0, 0)));
    }

    @Test
    void reachIsMeasuredInThreeDimensions() {
        // 6 across and 5 up is 7.8 blocks: out of a small pole's 7.5.
        assertFalse(PoleNetworks.withinReach(small(0, 0, 0), small(6, 5, 0)));
    }
}
