// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A wire tool's second click on a pole (ADR 0004): it wires an unwired pair, cuts a wired
 * one, cancels on the anchor itself, and refuses a pair out of reach without changing anything.
 */
class PoleWiringClickTest {

    private static PoleNetworks.Pole small(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, PoleKind.distribution(PoleTier.SMALL));
    }

    private static PoleNetworks.Pos pos(PoleNetworks.Pole p) {
        return new PoleNetworks.Pos(p.x(), p.y(), p.z());
    }

    @Test
    void clickingAnUnwiredPoleInReachWiresThePair() {
        PoleNetworks.Pole a = small(0, 0, 0);
        PoleNetworks.Pole b = small(7, 0, 0);
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.WIRED, PoleWiring.click(a, b, wires));
        assertTrue(wires.contains(pos(a), pos(b)));
    }

    @Test
    void aWireMadeBetweenDistributionPolesIsStoredAsDistribution() {
        WireSet wires = new WireSet();
        PoleWiring.click(small(0, 0, 0), small(7, 0, 0), wires);
        assertEquals(WireSystem.DISTRIBUTION, wires.all().iterator().next().system());
    }

    @Test
    void clickingAWiredPoleCutsTheWire() {
        PoleNetworks.Pole a = small(0, 0, 0);
        PoleNetworks.Pole b = small(7, 0, 0);
        WireSet wires = new WireSet();
        wires.add(pos(b), pos(a));
        assertEquals(PoleWiring.Click.CUT, PoleWiring.click(a, b, wires));
        assertFalse(wires.contains(pos(a), pos(b)));
    }

    @Test
    void clickingAPoleOutOfReachIsRefusedAndChangesNothing() {
        PoleNetworks.Pole a = small(0, 0, 0);
        PoleNetworks.Pole b = small(8, 0, 0);
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.REFUSED, PoleWiring.click(a, b, wires));
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void clickingTheAnchorItselfCancelsAndChangesNothing() {
        PoleNetworks.Pole a = small(0, 0, 0);
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.CANCELLED, PoleWiring.click(a, a, wires));
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void wiringByHandHasNoCap() {
        PoleNetworks.Pole hub = small(0, 0, 0);
        WireSet wires = new WireSet();
        for (int i = 1; i <= PoleWiring.AUTO_WIRES + 1; i++) {
            assertEquals(PoleWiring.Click.WIRED, PoleWiring.click(hub, small(i, 0, 0), wires));
        }
        assertEquals(PoleWiring.AUTO_WIRES + 1, wires.all().size());
    }

    /** The slack wire's red tint asks this before the click, so it must agree with the click. */
    @Test
    void refusesAgreesWithTheClick() {
        PoleNetworks.Pole a = small(0, 0, 0);
        for (PoleNetworks.Pole target : new PoleNetworks.Pole[] {a, small(7, 0, 0), small(8, 0, 0)}) {
            boolean refused = PoleWiring.click(a, target, new WireSet()) == PoleWiring.Click.REFUSED;
            assertEquals(refused, PoleWiring.refuses(a, target));
        }
    }

    private static PoleNetworks.Pole transmission(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, PoleKind.TRANSMISSION);
    }

    @Test
    void clickingTwoTransmissionPolesInReachWiresThemAsTransmission() {
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.WIRED, PoleWiring.click(transmission(0, 0, 0), transmission(32, 0, 0), wires));
        assertEquals(WireSystem.TRANSMISSION, wires.all().iterator().next().system());
    }

    @Test
    void clickingTwoTransmissionPolesBeyondReachIsRefusedForReach() {
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.REFUSED, PoleWiring.click(transmission(0, 0, 0), transmission(33, 0, 0), wires));
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void clickingATransmissionPoleAndADistributionPoleIsRefusedForTheSystemAndStoresNoWire() {
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.CROSS_SYSTEM, PoleWiring.click(transmission(0, 0, 0), small(3, 0, 0), wires));
        assertEquals(PoleWiring.Click.CROSS_SYSTEM, PoleWiring.click(small(3, 0, 0), transmission(0, 0, 0), wires));
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void aCrossSystemPairIsRefusedEvenWhenAWireAlreadyJoinsItSoNoStrayWireCanBeCut() {
        PoleNetworks.Pole t = transmission(0, 0, 0);
        PoleNetworks.Pole d = small(3, 0, 0);
        WireSet wires = new WireSet();
        wires.add(pos(t), pos(d));
        assertEquals(PoleWiring.Click.CROSS_SYSTEM, PoleWiring.click(t, d, wires));
        assertTrue(wires.contains(pos(t), pos(d)));
    }

    @Test
    void refusesCoversTheCrossSystemPair() {
        assertTrue(PoleWiring.refuses(transmission(0, 0, 0), small(3, 0, 0)));
        assertTrue(PoleWiring.refuses(small(3, 0, 0), transmission(0, 0, 0)));
    }
}
