// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Transformer's reach depends on the system at the far end (ADR 0008): the line side toward a
 * Transmission Pole, the District side toward a Distribution Pole. Both are values it is given.
 */
class TransformerReachTest {

    @Test
    void reachesTheLineSideTowardATransmissionPoleAndTheDistrictSideTowardADistributionPole() {
        PoleKind transformer = PoleKind.transformer(30, 11);
        assertEquals(30, transformer.reachToward(PoleKind.TRANSMISSION));
        assertEquals(11, transformer.reachToward(PoleKind.distribution(PoleTier.LARGE)));
    }

    @Test
    void theDefaultsAreTheTransmissionPolesDefaultReachAndTheMediumPolesReach() {
        assertEquals(PoleKind.DEFAULT_TRANSMISSION_REACH, PoleKind.TRANSFORMER.reachToward(PoleKind.TRANSMISSION));
        assertEquals(32, PoleKind.DEFAULT_TRANSMISSION_REACH);
        assertEquals(PoleTier.MEDIUM.wireReach(),
                PoleKind.TRANSFORMER.reachToward(PoleKind.distribution(PoleTier.SMALL)));
    }

    @Test
    void theLongestReachIsTheLongerSideSoASearchSizedByItFindsEveryPoleItCouldWire() {
        assertEquals(30, PoleKind.transformer(30, 11).longestReach());
        assertEquals(40, PoleKind.transformer(8, 40).longestReach());
        assertEquals(PoleTier.LARGE.wireReach(), PoleKind.distribution(PoleTier.LARGE).longestReach());
    }

    @Test
    void aWireToATransmissionPoleReachesTheTransformersLineSide() {
        assertEquals(30, PoleKind.wireReach(PoleKind.transformer(30, 11), PoleKind.TRANSMISSION));
        assertEquals(30, PoleKind.wireReach(PoleKind.TRANSMISSION, PoleKind.transformer(30, 11)));
    }

    @Test
    void aWireToADistributionPoleReachesTheShorterOfTheDistrictSideAndThePolesTier() {
        PoleKind transformer = PoleKind.transformer(30, 11);
        assertEquals(11, PoleKind.wireReach(transformer, PoleKind.distribution(PoleTier.LARGE)));
        assertEquals(PoleTier.SMALL.wireReach(),
                PoleKind.wireReach(transformer, PoleKind.distribution(PoleTier.SMALL)));
    }
}
