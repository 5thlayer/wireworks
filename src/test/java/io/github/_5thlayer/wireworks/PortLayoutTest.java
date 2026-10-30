// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github._5thlayer.wireworks.PortLayout.Port;
import io.github._5thlayer.wireworks.PortLayout.Tank;
import org.junit.jupiter.api.Test;

/** Where the Boiler's and the Reactor's Ports stand, which tank each reaches and which way each opens. */
class PortLayoutTest {

    @Test
    void theBoilersWaterPortsAreTheEndsOfItsFrontRowOpeningOutward() {
        assertEquals(new Port(0, 0, -1, 0, -1, Tank.WATER), PortLayout.BOILER.at(0, 0, -1));
        assertEquals(new Port(0, 0, 1, 0, 1, Tank.WATER), PortLayout.BOILER.at(0, 0, 1));
    }

    @Test
    void theBoilersBackMiddleIsItsOnlySteamPortOpeningBackward() {
        assertEquals(new Port(1, 0, 0, 1, 0, Tank.STEAM), PortLayout.BOILER.at(1, 0, 0));
        assertEquals(1, PortLayout.BOILER.ports().stream().filter(port -> port.tank() == Tank.STEAM).count());
    }

    @Test
    void theBoilersAnchorAndBackCornersAreNoPort() {
        assertNull(PortLayout.BOILER.at(0, 0, 0));
        assertNull(PortLayout.BOILER.at(1, 0, -1));
        assertNull(PortLayout.BOILER.at(1, 0, 1));
    }

    @Test
    void theMiddleOfEachSideOfTheReactorsBottomLayerIsWaterOpeningOutward() {
        assertEquals(new Port(-1, 0, 0, -1, 0, Tank.WATER), PortLayout.REACTOR.at(-1, 0, 0));
        assertEquals(new Port(1, 0, 0, 1, 0, Tank.WATER), PortLayout.REACTOR.at(1, 0, 0));
        assertEquals(new Port(0, 0, -1, 0, -1, Tank.WATER), PortLayout.REACTOR.at(0, 0, -1));
        assertEquals(new Port(0, 0, 1, 0, 1, Tank.WATER), PortLayout.REACTOR.at(0, 0, 1));
    }

    @Test
    void theBackMiddleOfTheReactorsMiddleLayerIsItsOnlySteamPortOpeningBackward() {
        assertEquals(new Port(1, 1, 0, 1, 0, Tank.STEAM), PortLayout.REACTOR.at(1, 1, 0));
        assertEquals(1, PortLayout.REACTOR.ports().stream().filter(port -> port.tank() == Tank.STEAM).count());
    }

    @Test
    void theReactorsCornersCentreAndTopAreNoPort() {
        assertNull(PortLayout.REACTOR.at(0, 0, 0));
        assertNull(PortLayout.REACTOR.at(0, 2, 0));
        assertNull(PortLayout.REACTOR.at(-1, 1, 0));
        assertNull(PortLayout.REACTOR.at(1, 0, 1));
        assertNull(PortLayout.REACTOR.at(-1, 0, -1));
        assertNull(PortLayout.REACTOR.at(1, 2, 0));
    }
}
