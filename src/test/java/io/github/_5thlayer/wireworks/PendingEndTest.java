// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a wire tool lets go of a wire's first end (ADR 0004, ADR 0006): the anchor pole breaks, the tool leaves
 * the main hand, the player changes dimension, or the player walks beyond the
 * anchor's wire reach plus their own interaction range.
 */
class PendingEndTest {

    /** A survival player's block interaction range. */
    private static final double RANGE = 4.5;

    private static final PoleNetworks.Pole ANCHOR = new PoleNetworks.Pole(0, 64, 0, PoleKind.distribution(PoleTier.SMALL));

    /** A player at {@code (x, y, z)} holding the tool in the main hand, in the anchor's dimension. */
    private static PendingEnd.Holder at(double x, double y, double z) {
        return new PendingEnd.Holder(x, y, z, RANGE, true, true);
    }

    @Test
    void anEndStaysHeldBesideAStandingAnchor() {
        assertTrue(PendingEnd.stillHeld(ANCHOR, true, at(3.5, 64.0, 0.5)));
    }

    @Test
    void anEndIsDroppedWhenTheAnchorBreaks() {
        assertFalse(PendingEnd.stillHeld(ANCHOR, false, at(3.5, 64.0, 0.5)));
    }

    @Test
    void anEndIsDroppedWhenTheToolLeavesTheMainHand() {
        assertFalse(PendingEnd.stillHeld(ANCHOR, true,
                new PendingEnd.Holder(3.5, 64.0, 0.5, RANGE, false, true)));
    }

    @Test
    void anEndIsDroppedWhenThePlayerChangesDimension() {
        assertFalse(PendingEnd.stillHeld(ANCHOR, true,
                new PendingEnd.Holder(3.5, 64.0, 0.5, RANGE, true, false)));
    }

    @Test
    void anEndIsHeldAsFarAsAPoleInReachOfTheAnchorCanStillBeClicked() {
        // A small pole reaches 7.5, and the player clicks 4.5 further: 12 blocks from the anchor's
        // block centre (0.5, 64.5, 0.5), so an out-of-reach pole is refused, never silently re-anchored.
        assertTrue(PendingEnd.stillHeld(ANCHOR, true, at(12.0, 64.5, 0.5)));
        assertFalse(PendingEnd.stillHeld(ANCHOR, true, at(13.5, 64.5, 0.5)));
    }
}
