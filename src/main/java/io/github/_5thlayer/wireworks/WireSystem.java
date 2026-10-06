// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import com.mojang.serialization.Codec;

import java.util.Locale;

/**
 * Which of the two wire systems a stored wire belongs to (ADR 0008). Decided once, when the wire is
 * made, from the poles at its ends, and stored: a client receives wires a chunk at a time and cannot
 * always see the far end's block.
 */
public enum WireSystem {
    DISTRIBUTION,
    TRANSMISSION;

    public static final Codec<WireSystem> CODEC = Codec.STRING.xmap(
            name -> valueOf(name.toUpperCase(Locale.ROOT)),
            system -> system.name().toLowerCase(Locale.ROOT));

    /** The system of a wire made between two poles: Transmission only where a Transmission Pole is an end. */
    public static WireSystem between(PoleKind a, PoleKind b) {
        return a == PoleKind.TRANSMISSION || b == PoleKind.TRANSMISSION ? TRANSMISSION : DISTRIBUTION;
    }
}
