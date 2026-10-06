// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A wire looks like the system stored on it, so a player tells the systems apart at a glance (ADR 0008). */
class WireLookTest {

    private static float brightness(WireLook look) {
        return look.red() + look.green() + look.blue();
    }

    @Test
    void aDistributionWireKeepsVanillaLeashLook() {
        WireLook look = WireLook.of(WireSystem.DISTRIBUTION);
        assertEquals(0.05F, look.width());
        assertEquals(0.5F, look.red());
        assertEquals(0.4F, look.green());
        assertEquals(0.3F, look.blue());
    }

    @Test
    void aDistributionWireHangsOnVanillasCurve() {
        WireLook look = WireLook.of(WireSystem.DISTRIBUTION);
        // Rising: dy * p^2. Falling: dy - dy * (1 - p)^2. Level: straight.
        assertEquals(4.0F * 0.25F, look.height(4.0F, 10.0F, 0.5F), 1e-6F);
        assertEquals(-4.0F + 4.0F * 0.25F, look.height(-4.0F, 10.0F, 0.5F), 1e-6F);
        assertEquals(0.0F, look.height(0.0F, 30.0F, 0.5F), 1e-6F);
    }

    @Test
    void aTransmissionWireIsThickerAndDarker() {
        WireLook distribution = WireLook.of(WireSystem.DISTRIBUTION);
        WireLook transmission = WireLook.of(WireSystem.TRANSMISSION);
        assertEquals(2.0F, transmission.width() / distribution.width(), 1e-6F);
        assertTrue(brightness(transmission) < brightness(distribution));
    }

    @Test
    void aTransmissionWireSagsDeeperOverTheSameSpan() {
        WireLook distribution = WireLook.of(WireSystem.DISTRIBUTION);
        WireLook transmission = WireLook.of(WireSystem.TRANSMISSION);
        // Same span, falling 4 over 20: the droop below the straight chord is 1.5 times deeper.
        float dy = -4.0F;
        float chord = dy * 0.5F;
        float ordinary = chord - distribution.height(dy, 20.0F, 0.5F);
        float deeper = chord - transmission.height(dy, 20.0F, 0.5F);
        assertTrue(ordinary > 0.0F);
        assertTrue(deeper >= 1.5F * ordinary - 1e-6F);
    }

    @Test
    void aLevelTransmissionWireStillSags() {
        assertTrue(WireLook.of(WireSystem.TRANSMISSION).height(0.0F, 30.0F, 0.5F) < -0.1F);
    }

    @Test
    void aWireMeetsItsEndsWhateverItsSag() {
        WireLook look = WireLook.of(WireSystem.TRANSMISSION);
        assertEquals(0.0F, look.height(-3.0F, 20.0F, 0.0F), 1e-6F);
        assertEquals(-3.0F, look.height(-3.0F, 20.0F, 1.0F), 1e-6F);
    }
}
