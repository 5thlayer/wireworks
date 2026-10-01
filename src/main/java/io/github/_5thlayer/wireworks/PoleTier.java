// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The three electric poles. A tier carries geometry and nothing else: a supply area and a wire
 * reach, which a Consumer sets in {@code wireworks-server.toml} ({@link WireworksConfig}).
 *
 * <p>The defaults are Factorio's small, medium and substation poles: 5x5, 7x7 and 18x18, reaching
 * 7.5, 9 and 18 blocks.
 *
 * <p>An even-sided area on a one-block pole is offset half a block, keeping the block count exact
 * ({@link SupplyArea}).
 */
public enum PoleTier {
    SMALL(5, 7.5),
    MEDIUM(7, 9.0),
    SUBSTATION(18, 18.0);

    /**
     * How far up and down a pole supplies, for every tier. Two blocks either way covers a machine on
     * the pole's own floor, one sunk into it and one on a platform above, without powering the floor
     * below through the ceiling.
     */
    public static final int VERTICAL_RADIUS = 2;

    private static final AtomicInteger CONFIGURATIONS = new AtomicInteger();

    private final int defaultSupplySize;
    private final double defaultWireReach;
    private volatile int supplySize;
    private volatile double wireReach;

    PoleTier(int supplySize, double wireReach) {
        this.defaultSupplySize = supplySize;
        this.defaultWireReach = wireReach;
        this.supplySize = supplySize;
        this.wireReach = wireReach;
    }

    public double wireReach() {
        return wireReach;
    }

    public int supplySize() {
        return supplySize;
    }

    public int defaultSupplySize() {
        return defaultSupplySize;
    }

    public double defaultWireReach() {
        return defaultWireReach;
    }

    /** Set by {@link WireworksConfig} when the server config loads or reloads, on both sides. */
    void configure(int supplySize, double wireReach) {
        if (supplySize < 1 || wireReach <= 0) {
            throw new IllegalArgumentException(this + ": supply size " + supplySize + ", wire reach " + wireReach);
        }
        this.supplySize = supplySize;
        this.wireReach = wireReach;
        CONFIGURATIONS.incrementAndGet();
    }

    /** How many times a tier has been configured: a change means a reach may have shortened. */
    public static int configurations() {
        return CONFIGURATIONS.get();
    }

    /** The longest wire any tier reaches, which bounds how far a wire is drawn from. */
    public static double maxWireReach() {
        double max = 0;
        for (PoleTier tier : values()) {
            max = Math.max(max, tier.wireReach);
        }
        return max;
    }

    public int verticalRadius() {
        return VERTICAL_RADIUS;
    }

    public int minOffset() {
        return -(supplySize / 2);
    }

    public int maxOffset() {
        return (supplySize - 1) / 2;
    }

    public String blockName() {
        return serializedName() + "_electric_pole";
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
