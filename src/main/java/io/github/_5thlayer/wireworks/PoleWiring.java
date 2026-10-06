// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The wiring rules (ADR 0004). Pure: no Minecraft types.
 */
public final class PoleWiring {

    /**
     * Factorio's {@code auto_connect_up_to_n_wires} default. It caps only the wires placement adds;
     * a wire made by hand has no cap, as in Factorio 2.0.7 and later.
     */
    public static final int AUTO_WIRES = 5;

    /** What a second click with a wire tool did. */
    public enum Click {
        WIRED, CUT, CANCELLED, REFUSED
    }

    private PoleWiring() {
    }

    /** Whether a second click on {@code target} would be refused; the slack wire turns red on it. */
    public static boolean refuses(PoleNetworks.Pole anchor, PoleNetworks.Pole target) {
        return !pos(anchor).equals(pos(target))
                && !(mayWire(anchor, target) && PoleNetworks.withinReach(anchor, target));
    }

    /**
     * Whether two poles' kinds may be wired at all, whatever their distance: a Transformer wires to a
     * pole of either system but not to another Transformer, and a Distribution Pole never to a
     * Transmission Pole, since only a Transformer joins the two systems.
     */
    public static boolean mayWire(PoleNetworks.Pole a, PoleNetworks.Pole b) {
        boolean aTransformer = a.kind() instanceof PoleKind.Transformer;
        boolean bTransformer = b.kind() instanceof PoleKind.Transformer;
        if (aTransformer || bTransformer) {
            return !(aTransformer && bTransformer);
        }
        return (a.kind() == PoleKind.TRANSMISSION) == (b.kind() == PoleKind.TRANSMISSION);
    }

    /** Applies a wire tool's second click, on {@code target}, to the wire set. */
    public static Click click(PoleNetworks.Pole anchor, PoleNetworks.Pole target, WireSet wires) {
        if (pos(anchor).equals(pos(target))) {
            return Click.CANCELLED;
        }
        if (refuses(anchor, target)) {
            return Click.REFUSED;
        }
        if (wires.contains(pos(anchor), pos(target))) {
            wires.remove(pos(anchor), pos(target));
            return Click.CUT;
        }
        wires.add(pos(anchor), pos(target), WireSystem.between(anchor.kind(), target.kind()));
        return Click.WIRED;
    }

    /**
     * The wires the Placement Preview draws for a held pole (ADR 0004): the same {@link #onPlace} the
     * server runs, asked of a hypothetical pole at the aimed spot, and nothing where the placement
     * only grows a column, since a column that grew adds no wire.
     *
     * @param joinsAColumn the placement would extend or join a standing column rather than start one
     */
    public static List<PoleNetworks.Pole> wouldAdd(PoleNetworks.Pole placed, Collection<PoleNetworks.Pole> standing,
                                                WireSet wires, boolean joinsAColumn) {
        return joinsAColumn ? List.of() : onPlace(placed, standing, wires);
    }

    /** The standing poles a newly placed pole wires itself to. */
    public static List<PoleNetworks.Pole> onPlace(PoleNetworks.Pole placed, Collection<PoleNetworks.Pole> standing,
                                               WireSet wires) {
        List<PoleNetworks.Pole> candidates = new ArrayList<>(standing);
        candidates.sort(Comparator.<PoleNetworks.Pole>comparingLong(other -> distanceSquared(placed, other))
                .thenComparingInt(PoleNetworks.Pole::x)
                .thenComparingInt(PoleNetworks.Pole::y)
                .thenComparingInt(PoleNetworks.Pole::z));
        List<PoleNetworks.Pole> wired = new ArrayList<>();
        boolean placedTransformer = placed.kind() instanceof PoleKind.Transformer;
        boolean wiredTransmission = false;
        boolean wiredDistribution = false;
        for (PoleNetworks.Pole other : candidates) {
            if (wired.size() == AUTO_WIRES) {
                break;
            }
            if (!mayWire(placed, other) || !PoleNetworks.withinReach(placed, other)) {
                continue;
            }
            if (placedTransformer) {
                // One pole of each system, the nearest in reach; neighbour sharing does not apply.
                boolean transmission = other.kind() == PoleKind.TRANSMISSION;
                if (transmission ? wiredTransmission : wiredDistribution) {
                    continue;
                }
                wiredTransmission |= transmission;
                wiredDistribution |= !transmission;
                wired.add(other);
            } else if (!sharesANeighbour(other, wired, wires)) {
                wired.add(other);
            }
        }
        return wired;
    }

    /** Whether {@code other} is already wired to a pole the placed one has just wired to. */
    private static boolean sharesANeighbour(PoleNetworks.Pole other, List<PoleNetworks.Pole> wired, WireSet wires) {
        for (PoleNetworks.Pole chosen : wired) {
            if (wires.contains(pos(chosen), pos(other))) {
                return true;
            }
        }
        return false;
    }

    private static PoleNetworks.Pos pos(PoleNetworks.Pole p) {
        return new PoleNetworks.Pos(p.x(), p.y(), p.z());
    }

    private static long distanceSquared(PoleNetworks.Pole a, PoleNetworks.Pole b) {
        long dx = a.x() - b.x();
        long dy = a.y() - b.y();
        long dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
