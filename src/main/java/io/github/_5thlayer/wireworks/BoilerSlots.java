// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Boiler's four item slots: the fuel it burns; the output, where what a burnt fuel leaves and every
 * container it has emptied or filled goes; a water container to empty into the water tank; and an
 * empty container to fill from the steam tank.
 */
public final class BoilerSlots {

    public static final int FUEL = 0;
    public static final int OUTPUT = 1;
    public static final int WATER = 2;
    public static final int STEAM = 3;

    public static final int SIZE = 4;

    private BoilerSlots() {
    }
}
