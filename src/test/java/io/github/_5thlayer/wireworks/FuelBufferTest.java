// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FuelBufferTest {

    @Test
    void aPartTickIsNotATick() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(4_000L);
        assertFalse(buffer.drawTick(4_500L));
        assertEquals(4_000L, buffer.storedJoules(), "a tick it could not pay spent nothing");
    }

    @Test
    void theRemainderStaysBankedForTheNextItem() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(100L);
        assertTrue(buffer.drawTick(60L));
        assertFalse(buffer.drawTick(60L));
        buffer.light(100L);
        assertEquals(140L, buffer.storedJoules());
    }

    @Test
    void theGaugeFillsToTheLastItemLit() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(1_000L);
        buffer.drawTick(250L);
        assertEquals(1_000L, buffer.gaugeCapacity());
    }

    @Test
    void loadKeepsBothFieldsAndRefusesNegatives() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.load(30L, 90L);
        assertEquals(30L, buffer.storedJoules());
        assertEquals(90L, buffer.lastLitJoules());
        buffer.load(-5L, -5L);
        assertEquals(0L, buffer.storedJoules());
    }
}
