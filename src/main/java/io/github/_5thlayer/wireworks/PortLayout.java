// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Where a machine's fixed Ports stand in its footprint, which tank each reaches and which way it opens,
 * in the anchor's frame: local {@code x} runs backward from the front, as in {@link SteamFootprints}.
 * Every other block, and every other face of a Port's block, opens onto nothing.
 *
 * <p>Pure: no Minecraft types.
 */
record PortLayout(List<Port> ports) {

    /**
     * The Boiler's: water at each end of its front row, opening outward along it, and steam at its back
     * middle, opening backward.
     */
    static final PortLayout BOILER = new PortLayout(List.of(
            new Port(0, 0, -1, 0, -1, Tank.WATER),
            new Port(0, 0, 1, 0, 1, Tank.WATER),
            new Port(1, 0, 0, 1, 0, Tank.STEAM)));

    /**
     * The Reactor's: water at the middle of each side of its bottom layer, opening outward, and
     * superheated steam at the back middle of its middle layer, opening backward.
     */
    static final PortLayout REACTOR = new PortLayout(List.of(
            new Port(-1, 0, 0, -1, 0, Tank.WATER),
            new Port(1, 0, 0, 1, 0, Tank.WATER),
            new Port(0, 0, -1, 0, -1, Tank.WATER),
            new Port(0, 0, 1, 0, 1, Tank.WATER),
            new Port(1, 1, 0, 1, 0, Tank.STEAM)));

    PortLayout {
        ports = List.copyOf(ports);
    }

    /** The Port at this block of the footprint, or null where there is none. */
    @Nullable Port at(int x, int y, int z) {
        for (Port port : ports) {
            if (port.x() == x && port.y() == y && port.z() == z) {
                return port;
            }
        }
        return null;
    }

    /** Which of a machine's tanks a Port reaches: water, which it draws, or steam, which it pushes. */
    enum Tank {
        WATER,
        STEAM
    }

    /** A Port: the block at {@code x, y, z} opens toward {@code outX, outZ} onto its tank. */
    record Port(int x, int y, int z, int outX, int outZ, Tank tank) {
    }
}
