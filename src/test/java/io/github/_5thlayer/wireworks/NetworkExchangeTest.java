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

    @Test
    void twoTransformersOnADistrictEachCarryHalfItsExchange() {
        NetworkExchange district = NetworkExchange.ofDistrict(100, 20, 5, 0);
        NetworkExchange each = district.sharedAmong(2);
        assertEquals(40, each.exported());
        assertEquals(0, each.imported());
        assertEquals(5, each.surplus());
    }

    @Test
    void aLoneTransformerCarriesTheWholeExchange() {
        assertEquals(new NetworkExchange(0, 50, 0, 3),
                NetworkExchange.ofDistrict(10, 60, 0, 3).sharedAmong(1));
    }

    @Test
    void aTransformerJoiningTwoDistrictsCarriesTheNetOfItsShares() {
        NetworkExchange exporting = new NetworkExchange(30, 0, 0, 0);
        NetworkExchange importing = new NetworkExchange(0, 10, 0, 0);
        NetworkExchange net = exporting.plus(importing);
        assertEquals(20, net.exported());
        assertEquals(0, net.imported());
        assertEquals(10, importing.plus(new NetworkExchange(0, 0, 0, 0)).imported());
    }
}
