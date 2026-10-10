// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Steam Engine's arithmetic: Factorio's 0.5 steam units per tick and 900 kW, frozen here as the
 * Library's own numbers (ADR-0115).
 *
 * <p>An engine burns that steam, and burns none when its buffer has no room for what it would make.
 *
 * <p>The trap is the half millibucket. 0.5 units per Factorio tick at 60 ticks a second is 30 mB/s,
 * which is 1.5 mB per Minecraft tick, and a tank moves whole millibuckets. Flooring 1.5 to 1 is an
 * engine at two thirds of its rate with nothing on the gauge to say so, so the fraction is carried
 * from tick to tick in a {@link Carry}, and energy likewise.
 *
 * <p>Pure: no Minecraft types.
 */
public final class SteamEngineSpec {

    /** Factorio's steam engine: 0.5 units per tick, 900 kW, a 200-unit steam box. */
    public static final SteamEngineSpec FACTORIO = new SteamEngineSpec(0.5, 900_000.0, 200);

    private static final int FACTORIO_TICKS_PER_SECOND = 60;
    private static final int MINECRAFT_TICKS_PER_SECOND = 20;

    /** Guards a floor against a sum like 0.1 + 0.2 landing a hair under a whole number. */
    private static final double EPSILON = 1e-9;

    private final double steamPerTick;
    private final double energyPerTick;
    private final int fluidBoxVolume;

    SteamEngineSpec(double fluidUsagePerFactorioTick, double maxPowerOutputWatts, int fluidBoxVolume) {
        this.steamPerTick = fluidUsagePerFactorioTick * FACTORIO_TICKS_PER_SECOND / MINECRAFT_TICKS_PER_SECOND;
        this.energyPerTick = maxPowerOutputWatts / MINECRAFT_TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE;
        this.fluidBoxVolume = fluidBoxVolume;
    }

    /** The fractions of a millibucket and of an FE owed from earlier ticks. */
    public record Carry(double steam, double energy) {
        public static final Carry NONE = new Carry(0.0, 0.0);
    }

    /** What the engine draws from its tank this tick, in whole millibuckets, and what it leaves owed. */
    public record Request(int steam, Carry carry) {
    }

    /** What one tick burnt and made, in whole units, and what it leaves owed. */
    public record Tick(int steam, long energy, Carry carry) {
    }

    /** The engine's steam, in mB a second. */
    public double steamPerSecond() {
        return steamPerTick * MINECRAFT_TICKS_PER_SECOND;
    }

    /** The engine's output, in FE a tick. */
    public double energyPerTick() {
        return energyPerTick;
    }

    /** The engine's steam tank, in mB. */
    public int portCapacity() {
        return fluidBoxVolume;
    }

    /** One tick of output, so the buffer hides no outage. */
    public long bufferCapacity() {
        return Math.round(energyPerTick);
    }

    /**
     * How much steam the engine asks for this tick, before anything is drawn.
     *
     * <p>{@code energyRoom} is what the FE buffer can still take. The buffer is one tick of output,
     * so a pole that drew less than that last tick leaves less room than a full burn makes, and the
     * request is cut: steam burnt into a full buffer is steam spent for nothing. The cut keeps every
     * millibucket whose energy <em>starts</em> inside the room, and {@link #burn} carries the part of
     * the last one that overshoots, since a millibucket is 300 FE and the carried half millibucket's
     * tick never fits whole in a drained buffer. A cut request owes no steam forward, or an engine
     * held back for a minute would owe a burst.
     */
    public Request request(Carry carry, long energyRoom) {
        double exact = steamPerTick + carry.steam();
        int whole = (int) Math.floor(exact + EPSILON);
        int fits = (int) Math.ceil((energyRoom - carry.energy()) / energyPerUnit() - EPSILON);
        if (fits < whole) {
            return new Request(Math.max(0, fits), new Carry(0.0, carry.energy()));
        }
        return new Request(whole, new Carry(Math.max(0.0, exact - whole), carry.energy()));
    }

    /** What one millibucket is worth, in FE. */
    private double energyPerUnit() {
        return energyPerTick / steamPerTick;
    }

    /**
     * What {@code steam} millibuckets actually drawn are worth, of which no more than
     * {@code energyRoom} is paid out this tick; the rest is owed.
     *
     * <p>Split from {@link #request} because the tank may hold less than was asked for, and the
     * energy is owed on what was burnt, not on what was wanted.
     */
    public Tick burn(int steam, Carry carry, long energyRoom) {
        double exact = steam * energyPerUnit() + carry.energy();
        long whole = Math.max(0L, Math.min((long) Math.floor(exact + EPSILON), energyRoom));
        return new Tick(steam, whole, new Carry(carry.steam(), Math.max(0.0, exact - whole)));
    }

    /** {@link #request} then {@link #burn}, when the tank covers the whole request. */
    public Tick tick(Carry carry, long energyRoom) {
        Request asked = request(carry, energyRoom);
        return burn(asked.steam(), asked.carry(), energyRoom);
    }
}
