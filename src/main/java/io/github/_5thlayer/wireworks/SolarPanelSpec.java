// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Solar Panel in FE, from the peak the config states in watts. The buffer is one tick of output,
 * so it hides no outage.
 *
 * <p>Set by {@link WireworksConfig} when the server config loads or reloads. Pure: no Minecraft types.
 */
public final class SolarPanelSpec {

    static final long DEFAULT_PEAK_WATTS = 60_000L;

    private static volatile long peakWatts = DEFAULT_PEAK_WATTS;

    private SolarPanelSpec() {
    }

    static void configure(long peakWatts) {
        if (peakWatts < 0L) {
            throw new IllegalArgumentException("peak watts " + peakWatts);
        }
        SolarPanelSpec.peakWatts = peakWatts;
    }

    public static long peakFePerTick() {
        return ForgeEnergy.fePerTick(peakWatts);
    }

    public static long bufferFe() {
        return peakFePerTick();
    }
}
