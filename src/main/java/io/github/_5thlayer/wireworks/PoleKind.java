// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * What a pole is (ADR 0008): a Distribution Pole of a tier (the creative pole counts as the large
 * tier's footprint), the Transmission Pole, or the Transformer with the two reaches it is given.
 *
 * <p>Wire Reach is asked of one end toward the other, since a Transformer's reach depends on the
 * system at the far end ({@link #reachToward}); a wire reaches the shorter of its two ends' answers
 * ({@link #wireReach}).
 *
 * <p>Pure: no Minecraft types.
 */
public sealed interface PoleKind {

    /** The Transmission Pole: no tier, no supply area. */
    PoleKind TRANSMISSION = new Transmission();

    static PoleKind distribution(PoleTier tier) {
        return new Distribution(tier);
    }

    /**
     * A Transformer with the reaches it is given: the line side and the District side.
     * {@link TransformerSpec#kind()} gives the configured ones.
     */
    static PoleKind transformer(double lineReach, double districtReach) {
        return new Transformer(lineReach, districtReach);
    }

    /** How far a wire may run from a pole of this kind toward a pole of kind {@code other}. */
    double reachToward(PoleKind other);

    /** Whether either of two poles is a Transformer, the one block that wires across systems. */
    static boolean eitherIsTransformer(PoleKind a, PoleKind b) {
        return a instanceof Transformer || b instanceof Transformer;
    }

    /** The farthest this pole's wires can run, toward any kind: what a search for poles it could wire is sized by. */
    default double longestReach() {
        return reachToward(this);
    }

    /**
     * A wire's reach: the shorter of the two ends' answers, except that a Transformer alone answers for
     * a wire to a Transmission Pole, whose own reach is the line side's default.
     */
    static double wireReach(PoleKind a, PoleKind b) {
        if (a instanceof Transformer && b instanceof Transmission) {
            return a.reachToward(b);
        }
        if (b instanceof Transformer && a instanceof Transmission) {
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
     * Distribution Pole.
     */
    record Transformer(double lineReach, double districtReach) implements PoleKind {
        @Override
        public double reachToward(PoleKind other) {
            return switch (other) {
                case Transmission t -> lineReach;
                case Distribution d -> districtReach;
                case Transformer t -> longestReach();
            };
        }

        @Override
        public double longestReach() {
            return Math.max(lineReach, districtReach);
        }
    }
}
