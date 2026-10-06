// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The render pipeline draws every wire of a frame into one triangle strip, so a wire's vertices must
 * not connect to the next wire's: a join between two wires has no area to draw.
 */
class WireStripTest {

    private static final WireLook LOOK = WireLook.of(WireSystem.DISTRIBUTION);

    /** One triangle of a strip, in the strip's winding: odd triangles swap their first two vertices. */
    private record Triangle(WireStrip.Vertex a, WireStrip.Vertex b, WireStrip.Vertex c) {
        boolean hasArea() {
            float ux = b.x() - a.x(), uy = b.y() - a.y(), uz = b.z() - a.z();
            float vx = c.x() - a.x(), vy = c.y() - a.y(), vz = c.z() - a.z();
            float cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
            return cx != 0.0F || cy != 0.0F || cz != 0.0F;
        }
    }

    private static List<Triangle> triangles(List<WireStrip.Vertex> strip) {
        List<Triangle> out = new ArrayList<>();
        for (int i = 0; i + 2 < strip.size(); i++) {
            out.add(i % 2 == 0
                    ? new Triangle(strip.get(i), strip.get(i + 1), strip.get(i + 2))
                    : new Triangle(strip.get(i + 1), strip.get(i), strip.get(i + 2)));
        }
        return out;
    }

    private static List<Triangle> visible(List<WireStrip.Vertex> strip) {
        return triangles(strip).stream().filter(Triangle::hasArea).toList();
    }

    private static List<WireStrip.Vertex> concat(List<WireStrip.Vertex> first, List<WireStrip.Vertex> second) {
        List<WireStrip.Vertex> both = new ArrayList<>(first);
        both.addAll(second);
        return both;
    }

    @Test
    void aTriangleSpanningTwoWiresHasNoArea() {
        List<WireStrip.Vertex> first = WireStrip.vertices(4.0F, 1.0F, 3.0F, LOOK);
        List<WireStrip.Vertex> second = WireStrip.vertices(-6.0F, 2.0F, 5.0F, LOOK);
        List<Triangle> all = triangles(concat(first, second));
        // Triangle i uses vertices i..i+2, so these two are the ones holding vertices of both wires.
        assertFalse(all.get(first.size() - 2).hasArea());
        assertFalse(all.get(first.size() - 1).hasArea());
    }

    @Test
    void joiningWiresKeepsEachOnesOwnTriangles() {
        List<WireStrip.Vertex> first = WireStrip.vertices(4.0F, 1.0F, 3.0F, LOOK);
        List<WireStrip.Vertex> second = WireStrip.vertices(-6.0F, 2.0F, 5.0F, LOOK);
        List<Triangle> expected = new ArrayList<>(visible(WireStrip.bare(4.0F, 1.0F, 3.0F, LOOK)));
        expected.addAll(visible(WireStrip.bare(-6.0F, 2.0F, 5.0F, LOOK)));
        assertEquals(expected, visible(concat(first, second)));
    }

    @Test
    void aWireTakesAnEvenNumberOfVerticesSoTheNextOneKeepsItsWinding() {
        assertEquals(0, WireStrip.vertices(4.0F, 1.0F, 3.0F, LOOK).size() % 2);
    }

    @Test
    void aWireHasTwoVerticesPerStepThereAndBack() {
        List<WireStrip.Vertex> bare = WireStrip.bare(4.0F, 1.0F, 3.0F, LOOK);
        assertEquals(2 * 2 * (WireStrip.STEPS + 1), bare.size());
        assertTrue(bare.stream().allMatch(v -> v.step() >= 0 && v.step() <= WireStrip.STEPS));
    }
}
