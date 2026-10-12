// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.jspecify.annotations.Nullable;

/**
 * Which of the Reactor's two fluid tanks a block of its 3x3x3 opens onto, in the anchor's frame: the
 * anchor is the bottom centre. The middle of each side of the bottom layer is water, the top centre
 * is superheated steam, and every other block is neither.
 *
 * <p>Pure: no Minecraft types.
 */
public enum ReactorPort {
    WATER,
    STEAM;

    public static @Nullable ReactorPort at(int x, int y, int z) {
        if (y == 0 && Math.abs(x) + Math.abs(z) == 1) {
            return WATER;
        }
        return x == 0 && y == 2 && z == 0 ? STEAM : null;
    }
}
