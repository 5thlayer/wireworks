// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A wire is made in the system its ends decide (ADR 0008). */
class WireSystemTest {

    @Test
    void aWireBetweenDistributionPolesIsDistribution() {
        PoleKind small = PoleKind.distribution(PoleTier.SMALL);
        assertEquals(WireSystem.DISTRIBUTION, WireSystem.between(small, PoleKind.distribution(PoleTier.LARGE)));
        assertEquals(WireSystem.DISTRIBUTION, WireSystem.between(small, TransformerSpec.kind()));
    }

    @Test
    void aWireWithATransmissionPoleAtAnEndIsTransmission() {
        assertEquals(WireSystem.TRANSMISSION, WireSystem.between(PoleKind.TRANSMISSION, PoleKind.TRANSMISSION));
        assertEquals(WireSystem.TRANSMISSION, WireSystem.between(TransformerSpec.kind(), PoleKind.TRANSMISSION));
    }
}
