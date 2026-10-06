// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * A pole's kind (ADR 0008): a Distribution Pole of a tier, the Transmission Pole or the Transformer.
 * Reach is asked of one end toward the other, and a wire reaches the shorter of its two answers.
 */
class PoleKindTest {

    @Test
    void aDistributionTierReachesItsTiersReachWhateverIsAtTheOtherEnd() {
        for (PoleTier tier : PoleTier.values()) {
            PoleKind from = PoleKind.distribution(tier);
            for (PoleTier other : PoleTier.values()) {
                assertEquals(tier.wireReach(), from.reachToward(PoleKind.distribution(other)));
            }
            assertEquals(tier.wireReach(), from.reachToward(PoleKind.TRANSMISSION));
            assertEquals(tier.wireReach(), from.reachToward(PoleKind.TRANSFORMER));
        }
    }

    @Test
    void aWiresReachIsTheShorterOfTheTwoEndsAnswers() {
        PoleKind large = PoleKind.distribution(PoleTier.LARGE);
        PoleKind small = PoleKind.distribution(PoleTier.SMALL);
        assertEquals(PoleTier.SMALL.wireReach(), PoleKind.wireReach(large, small));
        assertEquals(PoleTier.SMALL.wireReach(), PoleKind.wireReach(small, large));
    }

    @Test
    void thereAreThreeKindsAndEachTierIsItsOwnDistributionKind() {
        assertNotEquals(PoleKind.TRANSMISSION, PoleKind.TRANSFORMER);
        assertNotEquals(PoleKind.distribution(PoleTier.SMALL), PoleKind.distribution(PoleTier.MEDIUM));
        assertNotEquals(PoleKind.TRANSMISSION, PoleKind.distribution(PoleTier.LARGE));
    }
}
