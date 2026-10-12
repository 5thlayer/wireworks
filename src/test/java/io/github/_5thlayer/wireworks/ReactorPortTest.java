// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The middle of each side of the Reactor's bottom layer is water, its top centre steam, and nothing else a port. */
class ReactorPortTest {

    @Test
    void theMiddleOfEachBottomSideIsWater() {
        assertEquals(ReactorPort.WATER, ReactorPort.at(-1, 0, 0));
        assertEquals(ReactorPort.WATER, ReactorPort.at(1, 0, 0));
        assertEquals(ReactorPort.WATER, ReactorPort.at(0, 0, -1));
        assertEquals(ReactorPort.WATER, ReactorPort.at(0, 0, 1));
    }

    @Test
    void theTopCentreIsTheOnlySteamPort() {
        int steam = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (ReactorPort.at(x, y, z) == ReactorPort.STEAM) {
                        steam++;
                    }
                }
            }
        }
        assertEquals(1, steam);
        assertEquals(ReactorPort.STEAM, ReactorPort.at(0, 2, 0));
    }

    @Test
    void theCornersTheCentreAndTheUpperLayersAreNoPort() {
        assertNull(ReactorPort.at(0, 0, 0));
        assertNull(ReactorPort.at(1, 0, 1));
        assertNull(ReactorPort.at(-1, 0, -1));
        assertNull(ReactorPort.at(1, 1, 0));
        assertNull(ReactorPort.at(1, 2, 0));
    }
}
