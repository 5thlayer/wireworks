// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * What a pole is (ADR 0008): a Distribution Pole of a tier (the creative pole counts as the large
 * tier's footprint), the Transmission Pole, or the Transformer.
 *
 * <p>Wire Reach is asked of one end toward the other, since a Transformer's reach depends on the
 * system at the far end ({@link #reachToward}); a wire reaches the shorter of its two ends' answers
 * ({@link #wireReach}). The Transformer exists here as a constant only: its reaches arrive with its block.
 *
 * <p>Pure: no Minecraft types.
 */
public sealed interface PoleKind {

    /** The Transmission Pole: no tier, no supply area. */
    PoleKind TRANSMISSION = new Transmission();

    /** The Transmission Pole's default Wire Reach, which a Transformer's line side defaults to. */
    double DEFAULT_TRANSMISSION_REACH = 32.0;

    /**
     * The Transformer with its default reaches: {@link #DEFAULT_TRANSMISSION_REACH} on the line side
     * and the Medium Pole's current reach on the District side.
     */
    PoleKind TRANSFORMER = new Transformer(null, null);

    static PoleKind distribution(PoleTier tier) {
        return new Distribution(tier);
    }

    /** A Transformer with the reaches it is given: the line side and the District side. */
    static PoleKind transformer(double lineReach, double districtReach) {
        return new Transformer(lineReach, districtReach);
    }

    /** How far a wire may run from a pole of this kind toward a pole of kind {@code other}. */
    double reachToward(PoleKind other);

    /** The farthest this pole's wires can run, toward any kind: what a search for poles it could wire is sized by. */
    default double longestReach() {
        return reachToward(this);
    }

    /**
     * A wire's reach: the shorter of the two ends' answers, except that a Transformer alone answers for
     * a wire to a Transmission Pole, whose own reach is the line side's default.
     */
    static double wireReach(PoleKind a, PoleKind b) {
        if (a instanceof Transformer && b == TRANSMISSION) {
            return a.reachToward(b);
        }
        if (b instanceof Transformer && a == TRANSMISSION) {
            return b.reachToward(a);
        }
        return Math.min(a.reachToward(b), b.reachToward(a));
    }

    /** A Distribution Pole of one tier: its tier's reach, whatever is at the other end. */
    record Distribution(PoleTier tier) implements PoleKind {
        @Override
        public double reachToward(PoleKind other) {
            return tier.wireReach();
        }
    }

    /** The Transmission Pole: its configured reach, whatever is at the other end. */
    record Transmission() implements PoleKind {
        @Override
        public double reachToward(PoleKind other) {
            return TransmissionSpec.wireReach();
        }
    }

    /**
     * The Transformer: the line side's reach toward a Transmission Pole, the District side's toward a
     * Distribution Pole. A null reach is the default, read when asked so the Medium Pole's configured
     * reach is never stale.
     */
    record Transformer(Double lineReach, Double districtReach) implements PoleKind {
        @Override
        public double reachToward(PoleKind other) {
            return switch (other) {
                case Transmission t -> line();
                case Distribution d -> district();
                case Transformer t -> longestReach();
            };
        }

        @Override
        public double longestReach() {
            return Math.max(line(), district());
        }

        private double line() {
            return lineReach != null ? lineReach : DEFAULT_TRANSMISSION_REACH;
        }

        private double district() {
            return districtReach != null ? districtReach : PoleTier.MEDIUM.wireReach();
        }
    }
}
