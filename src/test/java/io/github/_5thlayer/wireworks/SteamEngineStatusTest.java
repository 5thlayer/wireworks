// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The problem the Steam Engine's HUD line names. Where several hold, the first link of
 * the chain from steam to pole that is broken wins; a running engine names nothing.
 */
class SteamEngineStatusTest {

    @Test
    void aFedDrawnEngineNamesNothing() {
        assertEquals(Optional.empty(), SteamEngineStatus.of(true, true));
    }

    @Test
    void anEmptyTankIsNoSteamWhateverElseHolds() {
        assertEquals(Optional.of(SteamEngineStatus.NO_STEAM), SteamEngineStatus.of(false, true));
        assertEquals(Optional.of(SteamEngineStatus.NO_STEAM), SteamEngineStatus.of(false, false));
    }

    @Test
    void steamWithNoPoleIsNotInPoleArea() {
        assertEquals(Optional.of(SteamEngineStatus.NOT_IN_POLE_AREA), SteamEngineStatus.of(true, false));
    }

    @Test
    void anOrdinalOutOfRangeNamesNothing() {
        assertEquals(Optional.empty(), SteamEngineStatus.fromOrdinal(-1));
        assertEquals(Optional.empty(), SteamEngineStatus.fromOrdinal(SteamEngineStatus.values().length));
    }
}
