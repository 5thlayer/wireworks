// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.wireworks.DayFraction;
import io.github._5thlayer.wireworks.NetworkReading;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;
import io.github._5thlayer.wireworks.SupplyAreaScan;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * The Solar Panel on the pole network: 30 FE/t drawn by a pole at noon, none at midnight and none
 * with a block one above its top layer's centre, and one panel however many of its blocks a pole
 * reaches. The 30 is typed, not read off the spec, so the test cannot agree with the spec by
 * construction.
 *
 * <p>The panel's pillar stands at x 5 under a top layer spanning x 4-6. A small pole's 5x5 area at
 * x 7 holds the pillar and a column of the top layer; at x 8 it holds only the top layer's x 6. A
 * consumer in reach, emptied every tick, always asks for more than the panel makes.
 */
final class SolarPanelTests {

    private static final long FE_PER_TICK = 30L;

    /** Fractions of the clock's day: Minecraft's noon is a quarter in, and its midnight three quarters. */
    private static final double NOON = 0.25;
    private static final double MIDNIGHT = 0.75;
    private static final BlockPos ANCHOR = new BlockPos(5, 1, 3);
    private static final BlockPos TOP_CENTRE = ANCHOR.above();

    private static final int SETTLE = Network.SETTLE;
    private static final int MEASURED = 20;

    /**
     * The tests share the world's one clock and a batch runs at once, so each holds the clock in a
     * slot of its own: from its start, past settling and measuring.
     */
    private static final int SLOT = SETTLE + MEASURED + 10;
    private static final int TIMEOUT = 4 * SLOT + SETTLE + MEASURED + 20;

    private SolarPanelTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("solar_panel_makes_30_fe_per_tick_at_noon", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(7, 1, 3), 0, NOON, false, FE_PER_TICK));
        tests.test("solar_panel_makes_nothing_at_midnight", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(7, 1, 3), 1, MIDNIGHT, false, 0L));
        tests.test("solar_panel_makes_nothing_at_noon_with_a_block_above_its_top_centre", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(7, 1, 3), 2, NOON, true, 0L));
        tests.test("pole_draws_a_solar_panel_through_a_part_alone", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(8, 1, 3), 3, NOON, false, FE_PER_TICK));
    }

    private static void drawsFrom(GameTestHelper helper, BlockPos pole, int slot, double clockFraction,
            boolean roof, long fePerTick) {
        helper.startSequence()
                .thenIdle(slot * SLOT)
                .thenExecute(() -> drawsFromNow(helper, pole, clockFraction, roof, fePerTick));
    }

    private static void drawsFromNow(GameTestHelper helper, BlockPos pole, double clockFraction, boolean roof,
            long fePerTick) {
        setDay(helper, clockFraction);
        if (roof) {
            helper.setBlock(TOP_CENTRE.above(), Blocks.STONE);
        }
        Network.footprint(helper, WireworksRegistries.SOLAR_PANEL_FOOTPRINT, ANCHOR, Direction.NORTH);
        BlockPos consumer = pole.offset(0, 0, 2);
        Network.small(helper, pole);
        Network.consumer(helper, consumer);

        List<BlockPos> generators = SupplyAreaScan.of(helper.getLevel(), helper.absolutePos(pole), PoleTier.SMALL).generators();
        if (!generators.equals(List.of(helper.absolutePos(ANCHOR)))) {
            helper.fail("the pole finds generators " + generators + " where the panel's anchor is the one", pole);
        }

        long[] produced = {0L};
        helper.onEachTick(() -> Network.drain(helper, consumer));
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecuteFor(MEASURED, () -> {
                    SupplyAreaPoleBlockEntity at = helper.getBlockEntity(pole, SupplyAreaPoleBlockEntity.class);
                    NetworkReading reading = at.networkReading();
                    if (at.machineCount() != 1) {
                        helper.fail("the pole files " + at.machineCount() + " consumers; only the test consumer"
                                + " is one, so a panel block is being fed", pole);
                    }
                    if (reading.produced() != fePerTick) {
                        helper.fail("the pole drew " + reading.produced() + " FE from a panel making "
                                + fePerTick + " a tick", pole);
                    }
                    produced[0] += reading.produced();
                })
                .thenExecute(() -> {
                    if (produced[0] != fePerTick * MEASURED) {
                        helper.fail("drew " + produced[0] + " FE over " + MEASURED + " ticks", pole);
                    }
                })
                .thenExecute(() -> endDay(helper))
                .thenSucceed();
    }

    /** Holds the clock at a fraction of the dimension's own day, whatever its length. */
    private static void setDay(GameTestHelper helper, double clockFraction) {
        ServerLevel level = helper.getLevel();
        var clock = level.dimensionType().defaultClock().orElseThrow();
        int period = DayFraction.periodTicks(level);
        level.clockManager().setPaused(clock, true);
        level.clockManager().setTotalTicks(clock, Math.round(clockFraction * period));
    }

    private static void endDay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.clockManager().setPaused(level.dimensionType().defaultClock().orElseThrow(), false);
    }
}
