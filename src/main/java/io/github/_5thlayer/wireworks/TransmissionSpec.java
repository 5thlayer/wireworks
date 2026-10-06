// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Transmission Pole's Wire Reach (ADR 0008): 32 blocks unless {@code wireworks-server.toml} says
 * otherwise. It has no tier to hang the number on, so it is kept here, as {@link SolarPanelSpec}
 * keeps the Solar Panel's.
 *
 * <p>Set by {@link WireworksConfig} when the server config loads or reloads. Pure: no Minecraft types.
 */
public final class TransmissionSpec {

    static final double DEFAULT_WIRE_REACH = 32.0;

    private static volatile double wireReach = DEFAULT_WIRE_REACH;

    private TransmissionSpec() {
    }

    static void configure(double wireReach) {
        if (wireReach <= 0) {
            throw new IllegalArgumentException("transmission wire reach " + wireReach);
        }
        TransmissionSpec.wireReach = wireReach;
    }

    public static double wireReach() {
        return wireReach;
    }
}
