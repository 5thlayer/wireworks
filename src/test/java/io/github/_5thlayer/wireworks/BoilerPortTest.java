// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The Boiler's three front blocks are water, its back middle steam, and its back corners no port. */
class BoilerPortTest {

    @Test
    void theFrontRowIsWater() {
        for (int z = -1; z <= 1; z++) {
            assertEquals(BoilerPort.WATER, BoilerPort.at(0, 0, z));
        }
    }

    @Test
    void theBackMiddleIsTheOnlySteamPort() {
        assertEquals(BoilerPort.STEAM, BoilerPort.at(1, 0, 0));
    }

    @Test
    void theBackCornersAreNoPort() {
        assertNull(BoilerPort.at(1, 0, -1));
        assertNull(BoilerPort.at(1, 0, 1));
    }

    @Test
    void nothingAboveTheFloorIsAPort() {
        assertNull(BoilerPort.at(0, 1, 0));
    }
}
