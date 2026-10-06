// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a District gave to and took from its Electric Network in a tick, and what the network had left. */
class NetworkExchangeTest {

    @Test
    void aDistrictThatSuppliedMoreThanItUsedExportedTheDifference() {
        NetworkExchange exchange = NetworkExchange.ofDistrict(100, 60, 0, 0);
        assertEquals(40, exchange.exported());
        assertEquals(0, exchange.imported());
    }

    @Test
    void aDistrictThatUsedMoreThanItSuppliedImportedTheDifference() {
        NetworkExchange exchange = NetworkExchange.ofDistrict(10, 60, 0, 0);
        assertEquals(0, exchange.exported());
        assertEquals(50, exchange.imported());
    }

    @Test
    void aBalancedDistrictCrossesNothing() {
        NetworkExchange exchange = NetworkExchange.ofDistrict(60, 60, 0, 0);
        assertEquals(0, exchange.exported());
        assertEquals(0, exchange.imported());
    }

    @Test
    void theNetworksSurplusAndShortfallRideAlong() {
        NetworkExchange exchange = NetworkExchange.ofDistrict(0, 0, 7, 3);
        assertEquals(7, exchange.surplus());
        assertEquals(3, exchange.shortfall());
    }

    @Test
    void theNetworkSideCountsWhatCrossedItsTransformersOnce() {
        NetworkExchange exchange = NetworkExchange.ofNetwork(new long[] {100, 10, 60}, new long[] {60, 60, 50}, 5, 0);
        assertEquals(50, exchange.exported());
        assertEquals(50, exchange.imported());
        assertEquals(5, exchange.surplus());
    }
}
