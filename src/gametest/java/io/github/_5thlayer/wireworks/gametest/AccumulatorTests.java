// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.wireworks.NetworkReading;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;
import io.github._5thlayer.wireworks.SupplyAreaScan;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Accumulator on the pole network: charged from a generator surplus and discharged to cover a
 * shortfall, at no more than 150 FE/t and to no more than 50,000 FE, and counted once by a pole
 * whose area covers all four of its blocks. The limits are typed, not read off the spec.
 *
 * <p>The accumulator faces north, so it covers its anchor and the blocks east and south of it. The creative pole's area is the large pole's, 16 wide.
 */
final class AccumulatorTests {

    private static final long FLOW = 150L;
    private static final long CAPACITY = 50_000L;

    private static final BlockPos ANCHOR = new BlockPos(9, 1, 3);
    private static final BlockPos CREATIVE = new BlockPos(3, 1, 3);
    private static final BlockPos POLE = new BlockPos(11, 1, 3);
    private static final BlockPos CONSUMER = new BlockPos(11, 1, 5);

    private static final int MEASURED = 20;

    private AccumulatorTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("accumulator_charges_from_a_generator_surplus_at_150_fe_per_tick", 100,
                AccumulatorTests::chargesFromASurplus);
        tests.test("accumulator_discharges_to_cover_a_shortfall_at_150_fe_per_tick", 100,
                AccumulatorTests::dischargesToCoverAShortfall);
        tests.test("accumulator_holds_no_more_than_50000_fe_and_moves_no_more_than_150_per_call", 100,
                AccumulatorTests::stopsAtItsLimits);
        tests.test("pole_counts_an_accumulator_once_however_many_of_its_blocks_it_covers", 100,
                AccumulatorTests::countedOnce);
    }

    private static void chargesFromASurplus(GameTestHelper helper) {
        accumulator(helper);
        Network.creative(helper, CREATIVE);
        long[] before = {-1L};
        helper.startSequence()
                .thenIdle(Network.SETTLE)
                .thenExecuteFor(MEASURED, () -> {
                    long now = stored(helper);
                    if (before[0] >= 0L && now - before[0] != FLOW) {
                        helper.fail("the accumulator took " + (now - before[0]) + " FE in a tick, not " + FLOW
                                + " from an unlimited surplus", ANCHOR);
                    }
                    before[0] = now;
                })
                .thenExecute(() -> {
                    if (before[0] <= 0L) {
                        helper.fail("the accumulator holds " + before[0] + " FE after a surplus", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** No generator, a consumer asking for more than the accumulator gives: the whole flow goes to it. */
    private static void dischargesToCoverAShortfall(GameTestHelper helper) {
        accumulator(helper);
        fill(helper, 10_000L);
        Network.small(helper, POLE);
        Network.consumer(helper, CONSUMER);
        long[] before = {-1L};
        helper.onEachTick(() -> Network.drain(helper, CONSUMER));
        helper.startSequence()
                .thenIdle(Network.SETTLE)
                .thenExecuteFor(MEASURED, () -> {
                    long now = stored(helper);
                    NetworkReading reading = helper.getBlockEntity(POLE, SupplyAreaPoleBlockEntity.class).networkReading();
                    if (before[0] >= 0L && before[0] - now != FLOW) {
                        helper.fail("the accumulator gave " + (before[0] - now) + " FE in a tick, not " + FLOW, ANCHOR);
                    }
                    if (reading.delivered() != FLOW || reading.discharged() != FLOW) {
                        helper.fail("the consumer was delivered " + reading.delivered() + " FE of a discharge of "
                                + reading.discharged() + ", not " + FLOW, POLE);
                    }
                    before[0] = now;
                })
                .thenSucceed();
    }

    /** Filled by hand and then left under a creative pole, which offers more than it could ever take. */
    private static void stopsAtItsLimits(GameTestHelper helper) {
        accumulator(helper);
        EnergyHandler face = Network.energy(helper, ANCHOR);
        long held = 0L;
        for (int calls = 0; calls < 1000; calls++) {
            int inserted;
            try (Transaction transaction = Transaction.open(null)) {
                inserted = face.insert(Integer.MAX_VALUE, transaction);
                transaction.commit();
            }
            if (inserted > FLOW) {
                helper.fail("one insert took " + inserted + " FE, not at most " + FLOW, ANCHOR);
            }
            if (inserted == 0) {
                break;
            }
            held += inserted;
        }
        if (held != CAPACITY || stored(helper) != CAPACITY) {
            helper.fail("the accumulator took " + held + " FE and holds " + stored(helper) + ", not " + CAPACITY, ANCHOR);
        }
        try (Transaction transaction = Transaction.open(null)) {
            int extracted = face.extract(Integer.MAX_VALUE, transaction);
            if (extracted != FLOW) {
                helper.fail("one extract gave " + extracted + " FE, not " + FLOW, ANCHOR);
            }
        }
        Network.creative(helper, CREATIVE);
        helper.startSequence()
                .thenIdle(Network.SETTLE)
                .thenExecuteFor(MEASURED, () -> {
                    if (stored(helper) != CAPACITY) {
                        helper.fail("a full accumulator under a surplus holds " + stored(helper) + " FE, not " + CAPACITY, ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** One buffer's charge, not four: the pole's books name one accumulator holding what it holds. */
    private static void countedOnce(GameTestHelper helper) {
        List<BlockPos> blocks = accumulator(helper);
        fill(helper, 10_000L);
        Network.small(helper, POLE);
        List<BlockPos> covered = SupplyAreaScan.of(helper.getLevel(), helper.absolutePos(POLE), PoleTier.SMALL).accumulators();
        if (!covered.equals(List.of(helper.absolutePos(ANCHOR)))) {
            helper.fail("the pole files accumulators " + covered + " where the anchor is the one", POLE);
        }
        BlockPos pole = helper.absolutePos(POLE);
        for (BlockPos block : blocks) {
            if (Math.abs(block.getX() - pole.getX()) > PoleTier.SMALL.maxOffset()
                    || Math.abs(block.getZ() - pole.getZ()) > PoleTier.SMALL.maxOffset()) {
                helper.fail("the accumulator block at " + block + " lies outside the pole's area, so the test proves nothing", POLE);
            }
        }
        helper.startSequence()
                .thenIdle(Network.SETTLE)
                .thenExecute(() -> {
                    NetworkReading reading = helper.getBlockEntity(POLE, SupplyAreaPoleBlockEntity.class).networkReading();
                    if (reading.accumulators() != 1 || reading.stored() != 10_000L || reading.capacity() != CAPACITY) {
                        helper.fail("the pole's network reads " + reading.accumulators() + " accumulators holding "
                                + reading.stored() + " of " + reading.capacity() + " FE; one holding 10000 of "
                                + CAPACITY + " was placed", POLE);
                    }
                    if (helper.getBlockEntity(POLE, SupplyAreaPoleBlockEntity.class).machineCount() != 0) {
                        helper.fail("the pole files an accumulator block as a consumer", POLE);
                    }
                })
                .thenSucceed();
    }

    private static List<BlockPos> accumulator(GameTestHelper helper) {
        return Network.footprint(helper, WireworksRegistries.ACCUMULATOR_FOOTPRINT, ANCHOR, Direction.NORTH);
    }

    private static void fill(GameTestHelper helper, long amount) {
        if (!Network.fill(Network.energy(helper, ANCHOR), amount)) {
            helper.fail("the accumulator took less than " + amount + " FE", ANCHOR);
        }
    }

    private static long stored(GameTestHelper helper) {
        return Network.energy(helper, ANCHOR).getAmountAsLong();
    }
}
