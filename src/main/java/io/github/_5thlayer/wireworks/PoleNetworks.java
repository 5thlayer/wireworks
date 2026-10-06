// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    /** A pole's position and kind. */
    public record Pole(int x, int y, int z, PoleKind kind) {
    }

    /** A block position, the base of a pole's column. */
    public record Pos(int x, int y, int z) {
    }

    /** A stored wire between two poles' bases, in one wire system. */
    public record Wire(Pos a, Pos b, WireSystem system) {

        /** A Distribution wire, which is every wire an old world holds. */
        public Wire(Pos a, Pos b) {
            this(a, b, WireSystem.DISTRIBUTION);
        }
    }

    /** The district id of a pole that belongs to no District: a Transformer or a Transmission Pole. */
    public static final int NO_DISTRICT = -1;

    /**
     * One Electric Network: its Districts by id, and each Transformer by its index among the poles.
     * Both lists are in order of first appearance.
     */
    public record ElectricNetwork(List<Integer> districts, List<Integer> transformers,
                                  Map<Integer, List<Integer>> transformersByDistrict) {

        /** The Transformers wired to a District, which join it to the rest of the network. */
        public List<Integer> transformersJoining(int district) {
            return transformersByDistrict.getOrDefault(district, List.of());
        }
    }

    /**
     * What the poles and wires make: a District id per pole ({@link #NO_DISTRICT} for a Transformer or
     * a Transmission Pole), an Electric Network id per pole, and the networks. Arrays are index-aligned
     * with the input poles, ids dense from zero in order of first appearance.
     */
    public record Topology(int[] districtOf, int[] networkOf, List<ElectricNetwork> networks) {
    }

    private PoleNetworks() {
    }

    /**
     * Districts and Electric Networks (ADR 0008). A District is the Distribution Poles joined by wires
     * between Distribution Poles; a Transformer joins none, only the network. A wire with an end that
     * is not among the poles joins nothing.
     */
    public static Topology topology(List<Pole> poles, Collection<Wire> wires) {
        int n = poles.size();
        int[] networkOf = networks(poles, wires);
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
            if (a != null && b != null && isDistribution(poles.get(a)) && isDistribution(poles.get(b))) {
                parent[root(parent, a)] = root(parent, b);
            }
        }
        int[] districtOf = new int[n];
        Map<Integer, Integer> idOfRoot = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (isDistribution(poles.get(i))) {
                int r = root(parent, i);
                Integer id = idOfRoot.get(r);
                if (id == null) {
                    id = idOfRoot.size();
                    idOfRoot.put(r, id);
                }
                districtOf[i] = id;
            } else {
                districtOf[i] = NO_DISTRICT;
            }
        }
        int networkCount = Arrays.stream(networkOf).max().orElse(-1) + 1;
        List<List<Integer>> districts = new ArrayList<>();
        List<List<Integer>> transformers = new ArrayList<>();
        List<Map<Integer, List<Integer>>> joining = new ArrayList<>();
        for (int i = 0; i < networkCount; i++) {
            districts.add(new ArrayList<>());
            transformers.add(new ArrayList<>());
            joining.add(new TreeMap<>());
        }
        for (int i = 0; i < n; i++) {
            if (districtOf[i] != NO_DISTRICT && !districts.get(networkOf[i]).contains(districtOf[i])) {
                districts.get(networkOf[i]).add(districtOf[i]);
            }
            if (poles.get(i).kind() instanceof PoleKind.Transformer) {
                transformers.get(networkOf[i]).add(i);
            }
        }
        for (Wire wire : wires) {
            Integer a = index.get(wire.a());
            Integer b = index.get(wire.b());
            if (a == null || b == null) {
                continue;
            }
            joinTransformer(poles, districtOf, networkOf, joining, a, b);
            joinTransformer(poles, districtOf, networkOf, joining, b, a);
        }
        List<ElectricNetwork> networks = new ArrayList<>();
        for (int i = 0; i < networkCount; i++) {
            joining.get(i).values().forEach(list -> list.sort(null));
            networks.add(new ElectricNetwork(districts.get(i), transformers.get(i), joining.get(i)));
        }
        return new Topology(districtOf, networkOf, networks);
    }

    private static void joinTransformer(List<Pole> poles, int[] districtOf, int[] networkOf,
                                        List<Map<Integer, List<Integer>>> joining, int transformer, int other) {
        if (poles.get(transformer).kind() instanceof PoleKind.Transformer && districtOf[other] != NO_DISTRICT) {
            List<Integer> list = joining.get(networkOf[transformer])
                    .computeIfAbsent(districtOf[other], d -> new ArrayList<>());
            if (!list.contains(transformer)) {
                list.add(transformer);
            }
        }
    }

    private static boolean isDistribution(Pole pole) {
        return pole.kind() instanceof PoleKind.Distribution;
    }

    /**
     * Whether two poles are within Wire Reach of each other: the shorter of their two reaches,
     * measured between block centres in three dimensions.
     */
    public static boolean withinReach(Pole a, Pole b) {
        double reach = PoleKind.wireReach(a.kind(), b.kind());
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
