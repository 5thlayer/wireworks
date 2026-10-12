// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Reactor's four item slots: the Fission Fuel it burns; the depleted slot, where what a burnt fuel
 * leaves goes; a water container to empty into the water tank; and the bucket output, where each emptied
 * container goes, so a waiting bucket never stops the Reactor.
 */
public final class ReactorSlots {

    public static final int FUEL = 0;
    public static final int DEPLETED = 1;
    public static final int WATER = 2;
    public static final int BUCKETS = 3;

    public static final int SIZE = 4;

    private ReactorSlots() {
    }

    /** Whether {@code slot} is one things go into, rather than one the Reactor fills. */
    public static boolean isInput(int slot) {
        return slot == FUEL || slot == WATER;
    }
}
