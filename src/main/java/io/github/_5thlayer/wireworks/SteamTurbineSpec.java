// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * The Steam Turbine's numbers: Factorio's 60 units a second of 500 degree steam for 5.82 MW, frozen
 * here as the Library's own (ADR-0115, ADR 0012). That is 3 mB/t for 2,910 FE/t, a whole millibucket,
 * and seven of them burn the Reactor's 21 mB/t.
 *
 * <p>The arithmetic is the Steam Engine's; only the numbers differ.
 */
public final class SteamTurbineSpec {

    /** Factorio's steam turbine: 1 unit per tick, 5.82 MW, a 200-unit steam box. */
    public static final SteamEngineSpec FACTORIO = SteamEngineSpec.wholeMillibuckets(1.0, 5_820_000.0, 200);

    private SteamTurbineSpec() {
    }
}
