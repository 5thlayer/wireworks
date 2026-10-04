// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/** The exchange rate between the joules and watts the config states and the FE the network moves. */
public final class ForgeEnergy {

    public static final long JOULES_PER_FE = 100L;

    static final long TICKS_PER_SECOND = 20L;

    private ForgeEnergy() {
    }

    /** Whole FE a tick for a power in watts. */
    static long fePerTick(long watts) {
        return Math.round((double) watts / TICKS_PER_SECOND / JOULES_PER_FE);
    }
}
