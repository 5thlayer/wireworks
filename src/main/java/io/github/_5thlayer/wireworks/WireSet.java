// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Every wire in a level (FactoryWorks ADR-0068), stored once each, as a pair of pole bases.
 *
 * <p>Pure: DataFixerUpper's codec, no Minecraft types.
 */
public final class WireSet {

    private static final Codec<PoleNetworks.Pos> POS = Codec.INT.listOf(3, 3).xmap(
            l -> new PoleNetworks.Pos(l.get(0), l.get(1), l.get(2)),
            p -> List.of(p.x(), p.y(), p.z()));

    private static final Codec<PoleNetworks.Wire> WIRE = RecordCodecBuilder.create(i -> i.group(
            POS.fieldOf("a").forGetter(PoleNetworks.Wire::a),
            POS.fieldOf("b").forGetter(PoleNetworks.Wire::b)
    ).apply(i, PoleNetworks.Wire::new));

    public static final Codec<WireSet> CODEC = WIRE.listOf().xmap(
            list -> {
                WireSet set = new WireSet();
                list.forEach(w -> set.add(w.a(), w.b()));
                return set;
            },
            set -> List.copyOf(set.wires));

    private final Set<PoleNetworks.Wire> wires = new LinkedHashSet<>();

    public void add(PoleNetworks.Pos a, PoleNetworks.Pos b) {
        wires.add(pair(a, b));
    }

    public void remove(PoleNetworks.Pos a, PoleNetworks.Pos b) {
        wires.remove(pair(a, b));
    }

    public boolean contains(PoleNetworks.Pos a, PoleNetworks.Pos b) {
        return wires.contains(pair(a, b));
    }

    /**
     * Moves every wire at {@code from} to {@code to}: a column whose base changed keeps its wires
     * (factoryworks#309). A wire that would now join the column to itself is dropped, and two wires to the same
     * third pole collapse into one, the set being unordered pairs.
     */
    public void rekey(PoleNetworks.Pos from, PoleNetworks.Pos to) {
        List<PoleNetworks.Wire> moving = wires.stream()
                .filter(w -> w.a().equals(from) || w.b().equals(from))
                .toList();
        for (PoleNetworks.Wire wire : moving) {
            wires.remove(wire);
            PoleNetworks.Pos other = wire.a().equals(from) ? wire.b() : wire.a();
            if (!other.equals(to)) {
                add(to, other);
            }
        }
    }

    /** Cuts every wire with an end at {@code pole}, which is what breaking it does. */
    public void removeAllOf(PoleNetworks.Pos pole) {
        wires.removeIf(w -> w.a().equals(pole) || w.b().equals(pole));
    }

    /** Every wire with an end in chunk ({@code chunkX}, {@code chunkZ}): what a client watching it is sent. */
    public List<PoleNetworks.Wire> touching(int chunkX, int chunkZ) {
        return wires.stream().filter(w -> inChunk(w, chunkX, chunkZ)).toList();
    }

    /**
     * A client's copy of a chunk's wires, replaced whole when the chunk is sent. A wire cut while no
     * end was watched is dropped here, when either end is watched again.
     */
    public void replaceTouching(int chunkX, int chunkZ, Collection<PoleNetworks.Wire> sent) {
        wires.removeIf(w -> inChunk(w, chunkX, chunkZ));
        sent.forEach(w -> add(w.a(), w.b()));
    }

    private static boolean inChunk(PoleNetworks.Wire w, int chunkX, int chunkZ) {
        return inChunk(w.a(), chunkX, chunkZ) || inChunk(w.b(), chunkX, chunkZ);
    }

    private static boolean inChunk(PoleNetworks.Pos p, int chunkX, int chunkZ) {
        return (p.x() >> 4) == chunkX && (p.z() >> 4) == chunkZ;
    }

    /** One wire per unordered pair: the end that sorts first by position is always {@code a}. */
    private static PoleNetworks.Wire pair(PoleNetworks.Pos a, PoleNetworks.Pos b) {
        return sortsFirst(a, b) ? new PoleNetworks.Wire(a, b) : new PoleNetworks.Wire(b, a);
    }

    private static boolean sortsFirst(PoleNetworks.Pos a, PoleNetworks.Pos b) {
        if (a.x() != b.x()) {
            return a.x() < b.x();
        }
        if (a.y() != b.y()) {
            return a.y() < b.y();
        }
        return a.z() <= b.z();
    }

    public Set<PoleNetworks.Wire> all() {
        return Collections.unmodifiableSet(wires);
    }
}
