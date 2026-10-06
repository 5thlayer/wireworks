// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * One tick of an Electric Network's books (ADR 0003), in Factorio's order.
 *
 * <ol>
 *   <li>Consumers are fed from generators first.</li>
 *   <li>What generators cannot cover, accumulators discharge to cover.</li>
 *   <li>Generator output nobody consumed charges accumulators. An accumulator never charges from
 *       another accumulator: that would be energy moved in a circle at no one's request.</li>
 * </ol>
 *
 * <p>A shortfall is water-filled across consumers ({@link EnergyShare#waterFill}). Generators and
 * accumulators instead share their part in proportion to what each offered, which is Factorio's
 * rule for generators and keeps one engine from running flat out while its neighbour idles.
 *
 * <p>Energy is conserved exactly: what is drawn from generators and accumulators equals what is
 * granted to consumers and accumulators, to the FE.
 *
 * <p>Pure: no Minecraft types.
 */
public final class NetworkBalance {

    /** Every flow, index-aligned with the arrays handed to {@link #settle}. */
    public record Settlement(long[] generatorDraws, long[] accumulatorDischarges,
                             long[] accumulatorCharges, long[] consumerGrants) {
    }

    /** One District's books: the inputs {@link #settle} takes, for one District. */
    public record District(long[] generatorOffers, long[] accumulatorOffers,
                           long[] accumulatorRooms, long[] consumerDemands) {
    }

    private NetworkBalance() {
    }

    /**
     * @param generatorOffers    FE each generator could give this tick
     * @param accumulatorOffers  FE each accumulator could give this tick
     * @param accumulatorRooms   FE each accumulator could take this tick
     * @param consumerDemands    FE each consumer asked for this tick
     */
    public static Settlement settle(long[] generatorOffers, long[] accumulatorOffers,
                                    long[] accumulatorRooms, long[] consumerDemands) {
        long demand = sum(consumerDemands);
        long generation = sum(generatorOffers);

        long fromGenerators = Math.min(demand, generation);
        long fromAccumulators = Math.min(demand - fromGenerators, sum(accumulatorOffers));
        long charge = Math.min(generation - fromGenerators, sum(accumulatorRooms));

        return new Settlement(
                EnergyShare.proportional(fromGenerators + charge, generatorOffers),
                EnergyShare.proportional(fromAccumulators, accumulatorOffers),
                EnergyShare.proportional(charge, accumulatorRooms),
                EnergyShare.waterFill(fromGenerators + fromAccumulators, consumerDemands));
    }

    /**
     * Settles an Electric Network of Districts in two levels (ADR 0008), with no Minecraft types.
     *
     * <ol>
     *   <li>Each District settles on its own book first ({@link #settle}).</li>
     *   <li>The network meets the Districts' remaining demand from their leftover generator surplus,
     *       then from their accumulators' spare discharge.</li>
     *   <li>Generator surplus still left charges the Districts' remaining accumulator room.</li>
     * </ol>
     *
     * <p>Across Districts, contributions are shared in proportion to what each offers, a shortfall
     * in proportion to each District's shortfall, and charging in proportion to each District's
     * remaining room. Inside a District, what it imports joins its own supply and the total is
     * water-filled across its machines in one pass. A network of one District settles exactly as
     * {@link #settle} does. Energy is conserved to the FE.
     *
     * @param districts each District's offers, rooms and demands
     * @return every flow per District, index-aligned with {@code districts}
     */
    public static Settlement[] settleNetwork(District... districts) {
        int n = districts.length;
        Settlement[] local = new Settlement[n];
        long[] shortfall = new long[n];
        long[] surplus = new long[n];
        long[] spare = new long[n];
        long[] room = new long[n];
        long[][] generatorLeft = new long[n][];
        long[][] accumulatorLeft = new long[n][];
        long[][] roomLeft = new long[n][];
        for (int i = 0; i < n; i++) {
            District d = districts[i];
            Settlement s = settle(d.generatorOffers(), d.accumulatorOffers(), d.accumulatorRooms(),
                    d.consumerDemands());
            local[i] = s;
            generatorLeft[i] = remaining(d.generatorOffers(), s.generatorDraws());
            accumulatorLeft[i] = remaining(d.accumulatorOffers(), s.accumulatorDischarges());
            roomLeft[i] = remaining(d.accumulatorRooms(), s.accumulatorCharges());
            shortfall[i] = sum(d.consumerDemands()) - sum(s.consumerGrants());
            surplus[i] = sum(generatorLeft[i]);
            spare[i] = sum(accumulatorLeft[i]);
            room[i] = sum(roomLeft[i]);
        }

        long needed = sum(shortfall);
        long fromGenerators = Math.min(needed, sum(surplus));
        long fromAccumulators = Math.min(needed - fromGenerators, sum(spare));
        long charge = Math.min(sum(surplus) - fromGenerators, sum(room));

        long[] imports = EnergyShare.proportional(fromGenerators + fromAccumulators, shortfall);
        long[] generatorExports = EnergyShare.proportional(fromGenerators + charge, surplus);
        long[] accumulatorExports = EnergyShare.proportional(fromAccumulators, spare);
        long[] charges = EnergyShare.proportional(charge, room);

        Settlement[] out = new Settlement[n];
        for (int i = 0; i < n; i++) {
            long[] draws = add(local[i].generatorDraws(),
                    EnergyShare.proportional(generatorExports[i], generatorLeft[i]));
            long[] discharges = add(local[i].accumulatorDischarges(),
                    EnergyShare.proportional(accumulatorExports[i], accumulatorLeft[i]));
            long[] charged = add(local[i].accumulatorCharges(),
                    EnergyShare.proportional(charges[i], roomLeft[i]));
            long supply = sum(local[i].consumerGrants()) + imports[i];
            out[i] = new Settlement(draws, discharges, charged,
                    EnergyShare.waterFill(supply, districts[i].consumerDemands()));
        }
        return out;
    }

    private static long[] remaining(long[] limits, long[] used) {
        long[] left = new long[limits.length];
        for (int i = 0; i < left.length; i++) {
            left[i] = Math.max(0L, limits[i]) - used[i];
        }
        return left;
    }

    private static long[] add(long[] a, long[] b) {
        long[] total = new long[a.length];
        for (int i = 0; i < total.length; i++) {
            total[i] = a[i] + b[i];
        }
        return total;
    }

    private static long sum(long[] values) {
        long total = 0L;
        for (long v : values) {
            total += Math.max(0L, v);
        }
        return total;
    }
}
