// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * A wire only exists within reach: one whose poles no longer reach each other, after a replace by a
 * shorter-reaching tier or a config that shortened a reach, is cut.
 */
class PoleWiringBeyondReachTest {

    private static final PoleLinks.Pos A = new PoleLinks.Pos(0, 0, 0);
    private static final PoleLinks.Pos B = new PoleLinks.Pos(8, 0, 0);

    private static PoleLinks.Pole pole(PoleLinks.Pos at, PoleTier tier) {
        return new PoleLinks.Pole(at.x(), at.y(), at.z(), tier);
    }

    private static List<PoleLinks.Wire> beyond(PoleTier a, PoleTier b) {
        return PoleWiring.beyondReach(List.of(new PoleLinks.Wire(A, B)), Map.of(A, pole(A, a), B, pole(B, b)));
    }

    @Test
    void aWireTheShorterReachSpansStays() {
        assertEquals(List.of(), beyond(PoleTier.MEDIUM, PoleTier.MEDIUM));
    }

    @Test
    void aWireBeyondTheShorterReachIsCut() {
        // Eight blocks: in a medium pole's 9, past a small pole's 7.5.
        assertEquals(List.of(new PoleLinks.Wire(A, B)), beyond(PoleTier.SMALL, PoleTier.MEDIUM));
    }

    @Test
    void aWireToAPoleNotStandingIsLeftAlone() {
        // An unloaded end's tier is not known, so its wire is not judged.
        assertEquals(List.of(), PoleWiring.beyondReach(List.of(new PoleLinks.Wire(A, B)),
                Map.of(A, pole(A, PoleTier.SMALL))));
    }
}
