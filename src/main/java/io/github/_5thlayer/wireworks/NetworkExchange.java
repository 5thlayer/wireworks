// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

/**
 * What crossed between Districts through their Transformers in one tick (ADR 0008), and what the
 * Electric Network had left over or lacked: what a Jade line reads next to a {@link NetworkReading}.
 *
 * <p>Like a reading, every figure is what the tick <em>committed</em>, so an aborted tick reads as
 * nothing crossed. Pure: no Minecraft types.
 *
 * @param exported  FE a District's sources gave beyond its own machines and accumulators; on the
 *                  network side, everything that crossed
 * @param imported  FE a District's machines and accumulators took beyond its own sources; on the
 *                  network side, everything that crossed
 * @param surplus   FE the network's generators offered and nobody took
 * @param shortfall FE the network's machines asked for and did not get
 */
public record NetworkExchange(long exported, long imported, long surplus, long shortfall) {

    public static final NetworkExchange NONE = new NetworkExchange(0L, 0L, 0L, 0L);

    /** A District that supplied {@code supplied} FE and used {@code used}: the difference crossed out or in. */
    public static NetworkExchange ofDistrict(long supplied, long used, long surplus, long shortfall) {
        return new NetworkExchange(Math.max(0L, supplied - used), Math.max(0L, used - supplied), surplus, shortfall);
    }

    /** The whole network, from each District's supplied and used FE, index-aligned: what crossed, counted once. */
    public static NetworkExchange ofNetwork(long[] supplied, long[] used, long surplus, long shortfall) {
        long out = 0L;
        long in = 0L;
        for (int i = 0; i < supplied.length; i++) {
            out += Math.max(0L, supplied[i] - used[i]);
            in += Math.max(0L, used[i] - supplied[i]);
        }
        return new NetworkExchange(out, in, surplus, shortfall);
    }

    /**
     * One of {@code transformers} Transformers joining a District: they share its exchange evenly,
     * and the network's surplus and shortfall are the same for each.
     */
    public NetworkExchange sharedAmong(int transformers) {
        int n = Math.max(1, transformers);
        return new NetworkExchange(exported / n, imported / n, surplus, shortfall);
    }

    /**
     * What crosses a Transformer that joins two Districts: its shares added, the two directions
     * netted into one. Surplus and shortfall are the network's, so this exchange's are kept.
     */
    public NetworkExchange plus(NetworkExchange other) {
        long net = (exported - imported) + (other.exported - other.imported);
        return new NetworkExchange(Math.max(0L, net), Math.max(0L, -net), surplus, shortfall);
    }
}
