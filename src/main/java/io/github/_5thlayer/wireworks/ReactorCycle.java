// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.function.BooleanSupplier;

/**
 * One tick of the Reactor, and the order its questions are asked in.
 *
 * <p>The Reactor has no heat (ADR 0012): it runs at its one rate or not at all. Water, room for the
 * steam and room for a depleted cell are asked <em>before</em> a cell is loaded, so a stopped Reactor
 * neither loads a cell nor burns the one it has. A cell's ticks count only ticks of running.
 *
 * <p>Pure: no Minecraft types.
 */
public final class ReactorCycle {

    private ReactorCycle() {
    }

    /**
     * What one tick did.
     *
     * @param converted how many millibuckets of water became superheated steam: {@code perTick}, or zero
     * @param ticksLeft the current cell's ticks of running left after this tick, 0 when none is loaded
     * @param burntOut whether this tick burnt the last of a cell, which leaves a depleted one
     */
    public record Tick(int converted, int ticksLeft, boolean burntOut) {
    }

    /**
     * @param waterAvailable the water tank's contents, in millibuckets
     * @param steamRoom the room left in the steam tank, in millibuckets
     * @param depletedRoom whether the depleted slot has room for one more cell
     * @param ticksLeft the loaded cell's ticks of running left, 0 when none is loaded
     * @param cellTicks how long a fresh cell lasts
     * @param load takes one fuel item out of the slot and reports whether there was one. Called only on
     *     a tick that will run and has no cell loaded.
     */
    public static Tick tick(int waterAvailable, int steamRoom, boolean depletedRoom, int perTick, int ticksLeft,
                            int cellTicks, BooleanSupplier load) {
        Tick stopped = new Tick(0, ticksLeft, false);
        if (perTick <= 0 || waterAvailable < perTick || steamRoom < perTick || !depletedRoom) {
            return stopped;
        }
        int cell = ticksLeft;
        if (cell <= 0) {
            if (!load.getAsBoolean()) {
                return stopped;
            }
            cell = cellTicks;
        }
        int left = cell - 1;
        return new Tick(perTick, left, left == 0);
    }
}
