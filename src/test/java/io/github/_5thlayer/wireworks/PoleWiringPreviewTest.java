// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the Placement Preview draws for a held pole (ADR 0004, ADR 0005): exactly the wires placing it
 * would add, and none where the placement only grows a column.
 */
class PoleWiringPreviewTest {

    private static PoleNetworks.Pole small(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, PoleKind.distribution(PoleTier.SMALL));
    }

    @Test
    void thePreviewNamesTheWiresAPlacementWouldAdd() {
        PoleNetworks.Pole placed = small(0, 64, 0);
        List<PoleNetworks.Pole> standing = List.of(small(5, 64, 0), small(0, 64, 5));
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.onPlace(placed, standing, wires),
                PoleWiring.wouldAdd(placed, standing, wires, false));
    }

    @Test
    void aPlacementThatOnlyGrowsAColumnDrawsNoWires() {
        PoleNetworks.Pole placed = small(0, 64, 0);
        List<PoleNetworks.Pole> standing = List.of(small(5, 64, 0));
        assertTrue(PoleWiring.wouldAdd(placed, standing, new WireSet(), true).isEmpty());
    }

    @Test
    void thePreviewChangesNoWire() {
        PoleNetworks.Pole placed = small(0, 64, 0);
        WireSet wires = new WireSet();
        PoleWiring.wouldAdd(placed, List.of(small(5, 64, 0)), wires, false);
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void aPoleOutOfReachIsNotDrawn() {
        PoleNetworks.Pole placed = small(0, 64, 0);
        assertTrue(PoleWiring.wouldAdd(placed, List.of(small(20, 64, 0)), new WireSet(), false).isEmpty());
    }

    private static final PoleKind TRANSFORMER = PoleKind.transformer(30, 9);

    private static PoleNetworks.Pole transformerAt(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, TRANSFORMER);
    }

    private static PoleNetworks.Pole line(int x, int y, int z) {
        return new PoleNetworks.Pole(x, y, z, PoleKind.TRANSMISSION);
    }

    @Test
    void aTransformerPreviewIsTheNearestPoleOfEachSystemAsPlacementWiresIt() {
        PoleNetworks.Pole placed = transformerAt(0, 64, 0);
        List<PoleNetworks.Pole> standing = List.of(line(20, 64, 0), line(10, 64, 0), line(0, 64, 12),
                small(3, 64, 0), small(0, 64, 4), small(30, 64, 0));
        WireSet wires = new WireSet();
        List<PoleNetworks.Pole> placement = PoleWiring.onPlace(placed, standing, wires);
        assertEquals(List.of(small(3, 64, 0), line(10, 64, 0)), placement);
        assertEquals(placement, PoleWiring.wouldAdd(placed, standing, wires, false));
    }

    @Test
    void aTransformerPreviewDrawsNoDistributionWireWithNoDistributionPoleInReach() {
        PoleNetworks.Pole placed = transformerAt(0, 64, 0);
        List<PoleNetworks.Pole> standing = List.of(line(10, 64, 0), small(12, 64, 0));
        assertEquals(List.of(line(10, 64, 0)), PoleWiring.wouldAdd(placed, standing, new WireSet(), false));
    }

    @Test
    void aTransformerNextToAnotherBlockOfItsKindStillWiresWhereItIsAColumnsBaseOnly() {
        // A Transformer is not a column: standing above one makes it no extension, so the placement
        // wires; standing below one is not a base, so it does not.
        assertEquals(false, PoleWiring.joinsAColumn(false, false, true));
        assertEquals(true, PoleWiring.joinsAColumn(false, true, false));
        // A stacking pole joins a column from either end.
        assertEquals(true, PoleWiring.joinsAColumn(true, false, true));
        assertEquals(true, PoleWiring.joinsAColumn(true, true, false));
        assertEquals(false, PoleWiring.joinsAColumn(true, false, false));
    }
}
