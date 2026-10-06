// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * Whether a wire's first end is still held by a wire tool (ADR 0004, ADR 0006).
 *
 * <p>Pure: no Minecraft types.
 */
public final class PendingEnd {

    /**
     * The player holding the end, as the end's hold on them is judged.
     *
     * @param x                the player's position, measured to the anchor's block centre
     * @param interactionRange how far the player can click; the end is held to the anchor's reach
     *                         plus this, so a pole beyond reach can still be clicked and refused
     * @param toolInMainHand   the tool that holds the end is still in the main hand
     * @param sameDimension    the player is in the anchor's dimension
     */
    public record Holder(double x, double y, double z, double interactionRange,
                         boolean toolInMainHand, boolean sameDimension) {
    }

    private PendingEnd() {
    }

    /** @param anchorStanding the anchor pole is still in the world */
    public static boolean stillHeld(PoleNetworks.Pole anchor, boolean anchorStanding, Holder holder) {
        double dx = holder.x() - (anchor.x() + 0.5);
        double dy = holder.y() - (anchor.y() + 0.5);
        double dz = holder.z() - (anchor.z() + 0.5);
        double reach = anchor.kind().longestReach() + holder.interactionRange();
        boolean inReach = dx * dx + dy * dy + dz * dz <= reach * reach;
        return anchorStanding && holder.toolInMainHand() && holder.sameDimension() && inReach;
    }
}
