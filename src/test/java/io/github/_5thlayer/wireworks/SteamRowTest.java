// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Steam crosses a generator's row in at one end and out of the other, from one supply a tick. */
class SteamRowTest {

    private enum End {
        FRONT,
        BACK
    }

    @Test
    void beforeAnySteamCameInAPullDrawsAtEitherEnd() {
        SteamRow<End> row = new SteamRow<>();
        SteamRow<End>.Pull pull = row.pull();

        assertNull(row.passOn());
        assertTrue(pull.drawsAt(End.FRONT));
        assertTrue(pull.drawsAt(End.BACK));
    }

    @Test
    void aPullLeavesAloneTheEndTheSteamGoesOutOf() {
        SteamRow<End> row = new SteamRow<>();
        row.passesOnAt(End.FRONT);
        SteamRow<End>.Pull pull = row.pull();

        assertTrue(pull.drawsAt(End.BACK));
        assertFalse(pull.drawsAt(End.FRONT));
    }

    @Test
    void theFirstSupplyOfATickEndsItsPullAtBothEnds() {
        SteamRow<End> row = new SteamRow<>();
        SteamRow<End>.Pull pull = row.pull();

        row.passesOnAt(End.FRONT);

        assertFalse(pull.drawsAt(End.BACK));
        assertFalse(pull.drawsAt(End.FRONT));
        assertTrue(row.pull().drawsAt(End.BACK));
    }

    @Test
    void steamComingInTheWayItAlreadyGoesKeepsThePullGoing() {
        SteamRow<End> row = new SteamRow<>();
        row.passesOnAt(End.FRONT);
        SteamRow<End>.Pull pull = row.pull();

        row.passesOnAt(End.FRONT);

        assertTrue(pull.drawsAt(End.BACK));
    }

    @Test
    void steamPushedInAtTheOtherEndTurnsTheWay() {
        SteamRow<End> row = new SteamRow<>();
        row.passesOnAt(End.FRONT);

        row.passesOnAt(End.BACK);

        assertEquals(End.BACK, row.passOn());
        assertTrue(row.pull().drawsAt(End.FRONT));
    }

    @Test
    void aLoadRestoresTheWay() {
        SteamRow<End> row = new SteamRow<>();

        row.restore(End.BACK);

        assertEquals(End.BACK, row.passOn());
    }
}
