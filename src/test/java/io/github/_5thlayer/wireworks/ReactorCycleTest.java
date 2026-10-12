// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Reactor's tick: it runs while it has water, room for its steam and room for a depleted cell,
 * and burns nothing otherwise. A cell is loaded only on a tick that will run, and lasts its ticks of
 * running however long the Reactor waits between them.
 */
class ReactorCycleTest {

    private static final int PER_TICK = 21;
    private static final int CELL = 4_000;

    /** A fuel slot holding {@code cells}, which counts what it hands over. */
    private static final class Slot {
        private int left;
        private final AtomicInteger loaded = new AtomicInteger();

        Slot(int cells) {
            this.left = cells;
        }

        boolean load() {
            if (left <= 0) {
                return false;
            }
            left--;
            loaded.incrementAndGet();
            return true;
        }
    }

    private static ReactorCycle.Tick run(int ticksLeft, Slot slot) {
        return ReactorCycle.tick(800, 200, true, PER_TICK, ticksLeft, CELL, slot::load);
    }

    @Test
    @DisplayName("with water, room and a cell it loads the cell and converts a tick's worth")
    void runsATick() {
        Slot slot = new Slot(1);
        ReactorCycle.Tick tick = run(0, slot);
        assertEquals(PER_TICK, tick.converted());
        assertEquals(CELL - 1, tick.ticksLeft());
        assertFalse(tick.burntOut());
        assertEquals(1, slot.loaded.get());
    }

    @Test
    @DisplayName("a cell lasts 4,000 ticks of running and burns out on the last")
    void cellLastsItsTicks() {
        Slot slot = new Slot(2);
        int left = 0;
        for (int t = 1; t < CELL; t++) {
            ReactorCycle.Tick tick = run(left, slot);
            assertFalse(tick.burntOut(), "burnt out on tick " + t);
            left = tick.ticksLeft();
        }
        ReactorCycle.Tick last = run(left, slot);
        assertTrue(last.burntOut());
        assertEquals(0, last.ticksLeft());
        assertEquals(1, slot.loaded.get(), "a second cell was loaded before the first burnt out");
        assertEquals(PER_TICK, run(0, slot).converted());
        assertEquals(2, slot.loaded.get());
    }

    @Test
    @DisplayName("with no water it burns nothing and loads nothing")
    void noWater() {
        Slot slot = new Slot(1);
        ReactorCycle.Tick tick = ReactorCycle.tick(PER_TICK - 1, 200, true, PER_TICK, 0, CELL, slot::load);
        assertEquals(0, tick.converted());
        assertEquals(0, slot.loaded.get());
        ReactorCycle.Tick midCell = ReactorCycle.tick(0, 200, true, PER_TICK, 100, CELL, slot::load);
        assertEquals(100, midCell.ticksLeft(), "a dry Reactor burnt its cell");
    }

    @Test
    @DisplayName("with no room for its steam it burns nothing")
    void noRoom() {
        Slot slot = new Slot(1);
        ReactorCycle.Tick tick = ReactorCycle.tick(800, PER_TICK - 1, true, PER_TICK, 100, CELL, slot::load);
        assertEquals(0, tick.converted());
        assertEquals(100, tick.ticksLeft());
        assertEquals(0, ReactorCycle.tick(800, 0, true, PER_TICK, 0, CELL, slot::load).converted());
        assertEquals(0, slot.loaded.get());
    }

    @Test
    @DisplayName("with no room for a depleted cell it stops")
    void fullDepletedSlot() {
        Slot slot = new Slot(1);
        ReactorCycle.Tick tick = ReactorCycle.tick(800, 200, false, PER_TICK, 100, CELL, slot::load);
        assertEquals(0, tick.converted());
        assertEquals(100, tick.ticksLeft());
        assertEquals(0, ReactorCycle.tick(800, 200, false, PER_TICK, 0, CELL, slot::load).converted());
        assertEquals(0, slot.loaded.get());
    }

    @Test
    @DisplayName("with no cell it makes nothing")
    void noFuel() {
        ReactorCycle.Tick tick = run(0, new Slot(0));
        assertEquals(0, tick.converted());
        assertEquals(0, tick.ticksLeft());
        assertFalse(tick.burntOut());
    }
}
