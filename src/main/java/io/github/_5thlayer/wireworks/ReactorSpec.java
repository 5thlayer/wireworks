// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Reactor's numbers, frozen as the Library's own (ADR-0115, ADR 0012).
 *
 * <p>Factorio's reactor is 40 MW. Read straight, through 500 degree steam at steam's 0.2 kJ, that is
 * 20.6 mB of superheated steam a tick, which no tank can move. So the Reactor feeds whole Turbines
 * instead: 40 MW wants 6.87 of Factorio's 5.82 MW Turbines, rounded up to seven, and seven Turbines
 * burn 21 mB/t. Water goes in unit for unit, so it takes 21 mB/t of water too. A cell lasts Factorio's
 * 200 seconds of running.
 *
 * <p>Pure: no Minecraft types.
 */
public final class ReactorSpec {

    public static final int MINECRAFT_TICKS_PER_SECOND = 20;

    /** Factorio's reactor, in watts. */
    public static final double FACTORIO_POWER = 40_000_000.0;

    /** How long Factorio's fuel cell lasts, in seconds of running. */
    public static final int CELL_SECONDS = 200;

    public static final int CELL_TICKS = CELL_SECONDS * MINECRAFT_TICKS_PER_SECOND;

    /** One of the Reactor's four water ports, and its steam port: Factorio's 200-unit fluid boxes. */
    public static final int PORT_VOLUME = 200;

    /** The middle of each side of the bottom layer is a water port, and the four are one tank. */
    public static final int WATER_PORTS = 4;

    public static final int WATER_CAPACITY = WATER_PORTS * PORT_VOLUME;
    public static final int STEAM_CAPACITY = PORT_VOLUME;

    /** How many Turbines the Reactor fills. */
    public static final int TURBINES = turbinesFor(FACTORIO_POWER, SteamTurbineSpec.FACTORIO);

    /** What one tick turns from water into superheated steam, unit for unit. */
    public static final int MILLIBUCKETS_PER_TICK = milliBucketsPerTick(TURBINES, SteamTurbineSpec.FACTORIO);

    private ReactorSpec() {
    }

    /** The whole number of these Turbines a reactor of {@code watts} fills, rounded up. */
    static int turbinesFor(double watts, SteamEngineSpec turbine) {
        double turbineWatts = turbine.energyPerTick() * MINECRAFT_TICKS_PER_SECOND * ForgeEnergy.JOULES_PER_FE;
        // A hair of float error must not round 6.0 up to 7.
        return (int) Math.ceil(watts / turbineWatts - 1e-9);
    }

    /**
     * The steam {@code turbines} of this spec burn each tick, refused when it is not a whole
     * millibucket: rounding it would run the Reactor at a rate nobody chose.
     */
    static int milliBucketsPerTick(int turbines, SteamEngineSpec turbine) {
        double perTick = turbines * turbine.steamPerSecond() / MINECRAFT_TICKS_PER_SECOND;
        long whole = Math.round(perTick);
        if (Math.abs(perTick - whole) > 1e-9) {
            throw new IllegalArgumentException(perTick + " mB a tick is not a whole millibucket");
        }
        return (int) whole;
    }

    /** What the Reactor makes, in mB a second. */
    public static int steamPerSecond() {
        return MILLIBUCKETS_PER_TICK * MINECRAFT_TICKS_PER_SECOND;
    }
}
