// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which wires a placed pole adds (ADR 0004): every pole in reach sharing no neighbour with it, at
 * most five, nearest first.
 */
class PoleWiringPlacementTest {

    private static PoleNetworks.Pole small(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, PoleKind.distribution(PoleTier.SMALL));
    }

    @Test
    void aPlacedPoleWiresToAPoleInReachAndNotToOneBeyond() {
        PoleNetworks.Pole near = small(7, 0, 0);
        List<PoleNetworks.Pole> wired = PoleWiring.onPlace(
                small(0, 0, 0), List.of(near, small(-8, 0, 0)), new WireSet());
        assertEquals(List.of(near), wired);
    }

    private static PoleNetworks.Pos pos(PoleNetworks.Pole p) {
        return new PoleNetworks.Pos(p.x(), p.y(), p.z());
    }

    @Test
    void aPoleAlreadyWiredToOneJustChosenIsSkippedSoNoTriangleForms() {
        PoleNetworks.Pole a = small(0, 0, 0);
        PoleNetworks.Pole b = small(7, 0, 0);
        WireSet wires = new WireSet();
        wires.add(pos(a), pos(b));
        // Placed between them, nearer a: it wires to a, and b is a's neighbour.
        List<PoleNetworks.Pole> wired = PoleWiring.onPlace(small(3, 0, 0), List.of(b, a), wires);
        assertEquals(List.of(a), wired);
    }

    @Test
    void aPlacedPoleWiresToAtMostFiveTakingTheNearest() {
        PoleNetworks.Pole p6 = small(0, 0, 6);
        PoleNetworks.Pole p1 = small(1, 0, 0);
        PoleNetworks.Pole p2 = small(-2, 0, 0);
        PoleNetworks.Pole p3 = small(0, 0, 3);
        PoleNetworks.Pole p4 = small(0, 0, -4);
        PoleNetworks.Pole p5 = small(5, 0, 0);
        List<PoleNetworks.Pole> wired = PoleWiring.onPlace(
                small(0, 0, 0), List.of(p6, p5, p4, p3, p2, p1), new WireSet());
        assertEquals(List.of(p1, p2, p3, p4, p5), wired);
    }

    @Test
    void polesAtTheSameDistanceAreTakenInPositionOrderWhateverOrderTheyStand() {
        PoleNetworks.Pole east = small(3, 0, 0);
        PoleNetworks.Pole west = small(-3, 0, 0);
        PoleNetworks.Pole south = small(0, 0, 3);
        List<PoleNetworks.Pole> wired = PoleWiring.onPlace(
                small(0, 0, 0), List.of(east, south, west), new WireSet());
        assertEquals(List.of(west, south, east), wired);
    }

    private static PoleNetworks.Pole transmission(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, PoleKind.TRANSMISSION);
    }

    @Test
    void aPlacedTransmissionPoleWiresOnlyToTransmissionPolesInReach() {
        PoleNetworks.Pole far = transmission(30, 0, 0);
        List<PoleNetworks.Pole> wired = PoleWiring.onPlace(transmission(0, 0, 0),
                List.of(small(2, 0, 0), far, transmission(40, 0, 0)), new WireSet());
        assertEquals(List.of(far), wired);
    }

    @Test
    void aPlacedDistributionPoleNeverWiresToATransmissionPole() {
        PoleNetworks.Pole near = small(7, 0, 0);
        List<PoleNetworks.Pole> wired = PoleWiring.onPlace(small(0, 0, 0),
                List.of(transmission(1, 0, 0), near), new WireSet());
        assertEquals(List.of(near), wired);
    }
}
