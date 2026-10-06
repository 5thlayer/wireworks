// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * How a wire looks, decided by the system stored on it and nothing else (ADR 0008): the client never
 * looks at the blocks at a wire's ends, since the far one is often in a chunk it has not loaded.
 * Pure, so the values and the curve are tested on a plain JVM; the client geometry only draws them.
 *
 * <p>Distribution is vanilla's leash exactly. Transmission is about twice as thick, a dark grey close
 * to iron, and sags about one and a half times as deep, plus a droop of its own so that a level span
 * hangs too (vanilla's curve is straight between poles of one height). These are starting values to
 * tune in game.
 */
public record WireLook(float width, float red, float green, float blue, float sagFactor, float levelSag) {

    private static final WireLook DISTRIBUTION = new WireLook(0.05F, 0.5F, 0.4F, 0.3F, 1.0F, 0.0F);
    private static final WireLook TRANSMISSION = new WireLook(0.10F, 0.28F, 0.28F, 0.30F, 1.5F, 0.012F);

    public static WireLook of(WireSystem system) {
        return system == WireSystem.TRANSMISSION ? TRANSMISSION : DISTRIBUTION;
    }

    /**
     * The wire's height above its start at {@code progress} (0 to 1) along a span that rises
     * {@code dy} over {@code horizontal} blocks. Vanilla's curve, with its droop below the straight
     * chord scaled by {@link #sagFactor}, and {@link #levelSag} blocks of droop per block of span added.
     */
    public float height(float dy, float horizontal, float progress) {
        float chord = dy * progress;
        float vanilla = dy > 0.0F
                ? dy * progress * progress
                : dy - dy * (1.0F - progress) * (1.0F - progress);
        float droop = (vanilla - chord) * sagFactor;
        // 4p(1-p) is 1 at the middle and 0 at both ends, so the ends stay on the poles.
        return chord + droop - levelSag * horizontal * 4.0F * progress * (1.0F - progress);
    }
}
