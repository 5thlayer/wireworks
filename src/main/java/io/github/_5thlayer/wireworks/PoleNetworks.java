// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which poles are one Electric Network (ADR 0003, ADR 0004).
 *
 * <p>Two poles are joined only by a stored wire, and a network is everything wired transitively. The
 * networks are recomputed from the poles standing and the wires between them whenever either set
 * changes, so a break that splits a network is not a separate operation from a place that merges two.
 * Wire Reach decides only which wires may exist ({@link #withinReach}).
 *
 * <p>Pure: no Minecraft types.
 */
public final class PoleNetworks {

    /** A pole's position and tier. */
    public record Pole(int x, int y, int z, PoleTier tier) {
    }

    /** A block position, the base of a pole's column. */
    public record Pos(int x, int y, int z) {
    }

    /** A stored wire between two poles' bases. */
    public record Wire(Pos a, Pos b) {
    }

    private PoleNetworks() {
    }

    /**
     * Whether two poles are within Wire Reach of each other: the shorter of their two reaches,
     * measured between block centres in three dimensions.
     */
    public static boolean withinReach(Pole a, Pole b) {
        double reach = Math.min(a.tier().wireReach(), b.tier().wireReach());
        long dx = a.x() - b.x();
        long dy = a.y() - b.y();
        long dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz <= reach * reach;
    }

    /**
     * A network id per pole, index-aligned with the input, numbered densely from zero in order of
     * first appearance. A wire with an end that is not among the poles joins nothing.
     */
    public static int[] networks(List<Pole> poles, Collection<Wire> wires) {
        int n = poles.size();
        int[] parent = new int[n];
        Map<Pos, Integer> index = new HashMap<>();
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            Pole p = poles.get(i);
            index.put(new Pos(p.x(), p.y(), p.z()), i);
        }
        for (Wire wire : wires) {
            Integer a = index.get(wire.a());
            Integer b = index.get(wire.b());
            if (a != null && b != null) {
                parent[root(parent, a)] = root(parent, b);
            }
        }
        return denseIds(parent);
    }

    private static int[] denseIds(int[] parent) {
        int n = parent.length;
        int[] ids = new int[n];
        int[] idOfRoot = new int[n];
        Arrays.fill(idOfRoot, -1);
        int next = 0;
        for (int i = 0; i < n; i++) {
            int r = root(parent, i);
            if (idOfRoot[r] < 0) {
                idOfRoot[r] = next++;
            }
            ids[i] = idOfRoot[r];
        }
        return ids;
    }

    private static int root(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }
}
