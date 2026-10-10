// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * A burner's fuel, held as joules rather than as burn ticks. Lighting an item banks its whole value;
 * a tick of work spends the machine's draw for that tick. Burn time is the quotient and is never stored.
 *
 * <p>A part-tick is not a tick: {@link #drawTick} is all or nothing, so 4,000,000 J of coal buys 44
 * whole ticks at 90,000 J and leaves 40,000 J banked, which the next item lights on top of. The buffer
 * is uncapped: it holds at most the largest fuel value plus the sliver left over from the last.
 *
 * <p>Pure: no Minecraft types.
 */
public final class FuelBuffer {

    private long storedJoules;

    /** What the last item lit was worth: the denominator the gauge is drawn against. */
    private long lastLitJoules;

    public long storedJoules() {
        return storedJoules;
    }

    /** What the gauge fills to: the last item's worth, or the buffer itself if that is larger. */
    public long gaugeCapacity() {
        return Math.max(lastLitJoules, storedJoules);
    }

    public long lastLitJoules() {
        return lastLitJoules;
    }

    /** Banks one fuel item, consumed whole. */
    public void light(long fuelValue) {
        if (fuelValue <= 0L) {
            return;
        }
        storedJoules += fuelValue;
        lastLitJoules = fuelValue;
    }

    /**
     * Pays for one tick of operation, all or nothing.
     *
     * @return false when the buffer cannot cover the whole tick, the caller's cue to light another
     *     item and, if there is none, to stall without spending anything
     */
    public boolean drawTick(long joulesPerTick) {
        if (joulesPerTick <= 0L) {
            return true;
        }
        if (storedJoules < joulesPerTick) {
            return false;
        }
        storedJoules -= joulesPerTick;
        return true;
    }

    public void load(long storedJoules, long lastLitJoules) {
        this.storedJoules = Math.max(0L, storedJoules);
        this.lastLitJoules = Math.max(0L, lastLitJoules);
    }
}
