// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.SteamGeneratorBlock;
import io.github._5thlayer.wireworks.SteamTurbineBlockEntity;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Steam Turbine: a generator that burns Superheated Steam and nothing else, passes it along a row
 * as the Steam Engine passes Steam, and makes Factorio's rate. The arithmetic is
 * {@code SteamTurbineSpecTest}'s; the row is the Steam Engine's, which {@code SteamEngineTests} holds
 * in full, so here it is held only that the Turbine has it.
 *
 * <p>Each Turbine faces north, so its row runs north to south and its three-block width east to west.
 */
final class SteamTurbineTests {

    /** One Turbine's 2,910 FE/t: 5.82 MW at 100 J per FE. A literal, so the test cannot agree with the spec by construction. */
    private static final long TURBINE_FE_PER_TICK = 2_910L;

    private static final BlockPos ANCHOR = new BlockPos(5, 1, 3);

    /** A row of three, end to end along z. */
    private static final BlockPos[] ROW = {new BlockPos(5, 1, 2), new BlockPos(5, 1, 3), new BlockPos(5, 1, 4)};

    /** The first Turbine's top west part: a facing north footprint's local {@code -z} is west. */
    private static final BlockPos FIRST_TOP_PART = ROW[0].offset(-1, 1, 0);

    private static final int SETTLE = Network.SETTLE;
    private static final int MEASURED = 20;
    /** How long the fan spins on after the last burn, in ticks: a literal second. */
    private static final int RUNNING_HOLD = 20;
    /** One tick of the Turbine's Superheated Steam, 3 mB: a literal. */
    private static final int MILLIBUCKETS_PER_TICK = 3;

    private SteamTurbineTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("fed_steam_turbine_makes_its_rate", 100, SteamTurbineTests::fed);
        tests.test("steam_turbine_draws_every_block_of_its_footprint", 20, helper -> {
            Network.expectDrawn(helper, "a Steam Turbine",
                    Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, ANCHOR, Direction.NORTH));
            helper.succeed();
        });
        tests.test("steam_turbine_row_on_fed_superheated_steam_makes_its_rate_per_turbine", 100,
                SteamTurbineTests::fedRow);
        tests.test("steam_turbine_spins_its_fan_while_it_runs_and_a_second_after", 100, SteamTurbineTests::spinsASecond);
        tests.test("steam_turbine_refuses_steam_and_gives_none_back", 20, SteamTurbineTests::refusesSteam);
        tests.test("steam_turbine_pulls_superheated_steam_from_a_passive_handler_against_its_north_end", 60,
                helper -> pullsFrom(helper, Direction.NORTH));
        tests.test("steam_turbine_pulls_superheated_steam_from_a_passive_handler_against_its_south_end", 60,
                helper -> pullsFrom(helper, Direction.SOUTH));
        tests.test("steam_turbine_row_fed_by_a_passive_handler_shares_the_steam", 100, SteamTurbineTests::pipeFedRow);
        tests.test("steam_engine_refuses_superheated_steam", 20, SteamTurbineTests::engineRefusesSuperheated);
    }

    /** A pipe pushes nothing, so the Turbine pulls: a stand-in against the end opening onto {@code side} empties into it. */
    private static void pullsFrom(GameTestHelper helper, Direction side) {
        Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, ANCHOR, Direction.NORTH);
        PassiveFluid.expectDrawn(helper, ANCHOR.relative(side), superheated(), 40, "the Turbine against its " + side + " end");
    }

    private static void pipeFedRow(GameTestHelper helper) {
        for (BlockPos anchor : ROW) {
            Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, anchor, Direction.NORTH);
        }
        PassiveFluid.stand(helper, ROW[0].north(), superheated(), PassiveFluid.PIPE_CAPACITY, PassiveFluid.PIPE_CAPACITY);
        helper.runAfterDelay(40, () -> {
            for (BlockPos anchor : ROW) {
                if (helper.getBlockEntity(anchor, SteamTurbineBlockEntity.class).energy().getAmountAsLong() <= 0L) {
                    helper.fail("a Turbine of the row made nothing from steam pulled in at its north end", anchor);
                }
            }
            helper.succeed();
        });
    }

    private static FluidResource superheated() {
        return FluidResource.of(SteamFluids.SUPERHEATED_STEAM_SOURCE.get());
    }

    private static FluidResource steam() {
        return FluidResource.of(SteamFluids.STEAM_SOURCE.get());
    }

    private static ResourceHandler<FluidResource> faceAt(GameTestHelper helper, BlockPos block, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(block), side);
    }

    private static int insertAt(GameTestHelper helper, BlockPos block, Direction side, FluidResource fluid, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = faceAt(helper, block, side).insert(fluid, amount, transaction);
            transaction.commit();
            return taken;
        }
    }

    private static void topUpAt(GameTestHelper helper, BlockPos block, Direction side) {
        ResourceHandler<FluidResource> face = faceAt(helper, block, side);
        int room = face.getCapacityAsInt(0, superheated()) - face.getAmountAsInt(0);
        if (room > 0) {
            insertAt(helper, block, side, superheated(), room);
        }
    }

    /** Runs {@code feed} and empties each Turbine's buffer every tick; per Turbine, the FE taken over the measured window. */
    private static long[] drawEachTick(GameTestHelper helper, BlockPos[] turbines, Runnable feed) {
        long[] drawn = new long[turbines.length];
        int[] tick = {0};
        helper.onEachTick(() -> {
            feed.run();
            boolean measured = tick[0] >= SETTLE && tick[0] < SETTLE + MEASURED;
            for (int i = 0; i < turbines.length; i++) {
                try (Transaction transaction = Transaction.openRoot()) {
                    int taken = helper.getBlockEntity(turbines[i], SteamTurbineBlockEntity.class).energy()
                            .extract(Integer.MAX_VALUE, transaction);
                    transaction.commit();
                    if (measured) {
                        drawn[i] += taken;
                    }
                }
            }
            tick[0]++;
        });
        return drawn;
    }

    private static void expectEachMadeItsRate(GameTestHelper helper, BlockPos[] turbines, long[] drawn) {
        for (int i = 0; i < drawn.length; i++) {
            if (drawn[i] != TURBINE_FE_PER_TICK * MEASURED) {
                helper.fail("turbine " + i + " made " + drawn[i] + " FE over " + MEASURED + " ticks, not "
                        + TURBINE_FE_PER_TICK * MEASURED, turbines[i]);
            }
        }
    }

    /** Fed to the brim and drained by the test every tick, a Turbine makes its own 2,910 FE/t. */
    private static void fed(GameTestHelper helper) {
        Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, ANCHOR, Direction.NORTH);
        BlockPos[] one = {ANCHOR};
        long[] drawn = drawEachTick(helper, one, () -> topUpAt(helper, ANCHOR, null));
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> expectEachMadeItsRate(helper, one, drawn))
                .thenSucceed();
    }

    /**
     * Superheated Steam enters the north end of a row of three, at a part block rather than the anchor,
     * and passes south, so each Turbine makes its own rate.
     */
    private static void fedRow(GameTestHelper helper) {
        for (BlockPos anchor : ROW) {
            Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, anchor, Direction.NORTH);
        }
        long[] drawn = drawEachTick(helper, ROW, () -> topUpAt(helper, FIRST_TOP_PART, Direction.NORTH));
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> expectEachMadeItsRate(helper, ROW, drawn))
                .thenSucceed();
    }

    /**
     * The fan on the anchor is still until the Turbine burns, spins while it does, and spins on for a
     * second after the last burn, so one at part load does not flicker, and then stops.
     */
    private static void spinsASecond(GameTestHelper helper) {
        Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, ANCHOR, Direction.NORTH);
        BlockPos[] one = {ANCHOR};
        drawEachTick(helper, one, () -> { });
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    expectRunning(helper, false, "a Turbine that has never burnt");
                    // Ten ticks of steam.
                    insertAt(helper, ANCHOR, null, superheated(), 10 * MILLIBUCKETS_PER_TICK);
                })
                .thenWaitUntil(() -> {
                    if (faceAt(helper, ANCHOR, null).getAmountAsInt(0) > 0) {
                        helper.fail("the Turbine has not burnt its steam yet", ANCHOR);
                    }
                })
                .thenExecute(() -> expectRunning(helper, true, "a Turbine that burnt a tick ago"))
                .thenIdle(RUNNING_HOLD - 3)
                .thenExecute(() -> expectRunning(helper, true, "a Turbine that burnt under a second ago"))
                .thenIdle(5)
                .thenExecute(() -> expectRunning(helper, false, "a Turbine that last burnt over a second ago"))
                .thenSucceed();
    }

    private static void expectRunning(GameTestHelper helper, boolean expected, String which) {
        if (helper.getBlockState(ANCHOR).getValue(SteamGeneratorBlock.RUNNING) != expected) {
            helper.fail(which + (expected ? " shows its fan still" : " shows its fan spinning"), ANCHOR);
        }
    }

    /** Steam, the Boiler's, goes into no Turbine, and a Turbine's superheated steam cannot be drawn back out. */
    private static void refusesSteam(GameTestHelper helper) {
        Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, ANCHOR, Direction.NORTH);
        for (Direction side : new Direction[] {null, Direction.NORTH, Direction.EAST}) {
            if (insertAt(helper, ANCHOR, side, steam(), 100) != 0) {
                helper.fail("the turbine took Steam on side " + side, ANCHOR);
                return;
            }
        }
        if (insertAt(helper, ANCHOR, null, superheated(), 100) != 100) {
            helper.fail("the turbine refused Superheated Steam", ANCHOR);
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (faceAt(helper, ANCHOR, null).extract(superheated(), 10, transaction) != 0) {
                helper.fail("a pipe drew superheated steam back out of the turbine", ANCHOR);
                return;
            }
        }
        helper.succeed();
    }

    private static void engineRefusesSuperheated(GameTestHelper helper) {
        Network.footprint(helper, WireworksRegistries.STEAM_ENGINE_FOOTPRINT, ANCHOR, Direction.NORTH);
        for (Direction side : new Direction[] {null, Direction.NORTH, Direction.EAST}) {
            if (insertAt(helper, ANCHOR, side, superheated(), 100) != 0) {
                helper.fail("the steam engine took Superheated Steam on side " + side, ANCHOR);
                return;
            }
        }
        helper.succeed();
    }
}
