// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Transformer's wiring rules (ADR 0008): what a click may wire, and what a placement wires. */
class TransformerWiringTest {

    private static final PoleKind TRANSFORMER = PoleKind.transformer(30, 9);

    private static PoleNetworks.Pole at(int x, PoleKind kind) {
        return new PoleNetworks.Pole(x, 0, 0, kind);
    }

    private static PoleNetworks.Pole dist(int x) {
        return at(x, PoleKind.distribution(PoleTier.LARGE));
    }

    private static PoleNetworks.Pole line(int x) {
        return at(x, PoleKind.TRANSMISSION);
    }

    private static PoleNetworks.Pole transformer(int x) {
        return at(x, TRANSFORMER);
    }

    @Test
    void aClickWiresATransformerToATransmissionPoleWithinTheLineSideReach() {
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.WIRED, PoleWiring.click(transformer(0), line(28), wires));
        assertEquals(WireSystem.TRANSMISSION, wires.all().iterator().next().system());
    }

    @Test
    void aClickWiresATransformerToADistributionPoleWithinTheDistrictSideReach() {
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.WIRED, PoleWiring.click(dist(8), transformer(0), wires));
        assertEquals(WireSystem.DISTRIBUTION, wires.all().iterator().next().system());
    }

    @Test
    void aClickBeyondTheSideReachIsRefusedOnThatSideOnly() {
        assertEquals(PoleWiring.Click.REFUSED, PoleWiring.click(transformer(0), dist(10), new WireSet()));
        assertEquals(PoleWiring.Click.WIRED, PoleWiring.click(transformer(0), line(10), new WireSet()));
        assertEquals(PoleWiring.Click.REFUSED, PoleWiring.click(transformer(0), line(31), new WireSet()));
    }

    @Test
    void aClickBetweenTwoTransformersIsRefusedHoweverNearTheyStand() {
        WireSet wires = new WireSet();
        assertTrue(PoleWiring.refuses(transformer(0), transformer(2)));
        assertEquals(PoleWiring.Click.REFUSED, PoleWiring.click(transformer(0), transformer(2), wires));
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void aPlacedTransformerWiresItselfToTheNearestPoleOfEachSystemInReachOneEach() {
        PoleNetworks.Pole nearLine = line(10);
        PoleNetworks.Pole farLine = line(20);
        PoleNetworks.Pole nearDist = dist(3);
        PoleNetworks.Pole farDist = dist(6);
        List<PoleNetworks.Pole> wired = PoleWiring.onPlace(transformer(0),
                List.of(farLine, farDist, nearLine, nearDist), new WireSet());
        assertEquals(List.of(nearDist, nearLine), wired);
    }

    @Test
    void aPlacedTransformerWiresToNoOtherTransformer() {
        assertEquals(List.of(), PoleWiring.onPlace(transformer(0), List.of(transformer(2)), new WireSet()));
    }

    @Test
    void aPlacedTransformerSkipsAPoleOutOfItsSideReachAndTakesTheNextInReach() {
        PoleNetworks.Pole tooFar = dist(12);
        PoleNetworks.Pole inReach = dist(9);
        assertEquals(List.of(inReach), PoleWiring.onPlace(transformer(0), List.of(tooFar, inReach), new WireSet()));
    }

    @Test
    void aPlacedPoleOfEitherSystemAlsoWiresItselfToTransformersInReach() {
        PoleNetworks.Pole t = transformer(0);
        assertEquals(List.of(t), PoleWiring.onPlace(dist(8), List.of(t), new WireSet()));
        assertEquals(List.of(t), PoleWiring.onPlace(line(25), List.of(t), new WireSet()));
    }

    @Test
    void aPlacedPoleDoesNotWireAcrossSystemsExceptThroughATransformer() {
        assertEquals(List.of(), PoleWiring.onPlace(dist(0), List.of(line(5)), new WireSet()));
        assertEquals(List.of(), PoleWiring.onPlace(line(0), List.of(dist(5)), new WireSet()));
    }

    @Test
    void aPlacedPoleStillWiresToAtMostFiveTransformers() {
        List<PoleNetworks.Pole> transformers = List.of(transformer(1), transformer(2), transformer(3),
                transformer(4), transformer(5), transformer(6), transformer(7));
        assertEquals(PoleWiring.AUTO_WIRES, PoleWiring.onPlace(dist(0), transformers, new WireSet()).size());
    }

    @Test
    void theSystemStoredOnATransformersWireIsThatOfThePoleAtItsOtherEnd() {
        assertEquals(WireSystem.TRANSMISSION, WireSystem.between(TRANSFORMER, PoleKind.TRANSMISSION));
        assertEquals(WireSystem.TRANSMISSION, WireSystem.between(PoleKind.TRANSMISSION, TRANSFORMER));
        assertEquals(WireSystem.DISTRIBUTION,
                WireSystem.between(TRANSFORMER, PoleKind.distribution(PoleTier.SMALL)));
        assertEquals(WireSystem.DISTRIBUTION,
                WireSystem.between(PoleKind.distribution(PoleTier.SMALL), TRANSFORMER));
    }
}
