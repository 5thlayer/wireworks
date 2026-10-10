// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.jspecify.annotations.Nullable;

/**
 * Which of the Boiler's two fluid tanks a block of its footprint opens onto. The front row, local
 * {@code x = 0}, is water, the back middle is steam, and the back corners are neither. Local {@code x}
 * runs backward from the front, as in {@link SteamFootprints}.
 *
 * <p>Pure: no Minecraft types.
 */
public enum BoilerPort {
    WATER,
    STEAM;

    public static @Nullable BoilerPort at(int x, int y, int z) {
        if (y != 0) {
            return null;
        }
        if (x == 0) {
            return WATER;
        }
        return x == 1 && z == 0 ? STEAM : null;
    }
}
