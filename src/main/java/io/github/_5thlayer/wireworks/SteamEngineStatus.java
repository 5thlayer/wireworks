// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.Locale;
import java.util.Optional;

/**
 * The problem the Steam Engine's HUD line names. A full buffer is left to Jade's energy row, so a
 * running engine names nothing.
 */
public enum SteamEngineStatus {
    NO_STEAM,
    NOT_IN_POLE_AREA;

    public static Optional<SteamEngineStatus> of(boolean hasSteam, boolean inPoleArea) {
        if (!hasSteam) {
            return Optional.of(NO_STEAM);
        }
        return inPoleArea ? Optional.empty() : Optional.of(NOT_IN_POLE_AREA);
    }

    /** A synced ordinal back to its status; -1, or one out of range from a mismatched peer, names nothing. */
    public static Optional<SteamEngineStatus> fromOrdinal(int ordinal) {
        SteamEngineStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? Optional.of(all[ordinal]) : Optional.empty();
    }

    public String langKey() {
        return "tooltip.wireworks.steam_engine.jade." + name().toLowerCase(Locale.ROOT);
    }
}
