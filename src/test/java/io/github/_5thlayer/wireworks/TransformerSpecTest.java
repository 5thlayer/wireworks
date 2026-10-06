// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The Transformer's two configured reaches (ADR 0008): the line side and the District side. */
class TransformerSpecTest {

    @AfterEach
    void restoreDefaults() {
        TransformerSpec.configure(TransformerSpec.DEFAULT_LINE_REACH, TransformerSpec.DEFAULT_DISTRICT_REACH);
    }

    @Test
    void theLineSideDefaultsToTheTransmissionPolesDefaultAndTheDistrictSideToTheMediumPoles() {
        assertEquals(TransmissionSpec.DEFAULT_WIRE_REACH, TransformerSpec.lineReach());
        assertEquals(PoleTier.MEDIUM.defaultWireReach(), TransformerSpec.districtReach());
    }

    @Test
    void theKindCarriesTheConfiguredReaches() {
        TransformerSpec.configure(50, 12);
        PoleKind kind = TransformerSpec.kind();
        assertEquals(50, kind.reachToward(PoleKind.TRANSMISSION));
        assertEquals(12, kind.reachToward(PoleKind.distribution(PoleTier.LARGE)));
    }

    @Test
    void aReachThatIsNotPositiveIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> TransformerSpec.configure(0, 9));
        assertThrows(IllegalArgumentException.class, () -> TransformerSpec.configure(32, -1));
    }
}
