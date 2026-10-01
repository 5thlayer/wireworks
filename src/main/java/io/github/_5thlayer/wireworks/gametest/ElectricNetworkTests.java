// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;


import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * Whether power crosses a wire (#280, ADR-0062).
 *
 * <p>{@code PoleLinksTest} holds which poles link and {@code NetworkBalanceTest} what one network
 * pays out. Neither can see that the level actually settles linked poles as one set of books:
 * that a pole reports to the network, that the network is rebuilt when a pole arrives or leaves,
 * and that the settle tick is listened for at all.
 *
 * <h2>The layout</h2>
 *
 * <p>A creative pole at x 1 -- an unlimited generator whose 18x18 area reaches x 9 -- and an
 * consumer at x 10, outside it. A small pole between them covers the consumer. Seven blocks
 * from the creative pole the small pole is within its 7.5 reach, and eight blocks away it is not.
 * The consumer can only be fed across the wire.
 */
final class ElectricNetworkTests {

    private static final BlockPos CREATIVE = new BlockPos(1, 1, 3);
    private static final BlockPos LINKED = new BlockPos(8, 1, 3);
    private static final BlockPos UNLINKED = new BlockPos(9, 1, 3);
    private static final BlockPos CONSUMER = new BlockPos(10, 1, 3);

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    private static final int SETTLE = Network.SETTLE;

    private ElectricNetworkTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("power_crosses_a_wire", 100, ElectricNetworkTests::powerCrossesAWire);
        tests.test("power_stops_beyond_reach", 100, ElectricNetworkTests::powerStopsBeyondReach);
        tests.test("breaking_the_link_splits_the_network", 200,
                ElectricNetworkTests::breakingTheLinkSplits);
    }

    private static void powerCrossesAWire(GameTestHelper helper) {
        place(helper, LINKED);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    if (stored(helper) <= 0L) {
                        helper.fail("a consumer on a pole wired to a creative pole holds no FE",
                                CONSUMER);
                    }
                })
                .thenSucceed();
    }

    private static void powerStopsBeyondReach(GameTestHelper helper) {
        place(helper, UNLINKED);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    long stored = stored(helper);
                    if (stored != 0L) {
                        helper.fail("a consumer on a pole eight blocks from the creative pole holds "
                                + stored + " FE; a small pole reaches 7.5", CONSUMER);
                    }
                })
                .thenSucceed();
    }

    /**
     * The split half of "no stored topology": a network that kept its poles after one was broken
     * would keep feeding a consumer nothing is wired to.
     */
    private static void breakingTheLinkSplits(GameTestHelper helper) {
        place(helper, LINKED);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    helper.setBlock(CREATIVE, Blocks.AIR);
                    Network.drain(helper, CONSUMER);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    long stored = stored(helper);
                    if (stored != 0L) {
                        helper.fail("the consumer took " + stored + " FE after the creative pole"
                                + " was broken", CONSUMER);
                    }
                })
                .thenSucceed();
    }

    private static void place(GameTestHelper helper, BlockPos smallPole) {
        Network.creative(helper, CREATIVE);
        Network.small(helper, smallPole);
        Network.consumer(helper, CONSUMER);
    }

    private static long stored(GameTestHelper helper) {
        return Network.stored(helper, CONSUMER);
    }
}
