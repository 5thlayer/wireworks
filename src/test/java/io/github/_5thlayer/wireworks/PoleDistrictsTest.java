// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Districts and Electric Networks (ADR 0008): a District is the Distribution Poles joined by wires, a
 * Transformer belongs to none, and a network is everything joined, Transformers included.
 */
class PoleDistrictsTest {

    private static PoleNetworks.Pole dist(int x) {
        return new PoleNetworks.Pole(x, 0, 0, PoleKind.distribution(PoleTier.SMALL));
    }

    private static PoleNetworks.Pole line(int x) {
        return new PoleNetworks.Pole(x, 0, 0, PoleKind.TRANSMISSION);
    }

    private static PoleNetworks.Pole transformer(int x) {
        return new PoleNetworks.Pole(x, 0, 0, TransformerSpec.kind());
    }

    private static PoleNetworks.Wire wire(int a, int b) {
        return new PoleNetworks.Wire(new PoleNetworks.Pos(a, 0, 0), new PoleNetworks.Pos(b, 0, 0));
    }

    private static PoleNetworks.Topology topology(List<PoleNetworks.Pole> poles, PoleNetworks.Wire... wires) {
        return PoleNetworks.topology(poles, Set.of(wires));
    }

    @Test
    void aDistrictIsTheDistributionPolesJoinedByWires() {
        PoleNetworks.Topology t = topology(List.of(dist(0), dist(7), dist(14), dist(50)), wire(0, 7), wire(7, 14));
        assertEquals(t.districtOf()[0], t.districtOf()[2]);
        assertNotEquals(t.districtOf()[0], t.districtOf()[3]);
    }

    @Test
    void aTransformerAndATransmissionPoleBelongToNoDistrict() {
        PoleNetworks.Topology t = topology(List.of(dist(0), transformer(5), line(10)), wire(0, 5), wire(5, 10));
        assertEquals(PoleNetworks.NO_DISTRICT, t.districtOf()[1]);
        assertEquals(PoleNetworks.NO_DISTRICT, t.districtOf()[2]);
    }

    @Test
    void aTransformerDoesNotJoinTwoDistrictsIntoOne() {
        PoleNetworks.Topology t = topology(List.of(dist(0), transformer(5), dist(10)), wire(0, 5), wire(5, 10));
        assertNotEquals(t.districtOf()[0], t.districtOf()[2]);
    }

    @Test
    void anElectricNetworkIsEverythingJoinedTransformersIncluded() {
        PoleNetworks.Topology t = topology(List.of(dist(0), transformer(5), line(10), line(60)),
                wire(0, 5), wire(5, 10));
        assertEquals(t.networkOf()[0], t.networkOf()[1]);
        assertEquals(t.networkOf()[0], t.networkOf()[2]);
        assertNotEquals(t.networkOf()[0], t.networkOf()[3]);
        assertEquals(2, t.networks().size());
    }

    @Test
    void aNetworkListsItsDistrictsAndTheTransformersJoiningEach() {
        List<PoleNetworks.Pole> poles = List.of(line(0), transformer(5), dist(10), dist(20), transformer(1));
        PoleNetworks.Topology t = topology(poles, wire(0, 5), wire(5, 10), wire(5, 20), wire(0, 1), wire(1, 10));
        assertEquals(1, t.networks().size());
        PoleNetworks.ElectricNetwork network = t.networks().get(0);
        assertEquals(List.of(t.districtOf()[2], t.districtOf()[3]), network.districts());
        assertEquals(List.of(1, 4), network.transformersJoining(t.districtOf()[2]));
        assertEquals(List.of(1), network.transformersJoining(t.districtOf()[3]));
        assertEquals(List.of(1, 4), network.transformers());
    }

    @Test
    void aSecondTransformerBetweenTheSameDistrictAndLineAddsNoDistrict() {
        List<PoleNetworks.Pole> one = List.of(line(0), transformer(5), dist(10));
        List<PoleNetworks.Pole> two = new ArrayList<>(one);
        two.add(transformer(1));
        PoleNetworks.Topology a = topology(one, wire(0, 5), wire(5, 10));
        PoleNetworks.Topology b = topology(two, wire(0, 5), wire(5, 10), wire(0, 1), wire(1, 10));
        assertEquals(a.networks().get(0).districts(), b.networks().get(0).districts());
        assertEquals(a.networkOf()[2], b.networkOf()[2]);
        assertEquals(1, b.networks().size());
    }

    @Test
    void twoTransmissionLinesJoinedOnlyThroughOneDistrictAreOneElectricNetwork() {
        List<PoleNetworks.Pole> poles = List.of(line(0), transformer(5), dist(10), transformer(15), line(20));
        PoleNetworks.Topology t = topology(poles, wire(0, 5), wire(5, 10), wire(10, 15), wire(15, 20));
        assertEquals(1, t.networks().size());
        assertEquals(1, t.networks().get(0).districts().size());
        assertEquals(2, t.networks().get(0).transformers().size());
    }

    @Test
    void aWorldWithNoTransformerHasOneDistrictPerNetworkMatchingTodaysNetworksPoleForPole() {
        List<PoleNetworks.Pole> poles = List.of(dist(0), dist(7), dist(14), dist(40), dist(47), dist(90));
        Set<PoleNetworks.Wire> wires = Set.of(wire(0, 7), wire(7, 14), wire(40, 47));
        PoleNetworks.Topology t = PoleNetworks.topology(poles, wires);
        assertEquals(Arrays.toString(PoleNetworks.networks(poles, wires)), Arrays.toString(t.networkOf()));
        assertEquals(Arrays.toString(t.networkOf()), Arrays.toString(t.districtOf()));
        for (PoleNetworks.ElectricNetwork network : t.networks()) {
            assertEquals(1, network.districts().size());
            assertEquals(List.of(), network.transformers());
        }
    }
}
