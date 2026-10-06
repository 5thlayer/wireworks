// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * What a pole is (ADR 0008): a Distribution Pole of a tier (the creative pole counts as the large
 * tier's footprint), the Transmission Pole, or the Transformer.
 *
 * <p>Wire Reach is asked of one end toward the other, since a Transformer's reach depends on the
 * system at the far end ({@link #reachToward}); a wire reaches the shorter of its two ends' answers
 * ({@link #wireReach}). The Transmission Pole and the Transformer exist here as constants only: their
 * reaches arrive with their blocks.
 *
 * <p>Pure: no Minecraft types.
 */
public sealed interface PoleKind {

    /** The Transmission Pole: no tier, no supply area. */
    PoleKind TRANSMISSION = new Transmission();

    /** The Transformer: the one joint between the two systems. */
    PoleKind TRANSFORMER = new Transformer();

    static PoleKind distribution(PoleTier tier) {
        return new Distribution(tier);
    }

    /** How far a wire may run from a pole of this kind toward a pole of kind {@code other}. */
    double reachToward(PoleKind other);

    /** A wire's reach: the shorter of the two ends' answers. */
    static double wireReach(PoleKind a, PoleKind b) {
        return Math.min(a.reachToward(b), b.reachToward(a));
    }

    /** A Distribution Pole of one tier: its tier's reach, whatever is at the other end. */
    record Distribution(PoleTier tier) implements PoleKind {
        @Override
        public double reachToward(PoleKind other) {
            return tier.wireReach();
        }
    }

    record Transmission() implements PoleKind {
        @Override
        public double reachToward(PoleKind other) {
            throw new UnsupportedOperationException("the Transmission Pole's reach arrives with its block");
        }
    }

    record Transformer() implements PoleKind {
        @Override
        public double reachToward(PoleKind other) {
            throw new UnsupportedOperationException("the Transformer's reaches arrive with its block");
        }
    }
}
