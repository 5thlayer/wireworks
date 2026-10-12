// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.jspecify.annotations.Nullable;

/**
 * Which of the Reactor's two fluid tanks a block of its 3x3x3 opens onto, in the anchor's frame: the
 * anchor is the bottom centre and local {@code x} runs backward, as the Boiler's does. The middle of
 * each side of the bottom layer is water, the back middle of the middle layer is superheated steam, and
 * every other block is neither.
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
        return x == 1 && y == 1 && z == 0 ? STEAM : null;
    }
}
