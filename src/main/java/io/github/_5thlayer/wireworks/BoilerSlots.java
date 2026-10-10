// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * Which slot an item goes to on a Boiler, and which it may be taken from. One slot: fuel in. Water
 * and steam are fluids and reach the block through its fluid faces, and no item comes out.
 *
 * <p>Nothing may be extracted. The one thing in the Boiler is the fuel it is burning, and a funnel
 * that took it back would pull the coal out from under the machine mid-tick.
 *
 * <p>Pure: no Minecraft types.
 */
public final class BoilerSlots {

    public static final int FUEL = 0;

    public static final int SIZE = 1;

    /** No slot will take this stack. */
    public static final int NONE = -1;

    private BoilerSlots() {
    }

    /** Where an inserted stack lands: the fuel slot if it is fuel, and nowhere else. */
    public static int insertionSlot(boolean isFuel) {
        return isFuel ? FUEL : NONE;
    }

    public static boolean canExtract(int slot) {
        return false;
    }
}
