// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.ArrayList;
import java.util.List;

/**
 * The vertex sequence of one wire as a triangle strip, with no Minecraft types so a JVM test can
 * read it.
 *
 * <p>The render pipeline draws every wire of a frame into one buffer, so consecutive wires would join
 * into one strip, and the triangles between one wire's end and the next one's start would show as
 * thin lines. {@link #vertices} therefore repeats its first and last vertex, which makes every
 * triangle that spans two wires zero-area.
 */
public final class WireStrip {

    /** Vanilla's leash segment count, so every wire here reads as the same wire. */
    public static final int STEPS = 24;

    /**
     * One corner of the strip: its offset from the wire's start, how far along the wire it lies (the
     * light is sampled there) and how bright it is drawn.
     */
    public record Vertex(float x, float y, float z, int step, float shade) {
    }

    private WireStrip() {
    }

    /**
     * The wire from its start to its end and back, padded so that it joins the strip around it
     * invisibly. The padding is two extra copies at each end, which keeps the count even and so
     * every triangle's winding the same as in {@link #bare}.
     */
    public static List<Vertex> vertices(float dx, float dy, float dz, WireLook look) {
        List<Vertex> bare = bare(dx, dy, dz, look);
        List<Vertex> padded = new ArrayList<>(bare.size() + 4);
        Vertex first = bare.get(0);
        Vertex last = bare.get(bare.size() - 1);
        padded.add(first);
        padded.add(first);
        padded.addAll(bare);
        padded.add(last);
        padded.add(last);
        return padded;
    }

    /** The wire's own strip, as vanilla's leash draws it: a forward pass, then a backward one. */
    public static List<Vertex> bare(float dx, float dy, float dz, WireLook look) {
        float width = look.width();
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        float offsetFactor = horizontal == 0.0F ? 0.0F : width / 2.0F / horizontal;
        float dxOff = dz * offsetFactor;
        float dzOff = dx * offsetFactor;
        List<Vertex> out = new ArrayList<>(4 * (STEPS + 1));
        for (int k = 0; k <= STEPS; k++) {
            step(out, dx, dy, dz, horizontal, look, width, width, dxOff, dzOff, k, false);
        }
        for (int k = STEPS; k >= 0; k--) {
            step(out, dx, dy, dz, horizontal, look, width, 0.0F, dxOff, dzOff, k, true);
        }
        return out;
    }

    private static void step(List<Vertex> out, float dx, float dy, float dz, float horizontal, WireLook look,
            float width, float fudge, float dxOff, float dzOff, int k, boolean backwards) {
        float progress = k / (float) STEPS;
        float shade = k % 2 == (backwards ? 1 : 0) ? 0.7F : 1.0F;
        float x = dx * progress;
        // The sag: vanilla's own curve for Distribution, so a previewed wire hangs where the stored one will.
        float y = look.height(dy, horizontal, progress);
        float z = dz * progress;
        out.add(new Vertex(x - dxOff, y + fudge, z + dzOff, k, shade));
        out.add(new Vertex(x + dxOff, y + width - fudge, z - dzOff, k, shade));
    }
}
