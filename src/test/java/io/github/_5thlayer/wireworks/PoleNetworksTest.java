// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A wire, not reach, joins two poles (ADR 0004): networks are built from the stored wires alone, so a
 * split is the same question as a merge asked of fewer wires.
 */
class PoleNetworksTest {

    private static PoleNetworks.Pole small(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, PoleTier.SMALL);
    }

    @Test
    void polesInReachButUnwiredAreTwoNetworks() {
        int[] network = PoleNetworks.networks(List.of(small(0, 0, 0), small(7, 0, 0)), Set.of());
        assertEquals(2, Arrays.stream(network).distinct().count());
    }

    private static PoleNetworks.Wire wire(int ax, int bx) {
        return new PoleNetworks.Wire(new PoleNetworks.Pos(ax, 0, 0), new PoleNetworks.Pos(bx, 0, 0));
    }

    @Test
    void aWiredChainIsOneNetworkEvenWhereItsEndsAreOutOfReach() {
        int[] network = PoleNetworks.networks(List.of(small(0, 0, 0), small(7, 0, 0), small(14, 0, 0)),
                Set.of(wire(0, 7), wire(7, 14)));
        assertEquals(network[0], network[2]);
    }

    @Test
    void cuttingTheOnlyWireBetweenTwoHalvesSplitsTheNetwork() {
        int[] network = PoleNetworks.networks(List.of(small(0, 0, 0), small(7, 0, 0), small(14, 0, 0)),
                Set.of(wire(7, 14)));
        assertEquals(network[1], network[2]);
        assertEquals(2, Arrays.stream(network).distinct().count());
    }

    @Test
    void aWireIsTheSamePairWhicheverEndIsNamedFirst() {
        int[] network = PoleNetworks.networks(List.of(small(0, 0, 0), small(7, 0, 0)), Set.of(wire(7, 0)));
        assertEquals(network[0], network[1]);
    }

    @Test
    void aWireToAPoleNoLongerStandingJoinsNothing() {
        int[] network = PoleNetworks.networks(List.of(small(0, 0, 0)), Set.of(wire(0, 7)));
        assertEquals(1, network.length);
        assertEquals(0, network[0]);
    }

    @Test
    void networkIdsAreDenseFromZero() {
        int[] network = PoleNetworks.networks(List.of(small(0, 0, 0), small(100, 0, 0), small(7, 0, 0)),
                Set.of(wire(0, 7)));
        assertEquals(0, network[0]);
        assertEquals(1, network[1]);
        assertEquals(0, network[2]);
    }
}
