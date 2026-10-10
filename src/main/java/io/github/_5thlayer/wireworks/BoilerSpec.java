// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Boiler's arithmetic, frozen from Factorio's prototype as the Library's own numbers (ADR-0115):
 * a 1.8 MW draw, a 165 degree target, water at 15 degrees and steam's heat capacity of 0.2 kJ per
 * unit per degree.
 *
 * <p>There are two ways to get it wrong and both give a plausible number.
 *
 * <p>The governing heat capacity is <em>steam's</em>, not water's. Factorio pays for the rise at the
 * capacity of the fluid that comes out, 0.2 kJ against water's 2 kJ. Water's would run the Boiler at
 * 6 mB/s, a tenth of its rate.
 *
 * <p>{@code energy_consumption} is per second and the buffer is drained per tick. Minecraft's tick is
 * a twentieth of a second, so the per-tick figures are 90,000 J and 3 mB. Taking the joule figure as
 * already per tick gives a Boiler that eats twenty times its fuel and still makes steam.
 *
 * <p>One unit is one millibucket. Pure: no Minecraft types.
 */
public final class BoilerSpec {

    public static final int MINECRAFT_TICKS_PER_SECOND = 20;

    /** Joules a second the Boiler burns. */
    public static final double ENERGY_CONSUMPTION = 1_800_000.0;

    /** Degrees Celsius of the steam it makes and of the water it is fed. */
    public static final int TARGET_TEMPERATURE = 165;
    public static final int WATER_TEMPERATURE = 15;

    /** Steam's heat capacity in joules per unit per degree: the one that governs. */
    public static final double STEAM_HEAT_CAPACITY = 200.0;

    /** How much of a fuel item's value the burner banks. Factorio's is 1. */
    public static final double EFFECTIVITY = 1.0;

    /** One of the Boiler's three water ports, and its steam port: Factorio's 200-unit fluid boxes. */
    public static final int PORT_VOLUME = 200;

    /** The water front row is three ports joined into one tank. */
    public static final int WATER_PORTS = 3;

    public static final long JOULES_PER_TICK = joulesPerTick(ENERGY_CONSUMPTION);
    public static final long JOULES_PER_MILLIBUCKET =
            joulesPerMilliBucket(TARGET_TEMPERATURE, WATER_TEMPERATURE, STEAM_HEAT_CAPACITY);
    public static final int MILLIBUCKETS_PER_TICK = milliBucketsPerTick(JOULES_PER_TICK, JOULES_PER_MILLIBUCKET);

    private BoilerSpec() {
    }

    /**
     * What one tick of boiling costs, in joules: 1.8 MW over twenty ticks is 90,000 J.
     *
     * <p>{@code effectivity} is not applied here: it multiplies the fuel item's value on the way into
     * the buffer, where {@link BoilerBlockEntity} spends it.
     */
    public static long joulesPerTick(double energyConsumptionPerSecond) {
        return Math.round(energyConsumptionPerSecond / MINECRAFT_TICKS_PER_SECOND);
    }

    /**
     * What heating one unit of water to the target costs, in joules: 150 degrees at steam's
     * 0.2 kJ is 30,000 J.
     */
    public static long joulesPerMilliBucket(int targetTemperature, int sourceTemperature, double steamHeatCapacity) {
        int rise = targetTemperature - sourceTemperature;
        if (rise <= 0) {
            // Steam for nothing, in unlimited quantity. Factorio's prototype cannot say so.
            throw new IllegalArgumentException("a boiler targeting " + targetTemperature
                    + " C from water at " + sourceTemperature + " C would make steam for nothing");
        }
        return Math.round(rise * steamHeatCapacity);
    }

    /** Factorio's own figure: 60 mB a second. */
    public static int milliBucketsPerSecond(double energyConsumptionPerSecond, long joulesPerUnit) {
        return (int) (Math.round(energyConsumptionPerSecond) / joulesPerUnit);
    }

    /**
     * What one Minecraft tick converts: 3 mB of water into 3 mB of steam. Water in equals steam out,
     * unit for unit: the Boiler is a temperature change, not a reaction.
     */
    public static int milliBucketsPerTick(long joulesPerTick, long joulesPerUnit) {
        if (joulesPerTick % joulesPerUnit != 0) {
            // Integer division would boil at a rate nobody chose, a plausible number on the gauge.
            throw new IllegalArgumentException(joulesPerTick + " J a tick is not a whole number of "
                    + joulesPerUnit + " J units");
        }
        return (int) (joulesPerTick / joulesPerUnit);
    }

    public static int steamPerSecond() {
        return milliBucketsPerSecond(ENERGY_CONSUMPTION, JOULES_PER_MILLIBUCKET);
    }
}
