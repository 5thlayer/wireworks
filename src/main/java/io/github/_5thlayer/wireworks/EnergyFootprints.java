// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.FootprintShape;
import io.github._5thlayer.groundworks.FootprintShape.Local;

import java.util.ArrayList;
import java.util.List;

/** Where the blocks of each footprint stand, in the anchor's frame. */
final class EnergyFootprints {

    /** A pillar of one block, the anchor, under a 3x3 top layer. */
    static final FootprintShape SOLAR_PANEL = solarPanel();

    /** Factorio's flat 2x2: the anchor, one beside it, and the two behind those. */
    static final FootprintShape ACCUMULATOR = FootprintShape.of(
            new Local(1, 0, 0), new Local(0, 0, 1), new Local(1, 0, 1));

    private EnergyFootprints() {
    }

    private static FootprintShape solarPanel() {
        List<Local> parts = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                parts.add(new Local(x, 1, z));
            }
        }
        return FootprintShape.of(parts.toArray(Local[]::new));
    }
}
