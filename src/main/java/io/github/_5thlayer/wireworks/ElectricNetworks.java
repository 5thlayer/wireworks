// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Every Electric Network in a level, and the tick that settles them (ADR 0003, ADR 0008).
 *
 * <h2>Networks follow the stored wires</h2>
 *
 * <p>A pole reports itself every tick it runs: a Distribution Pole, a Transmission Pole or a
 * Transformer ({@link NetworkPole}). A pole that did not report -- broken, unloaded, or turned into a
 * column extension -- is dropped at the level tick. Any change to the set of poles, or a wire made or
 * cut ({@link LevelWires}), rebuilds the Districts and Electric Networks from the wires between the
 * poles standing (ADR 0004, {@link PoleNetworks#topology}). A merge and a split are the same
 * recomputation.
 *
 * <h2>One settlement per Electric Network</h2>
 *
 * <p>A block inside two wired poles' areas is one consumer, not two: the member poles' lists are
 * unioned by position before anything is probed. Room and offer are each measured with an insert or
 * extract inside a transaction that is then aborted, since the transfer API has no "how much"
 * question, and {@link NetworkBalance#settleNetwork} decides the flows: each District on its own book
 * first, then the network across its Districts. Sources are weighted by what they offer this tick; for
 * a generator whose face caps extraction at its rated output, that is its maximum output. The flows of
 * every District in the network are applied in a single transaction. A network with no Transformer is
 * one District and settles exactly as it always did.
 */
public final class ElectricNetworks {

    private static final Map<Level, ElectricNetworks> BY_LEVEL = new WeakHashMap<>();

    /**
     * An offer large enough to cover any area, and small enough to sum without overflow. It also
     * charges accumulators from nothing, which is what a creative pole is for.
     *
     * <p>Every draw, grant and charge is bounded by a probe of {@code Integer.MAX_VALUE} or by this,
     * which is what makes the {@code int} casts in {@link #insert} and {@link #extract} safe.
     */
    private static final long CREATIVE_OFFER = Integer.MAX_VALUE;

    /** One Electric Network: every pole wired into it, and the supply poles of each of its Districts. */
    private record Network(List<NetworkPole> members, List<List<SupplyAreaPoleBlockEntity>> districts,
                           List<List<NetworkPole>> transformers) {
    }

    /** One District's machines, as found by its poles this tick. */
    private static final class Book {
        final List<SupplyAreaPoleBlockEntity> poles;
        final int creative;
        final List<EnergyHandler> generators;
        final List<BlockPos> accumulatorsAt;
        final List<EnergyHandler> accumulators;
        final List<EnergyHandler> consumers;
        long demanded;

        Book(List<SupplyAreaPoleBlockEntity> poles, int creative, List<EnergyHandler> generators,
                List<BlockPos> accumulatorsAt, List<EnergyHandler> accumulators, List<EnergyHandler> consumers) {
            this.poles = poles;
            this.creative = creative;
            this.generators = generators;
            this.accumulatorsAt = accumulatorsAt;
            this.accumulators = accumulators;
            this.consumers = consumers;
        }
    }

    private final Map<BlockPos, NetworkPole> poles = new LinkedHashMap<>();
    private final Map<BlockPos, Long> lastReport = new LinkedHashMap<>();
    private List<Network> networks = List.of();
    private Map<BlockPos, Long> accumulatorFlows = Map.of();
    private boolean dirty;

    private ElectricNetworks() {
    }

    public static ElectricNetworks of(Level level) {
        return BY_LEVEL.computeIfAbsent(level, l -> new ElectricNetworks());
    }

    /**
     * A pole reports itself: a Distribution Pole, a Transmission Pole or a Transformer. A change to
     * the set of any of the three rebuilds the topology.
     */
    void report(NetworkPole pole) {
        BlockPos pos = pole.getBlockPos();
        if (poles.put(pos, pole) != pole) {
            dirty = true;
        }
        lastReport.put(pos, pole.getLevel().getGameTime());
    }

    /** A wire was made or cut: the networks are rebuilt on the next level tick. */
    void wiresChanged() {
        dirty = true;
    }

    /**
     * The supply poles of this pole's District, itself included: the poles that keep one book (ADR
     * 0008), not every District the Electric Network reaches through its Transformers.
     */
    public List<SupplyAreaPoleBlockEntity> networkOf(SupplyAreaPoleBlockEntity pole) {
        for (Network network : networks) {
            for (List<SupplyAreaPoleBlockEntity> district : network.districts()) {
                if (district.contains(pole)) {
                    return List.copyOf(district);
                }
            }
        }
        return List.of();
    }

    public boolean drawsFrom(BlockPos generator) {
        for (NetworkPole pole : poles.values()) {
            if (pole instanceof SupplyAreaPoleBlockEntity supply && supply.generators().contains(generator)) {
                return true;
            }
        }
        return false;
    }

    public boolean chargesFrom(BlockPos accumulator) {
        for (NetworkPole pole : poles.values()) {
            if (pole instanceof SupplyAreaPoleBlockEntity supply && supply.accumulators().contains(accumulator)) {
                return true;
            }
        }
        return false;
    }

    /** FE the accumulator took last tick, negative when it gave. */
    public long accumulatorFlow(BlockPos accumulator) {
        return accumulatorFlows.getOrDefault(accumulator, 0L);
    }

    /**
     * Forget an unloaded level. A weak key is not enough: the poles held as values point back at
     * their level, so the key would never be collected.
     */
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            BY_LEVEL.remove(level);
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        ElectricNetworks networks = BY_LEVEL.get(level);
        if (networks != null) {
            networks.tick(level);
        }
    }

    private void tick(Level level) {
        long now = level.getGameTime();
        // A report from this tick or the last one counts: block entities tick inside the level tick,
        // and which side of the clock increment they land on is not this class's to assume.
        var stale = lastReport.entrySet().iterator();
        while (stale.hasNext()) {
            var entry = stale.next();
            NetworkPole pole = poles.get(entry.getKey());
            if (entry.getValue() < now - 1 || pole.isRemoved()) {
                poles.remove(entry.getKey());
                stale.remove();
                dirty = true;
            }
        }
        if (dirty) {
            rebuild(level);
            dirty = false;
        }
        Map<BlockPos, Long> flows = new LinkedHashMap<>();
        for (Network network : networks) {
            settle(level, network, flows);
        }
        accumulatorFlows = flows;
    }

    /**
     * Districts and Electric Networks from the stored wires between the poles standing: a District's
     * supply poles settle together, and the Districts a Transformer joins settle as one network.
     */
    private void rebuild(Level level) {
        List<NetworkPole> all = new ArrayList<>(poles.values());
        List<PoleNetworks.Pole> shapes = new ArrayList<>(all.size());
        for (NetworkPole pole : all) {
            shapes.add(pole.shape());
        }
        PoleNetworks.Topology topology = PoleNetworks.topology(shapes,
                LevelWires.of((ServerLevel) level).wires().all());
        List<Network> built = new ArrayList<>();
        List<Map<Integer, List<SupplyAreaPoleBlockEntity>>> byDistrict = new ArrayList<>();
        for (PoleNetworks.ElectricNetwork electric : topology.networks()) {
            Map<Integer, List<SupplyAreaPoleBlockEntity>> districts = new LinkedHashMap<>();
            for (int district : electric.districts()) {
                districts.put(district, new ArrayList<>());
            }
            byDistrict.add(districts);
            // Index-aligned with the districts: the Transformers wired to each.
            List<List<NetworkPole>> joining = new ArrayList<>();
            for (int district : electric.districts()) {
                List<NetworkPole> transformers = new ArrayList<>();
                for (int pole : electric.transformersJoining(district)) {
                    transformers.add(all.get(pole));
                }
                joining.add(transformers);
            }
            built.add(new Network(new ArrayList<>(), new ArrayList<>(districts.values()), joining));
        }
        for (int i = 0; i < all.size(); i++) {
            int network = topology.networkOf()[i];
            built.get(network).members().add(all.get(i));
            if (all.get(i) instanceof SupplyAreaPoleBlockEntity supply) {
                byDistrict.get(network).get(topology.districtOf()[i]).add(supply);
            }
        }
        networks = built;
    }

    /**
     * Settles one Electric Network (ADR 0008): every District's books go through
     * {@link NetworkBalance#settleNetwork}, and the result is applied in one transaction. Receivers
     * first, sources second, and the whole network's tick aborted if a source gives less than its probe
     * promised.
     */
    private static void settle(Level level, Network network, Map<BlockPos, Long> accumulatorFlows) {
        // A block standing in the areas of two Districts is one machine of the network, not two.
        Set<BlockPos> claimed = new LinkedHashSet<>();
        List<Book> books = new ArrayList<>(network.districts().size());
        for (List<SupplyAreaPoleBlockEntity> district : network.districts()) {
            books.add(book(level, district, claimed));
        }
        int n = books.size();
        if (n == 0) {
            return;
        }

        NetworkBalance.District[] districts = new NetworkBalance.District[n];
        long realOffered = 0L;
        for (int d = 0; d < n; d++) {
            Book book = books.get(d);
            // The creative poles lead the generator array as generators with no handler.
            long[] generatorOffers = new long[book.creative + book.generators.size()];
            for (int i = 0; i < book.creative; i++) {
                generatorOffers[i] = CREATIVE_OFFER;
            }
            for (int i = 0; i < book.generators.size(); i++) {
                generatorOffers[book.creative + i] = probeExtract(book.generators.get(i));
                realOffered += generatorOffers[book.creative + i];
            }
            long[] accumulatorOffers = new long[book.accumulators.size()];
            long[] accumulatorRooms = new long[book.accumulators.size()];
            for (int i = 0; i < book.accumulators.size(); i++) {
                accumulatorOffers[i] = probeExtract(book.accumulators.get(i));
                accumulatorRooms[i] = probeInsert(book.accumulators.get(i));
            }
            long[] demands = new long[book.consumers.size()];
            for (int i = 0; i < book.consumers.size(); i++) {
                demands[i] = probeInsert(book.consumers.get(i));
                book.demanded += demands[i];
            }
            districts[d] = new NetworkBalance.District(generatorOffers, accumulatorOffers,
                    accumulatorRooms, demands);
        }

        NetworkBalance.Settlement[] plan = NetworkBalance.settleNetwork(districts);

        // Receivers first, sources second, all in one transaction. What is extracted is exactly
        // what was accepted, so a receiver that takes less than its grant costs its sources the
        // difference rather than destroying it. A source that gives less than its probe promised
        // cannot cover the tick, and the whole tick is aborted rather than creating energy.
        long[] delivered = new long[n];
        long[] produced = new long[n];
        long[] charged = new long[n];
        long[] discharged = new long[n];
        long[][] accumulatorFlow = new long[n][];
        long realProduced = 0L;
        for (int d = 0; d < n; d++) {
            accumulatorFlow[d] = new long[books.get(d).accumulators.size()];
        }
        try (Transaction transaction = Transaction.open(null)) {
            long owed = 0L;
            for (int d = 0; d < n; d++) {
                Book book = books.get(d);
                for (int i = 0; i < book.consumers.size(); i++) {
                    delivered[d] += insert(book.consumers.get(i), plan[d].consumerGrants()[i], transaction);
                }
                for (int i = 0; i < book.accumulators.size(); i++) {
                    accumulatorFlow[d][i] = insert(book.accumulators.get(i),
                            plan[d].accumulatorCharges()[i], transaction);
                    charged[d] += accumulatorFlow[d][i];
                }
                owed += delivered[d] + charged[d];
            }
            for (int d = 0; d < n && owed > 0L; d++) {
                Book book = books.get(d);
                for (int i = 0; i < book.creative && owed > 0L; i++) {
                    long drawn = Math.min(owed, plan[d].generatorDraws()[i]);
                    produced[d] += drawn;
                    owed -= drawn;
                }
                for (int i = 0; i < book.generators.size() && owed > 0L; i++) {
                    long drawn = extract(book.generators.get(i),
                            Math.min(owed, plan[d].generatorDraws()[book.creative + i]), transaction);
                    produced[d] += drawn;
                    realProduced += drawn;
                    owed -= drawn;
                }
            }
            for (int d = 0; d < n && owed > 0L; d++) {
                Book book = books.get(d);
                for (int i = 0; i < book.accumulators.size() && owed > 0L; i++) {
                    long drawn = extract(book.accumulators.get(i),
                            Math.min(owed, plan[d].accumulatorDischarges()[i]), transaction);
                    accumulatorFlow[d][i] -= drawn;
                    discharged[d] += drawn;
                    owed -= drawn;
                }
            }
            if (owed == 0L) {
                transaction.commit();
            } else {
                for (int d = 0; d < n; d++) {
                    delivered[d] = produced[d] = charged[d] = discharged[d] = 0L;
                    accumulatorFlow[d] = new long[books.get(d).accumulators.size()];
                }
                realProduced = 0L;
            }
        }

        // Read after the transaction closes, so an aborted tick reads what is really stored.
        long demanded = 0L;
        long deliveredTotal = 0L;
        long[] supplied = new long[n];
        long[] used = new long[n];
        for (int d = 0; d < n; d++) {
            demanded += books.get(d).demanded;
            deliveredTotal += delivered[d];
            supplied[d] = produced[d] + discharged[d];
            used[d] = delivered[d] + charged[d];
        }
        long surplus = realOffered - realProduced;
        long shortfall = demanded - deliveredTotal;
        NetworkReading[] readings = new NetworkReading[n];
        for (int d = 0; d < n; d++) {
            Book book = books.get(d);
            long stored = 0L;
            long capacity = 0L;
            for (EnergyHandler accumulator : book.accumulators) {
                stored += accumulator.getAmountAsLong();
                capacity += accumulator.getCapacityAsLong();
            }
            for (int i = 0; i < book.accumulators.size(); i++) {
                accumulatorFlows.put(book.accumulatorsAt.get(i), accumulatorFlow[d][i]);
            }
            readings[d] = new NetworkReading(produced[d], delivered[d], book.demanded, charged[d],
                    discharged[d], stored, capacity, book.accumulators.size(), book.poles.size());
            NetworkExchange exchange = NetworkExchange.ofDistrict(supplied[d], used[d], surplus, shortfall);
            for (SupplyAreaPoleBlockEntity pole : book.poles) {
                pole.recordNetworkTick(readings[d], exchange);
            }
        }
        NetworkReading whole = sum(readings, network.members().size());
        NetworkExchange crossed = NetworkExchange.ofNetwork(supplied, used, surplus, shortfall);
        // A Transformer carries its share of each District it joins: the District's exchange, split
        // evenly among the Transformers wired to it.
        Map<NetworkPole, NetworkExchange> carried = new LinkedHashMap<>();
        for (int d = 0; d < n; d++) {
            List<NetworkPole> joining = network.transformers().get(d);
            NetworkExchange share = NetworkExchange.ofDistrict(supplied[d], used[d], surplus, shortfall)
                    .sharedAmong(joining.size());
            for (NetworkPole transformer : joining) {
                carried.merge(transformer, share, NetworkExchange::plus);
            }
        }
        for (NetworkPole member : network.members()) {
            if (!(member instanceof SupplyAreaPoleBlockEntity)) {
                member.recordNetworkTick(whole, carried.getOrDefault(member, crossed));
            }
        }
    }

    /** The Districts' readings added up, for a pole that belongs to the network and to no District. */
    private static NetworkReading sum(NetworkReading[] readings, int poles) {
        long produced = 0L;
        long delivered = 0L;
        long demanded = 0L;
        long charged = 0L;
        long discharged = 0L;
        long stored = 0L;
        long capacity = 0L;
        int accumulators = 0;
        for (NetworkReading r : readings) {
            produced += r.produced();
            delivered += r.delivered();
            demanded += r.demanded();
            charged += r.charged();
            discharged += r.discharged();
            stored += r.stored();
            capacity += r.capacity();
            accumulators += r.accumulators();
        }
        return new NetworkReading(produced, delivered, demanded, charged, discharged, stored, capacity,
                accumulators, poles);
    }

    /** What a District's supply poles found, each block counted once across the whole network. */
    private static Book book(Level level, List<SupplyAreaPoleBlockEntity> poles, Set<BlockPos> claimed) {
        Set<BlockPos> generatorPositions = new LinkedHashSet<>();
        Set<BlockPos> accumulatorPositions = new LinkedHashSet<>();
        Set<BlockPos> consumerPositions = new LinkedHashSet<>();
        int creative = 0;
        for (SupplyAreaPoleBlockEntity pole : poles) {
            claim(pole.generators(), generatorPositions, claimed);
            claim(pole.accumulators(), accumulatorPositions, claimed);
            claim(pole.consumers(), consumerPositions, claimed);
            if (pole.isCreative()) {
                creative++;
            }
        }
        List<BlockPos> accumulatorsAt = new ArrayList<>(accumulatorPositions.size());
        List<EnergyHandler> accumulators = handlers(level, accumulatorPositions, accumulatorsAt);
        return new Book(poles, creative, handlers(level, generatorPositions), accumulatorsAt, accumulators,
                handlers(level, consumerPositions));
    }

    /**
     * Takes the positions no other District has, plus any this District already took through another
     * of its poles.
     */
    private static void claim(List<BlockPos> found, Set<BlockPos> into, Set<BlockPos> claimed) {
        for (BlockPos pos : found) {
            if (into.contains(pos) || claimed.add(pos)) {
                into.add(pos);
            }
        }
    }

    private static List<EnergyHandler> handlers(Level level, Set<BlockPos> positions) {
        return handlers(level, positions, new ArrayList<>());
    }

    /** @param at filled with each found handler's position, index for index */
    private static List<EnergyHandler> handlers(Level level, Set<BlockPos> positions, List<BlockPos> at) {
        List<EnergyHandler> found = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            EnergyHandler handler = SupplyAreaPoleBlockEntity.handler(level, pos);
            if (handler != null) {
                found.add(handler);
                at.add(pos);
            }
        }
        return found;
    }

    private static long probeInsert(EnergyHandler handler) {
        try (Transaction probe = Transaction.open(null)) {
            return handler.insert(Integer.MAX_VALUE, probe);
        }
    }

    private static long probeExtract(EnergyHandler handler) {
        try (Transaction probe = Transaction.open(null)) {
            return handler.extract(Integer.MAX_VALUE, probe);
        }
    }

    private static long insert(EnergyHandler handler, long amount, Transaction transaction) {
        return amount <= 0L ? 0L : handler.insert((int) amount, transaction);
    }

    private static long extract(EnergyHandler handler, long amount, Transaction transaction) {
        return amount <= 0L ? 0L : handler.extract((int) amount, transaction);
    }
}
