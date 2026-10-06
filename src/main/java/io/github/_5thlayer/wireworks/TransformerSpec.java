// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Transformer's two Wire Reaches (ADR 0008): the line side toward a Transmission Pole, which
 * defaults to the Transmission Pole's default, and the District side toward a Distribution Pole, which
 * defaults to the Medium Pole's. {@code wireworks-server.toml} sets both.
 *
 * <p>Set by {@link WireworksConfig} when the server config loads or reloads. Pure: no Minecraft types.
 */
public final class TransformerSpec {

    static final double DEFAULT_LINE_REACH = TransmissionSpec.DEFAULT_WIRE_REACH;
    static final double DEFAULT_DISTRICT_REACH = PoleTier.MEDIUM.defaultWireReach();

    private static volatile double lineReach = DEFAULT_LINE_REACH;
    private static volatile double districtReach = DEFAULT_DISTRICT_REACH;

    private TransformerSpec() {
    }

    static void configure(double lineReach, double districtReach) {
        if (lineReach <= 0 || districtReach <= 0) {
            throw new IllegalArgumentException("transformer reaches " + lineReach + ", " + districtReach);
        }
        TransformerSpec.lineReach = lineReach;
        TransformerSpec.districtReach = districtReach;
    }

    public static double lineReach() {
        return lineReach;
    }

    public static double districtReach() {
        return districtReach;
    }

    /** The Transformer as the topology sees it, with the reaches the config gives it. */
    public static PoleKind kind() {
        return PoleKind.transformer(lineReach, districtReach);
    }
}
