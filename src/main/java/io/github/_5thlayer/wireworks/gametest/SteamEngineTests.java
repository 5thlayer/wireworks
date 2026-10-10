// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.EnergyOwner;
import io.github._5thlayer.wireworks.NetworkReading;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SteamEngineBlockEntity;
import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;
import io.github._5thlayer.wireworks.SupplyAreaScan;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

/**
 * The Steam Engine on the pole network: a generator a Wireworks pole draws through a part of its
 * footprint alone and once, which burns steam from its own tank and from nothing else, keeps its
 * charge across a reload, and makes Factorio's rate however it is fed. The arithmetic is
 * {@code SteamEngineSpecTest}'s; what is held here is that it survives a running block entity.
 *
 * <p>The engine faces north, so its footprint is the anchor, the block above it, and those two
 * again one west. A small pole stands three west of the anchor, which puts the engine's west column
 * inside its 5x5 area and the anchor outside it: the pole can only reach the engine through a part.
 */
final class SteamEngineTests {

    /** One engine's 450 FE/t: 900 kW at 100 J per FE. A literal, so the test cannot agree with the spec by construction. */
    private static final long ENGINE_FE_PER_TICK = 450L;

    /** FE a millibucket of steam is worth: 450 FE/t over 1.5 mB/t. */
    private static final long FE_PER_MB = 300L;

    private static final BlockPos ANCHOR = new BlockPos(5, 1, 3);
    private static final BlockPos POLE = new BlockPos(2, 1, 3);
    private static final BlockPos CONSUMER = new BlockPos(2, 1, 5);

    private static final int SETTLE = Network.SETTLE;
    private static final int MEASURED = 20;

    private SteamEngineTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("pole_draws_steam_engine_through_a_part_alone", 200, SteamEngineTests::poleDrawsThroughAPart);
        tests.test("steam_engine_keeps_its_charge_across_a_reload", 60, SteamEngineTests::keepsItsCharge);
        tests.test("steam_engine_takes_steam_alone_and_gives_none_back", 60, SteamEngineTests::takesSteamAlone);
        tests.test("steam_engine_answers_the_fluid_capability_on_every_block_with_the_anchors_tank", 20,
                SteamEngineTests::sharesItsTank);
        tests.test("fed_steam_engine_makes_its_rate", 100, SteamEngineTests::fed);
        tests.test("starved_steam_engine_makes_what_its_steam_is_worth", 100, SteamEngineTests::starved);
        tests.test("steam_engine_row_on_fed_steam_makes_its_rate_per_engine", 100, SteamEngineTests::fedRow);
        tests.test("steam_engine_row_fed_from_the_other_end_makes_its_rate_per_engine", 100,
                SteamEngineTests::fedRowFromTheOtherEnd);
        tests.test("steam_engine_row_on_starved_steam_splits_it", 100, SteamEngineTests::starvedRow);
        tests.test("steam_engine_passes_on_nothing_it_took_from_no_end", 60, SteamEngineTests::passesNothingFromNoEnd);
        tests.test("steam_engine_remembers_the_way_steam_passes_across_a_reload", 20,
                SteamEngineTests::remembersTheWayAcrossAReload);
    }

    /** The row runs south from {@code ROW_A}: a facing north engine's local x is south, so ports touch at z. */
    private static final BlockPos[] ROW = {new BlockPos(5, 1, 2), new BlockPos(5, 1, 3), new BlockPos(5, 1, 4)};

    /** Supply to the row each tick of a starved row: over one engine's 1.5 mB, under the 4.5 the three burn. */
    private static final int STARVED_SUPPLY = 2;

    private static BlockPos place(GameTestHelper helper) {
        Network.footprint(helper, WireworksRegistries.STEAM_ENGINE_FOOTPRINT, ANCHOR, Direction.NORTH);
        return helper.absolutePos(ANCHOR);
    }

    private static SteamEngineBlockEntity engineAt(GameTestHelper helper, BlockPos anchor) {
        return helper.getBlockEntity(anchor, SteamEngineBlockEntity.class);
    }

    private static void placeRow(GameTestHelper helper) {
        for (BlockPos anchor : ROW) {
            Network.footprint(helper, WireworksRegistries.STEAM_ENGINE_FOOTPRINT, anchor, Direction.NORTH);
        }
    }

    /** Puts steam into the anchor at {@code anchor} through its face on {@code side}, as a neighbour on that side would. */
    private static int insertAt(GameTestHelper helper, BlockPos anchor, Direction side, int amount) {
        ResourceHandler<FluidResource> face =
                helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(anchor), side);
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = face.insert(steam(), amount, transaction);
            transaction.commit();
            return taken;
        }
    }

    private static void topUpAt(GameTestHelper helper, BlockPos anchor, Direction side) {
        ResourceHandler<FluidResource> face =
                helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(anchor), side);
        int room = face.getCapacityAsInt(0, steam()) - face.getAmountAsInt(0);
        if (room > 0) {
            insertAt(helper, anchor, side, room);
        }
    }

    /** Runs {@code feed} and empties every engine's buffer each tick; per engine, the FE taken over the measured window. */
    private static long[] drawEachTick(GameTestHelper helper, Runnable feed) {
        long[] drawn = new long[ROW.length];
        int[] tick = {0};
        helper.onEachTick(() -> {
            feed.run();
            boolean measured = tick[0] >= SETTLE && tick[0] < SETTLE + MEASURED;
            for (int i = 0; i < ROW.length; i++) {
                try (Transaction transaction = Transaction.openRoot()) {
                    int taken = engineAt(helper, ROW[i]).energy().extract(Integer.MAX_VALUE, transaction);
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

    private static void expectEachMadeItsRate(GameTestHelper helper, long[] drawn) {
        for (int i = 0; i < drawn.length; i++) {
            if (drawn[i] != ENGINE_FE_PER_TICK * MEASURED) {
                helper.fail("engine " + i + " of the row made " + drawn[i] + " FE over " + MEASURED
                        + " ticks, not " + ENGINE_FE_PER_TICK * MEASURED, ROW[i]);
            }
        }
    }

    private static SteamEngineBlockEntity engine(GameTestHelper helper) {
        return helper.getBlockEntity(ANCHOR, SteamEngineBlockEntity.class);
    }

    private static FluidResource steam() {
        return FluidResource.of(SteamFluids.STEAM_SOURCE.get());
    }

    private static ResourceHandler<FluidResource> steamFace(GameTestHelper helper) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(ANCHOR), null);
    }

    private static int insert(GameTestHelper helper, FluidResource fluid, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = steamFace(helper).insert(fluid, amount, transaction);
            transaction.commit();
            return taken;
        }
    }

    /** Keeps the tank full. */
    private static void topUp(GameTestHelper helper) {
        ResourceHandler<FluidResource> face = steamFace(helper);
        int room = face.getCapacityAsInt(0, steam()) - face.getAmountAsInt(0);
        if (room > 0) {
            insert(helper, steam(), room);
        }
    }

    /** Takes every FE the engine holds, as a charger would, and returns how many. */
    private static int drain(GameTestHelper helper) {
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = engine(helper).energy().extract(Integer.MAX_VALUE, transaction);
            transaction.commit();
            return taken;
        }
    }

    private static void poleDrawsThroughAPart(GameTestHelper helper) {
        BlockPos anchor = place(helper);
        Network.small(helper, POLE);
        Network.consumer(helper, CONSUMER);

        List<BlockPos> generators = SupplyAreaScan.of(helper.getLevel(), helper.absolutePos(POLE), PoleTier.SMALL).generators();
        if (!generators.equals(List.of(anchor))) {
            helper.fail("the pole finds generators " + generators + " where the engine's anchor is the one", POLE);
        }
        for (BlockPos block : WireworksRegistries.STEAM_ENGINE_FOOTPRINT.positions(anchor, Direction.NORTH)) {
            if (!block.equals(anchor) && !anchor.equals(EnergyOwner.of(helper.getLevel(), block))) {
                helper.fail("a block of the engine answers for " + EnergyOwner.of(helper.getLevel(), block)
                        + ", not its anchor", ANCHOR);
            }
        }

        long[] produced = {0L};
        helper.onEachTick(() -> {
            topUp(helper);
            Network.drain(helper, CONSUMER);
        });
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecuteFor(MEASURED, () -> {
                    SupplyAreaPoleBlockEntity at = helper.getBlockEntity(POLE, SupplyAreaPoleBlockEntity.class);
                    NetworkReading reading = at.networkReading();
                    if (at.machineCount() != 1) {
                        helper.fail("the pole files " + at.machineCount() + " consumers; only the test consumer"
                                + " is one, so an engine block is being fed", POLE);
                    }
                    if (reading.produced() != ENGINE_FE_PER_TICK) {
                        helper.fail("the pole drew " + reading.produced() + " FE from an engine making "
                                + ENGINE_FE_PER_TICK + " a tick, so it drew through a part wrongly or twice", POLE);
                    }
                    produced[0] += reading.produced();
                })
                .thenExecute(() -> {
                    if (produced[0] != ENGINE_FE_PER_TICK * MEASURED) {
                        helper.fail("drew " + produced[0] + " FE over " + MEASURED + " ticks", POLE);
                    }
                })
                .thenSucceed();
    }

    /** The charge, the owed fractions and the steam are saved, so a reload neither loses nor invents power. */
    private static void keepsItsCharge(GameTestHelper helper) {
        place(helper);
        helper.onEachTick(() -> topUp(helper));
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    SteamEngineBlockEntity engine = engine(helper);
                    CompoundTag saved = engine.saveWithFullMetadata(helper.getLevel().registryAccess());
                    BlockEntity loaded = BlockEntity.loadStatic(engine.getBlockPos(), engine.getBlockState(),
                            saved, helper.getLevel().registryAccess());
                    if (!(loaded instanceof SteamEngineBlockEntity reloaded)) {
                        helper.fail("the saved Steam Engine reloaded as " + loaded, ANCHOR);
                        return;
                    }
                    long held = engine.energy().getAmountAsLong();
                    long kept = reloaded.energy().getAmountAsLong();
                    if (held <= 0L) {
                        helper.fail("the engine held no charge to save", ANCHOR);
                    }
                    if (kept != held) {
                        helper.fail("the engine saved " + held + " FE and reloaded " + kept, ANCHOR);
                    }
                    CompoundTag again = reloaded.saveWithFullMetadata(helper.getLevel().registryAccess());
                    for (String key : new String[] {"CarrySteam", "CarryEnergy", "Steam"}) {
                        if (!saved.contains(key)) {
                            helper.fail("the engine saved no " + key, ANCHOR);
                        }
                        double before = saved.getDoubleOr(key, Double.NaN);
                        double after = again.getDoubleOr(key, Double.NaN);
                        if (before != after) {
                            helper.fail("the engine saved " + key + " " + before + " and reloaded " + after, ANCHOR);
                        }
                    }
                })
                .thenSucceed();
    }

    /** A tank of water makes no power, steam does, and the face gives nothing back to a pipe. */
    private static void takesSteamAlone(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    if (insert(helper, FluidResource.of(Fluids.WATER), 100) != 0) {
                        helper.fail("the engine took water into its steam tank", ANCHOR);
                    }
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    if (engine(helper).energy().getAmountAsLong() != 0L) {
                        helper.fail("the engine made power from no steam", ANCHOR);
                    }
                    topUp(helper);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    long held = engine(helper).energy().getAmountAsLong();
                    if (held != ENGINE_FE_PER_TICK) {
                        helper.fail("the engine holds " + held + " FE, not a full buffer of " + ENGINE_FE_PER_TICK, ANCHOR);
                    }
                    try (Transaction transaction = Transaction.openRoot()) {
                        if (steamFace(helper).extract(steam(), 10, transaction) != 0) {
                            helper.fail("a pipe drew steam back out of the engine", ANCHOR);
                        }
                    }
                })
                .thenSucceed();
    }

    private static void sharesItsTank(GameTestHelper helper) {
        BlockPos anchor = place(helper);
        ResourceHandler<FluidResource> tank = steamFace(helper);
        if (tank == null) {
            helper.fail("the anchor answers no fluid capability", ANCHOR);
            return;
        }
        if (tank.getCapacityAsInt(0, steam()) != 200) {
            helper.fail("the steam tank holds " + tank.getCapacityAsInt(0, steam()) + " mB, not 200", ANCHOR);
        }
        for (BlockPos pos : WireworksRegistries.STEAM_ENGINE_FOOTPRINT.positions(anchor, Direction.NORTH)) {
            for (Direction side : new Direction[] {null, Direction.UP, Direction.EAST}) {
                ResourceHandler<FluidResource> there = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, side);
                if (there != tank) {
                    helper.fail("the block at " + pos + " answers " + there + " on side " + side
                            + ", not the anchor's tank", ANCHOR);
                }
            }
        }
        for (BlockPos pos : WireworksRegistries.STEAM_ENGINE_FOOTPRINT.positions(anchor, Direction.NORTH)) {
            for (Direction end : new Direction[] {Direction.NORTH, Direction.SOUTH}) {
                ResourceHandler<FluidResource> there = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, end);
                if (there == null || there.getCapacityAsInt(0, steam()) != 200) {
                    helper.fail("the block at " + pos + " answers " + there + " at the row end " + end
                            + ", not a 200 mB steam tank", ANCHOR);
                    return;
                }
            }
        }
        insertAt(helper, ANCHOR, Direction.NORTH, 50);
        if (tank.getAmountAsInt(0) != 50) {
            helper.fail("steam put in at the row end is not in the anchor's tank: " + tank.getAmountAsInt(0), ANCHOR);
        }
        helper.succeed();
    }

    /** Fed to the brim and drained by the test every tick, an engine makes its own 450 FE/t. */
    private static void fed(GameTestHelper helper) {
        place(helper);
        long[] drawn = {0L};
        int[] tick = {0};
        helper.onEachTick(() -> {
            topUp(helper);
            int taken = drain(helper);
            if (tick[0] >= SETTLE && tick[0] < SETTLE + MEASURED) {
                drawn[0] += taken;
            }
            tick[0]++;
        });
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> {
                    if (drawn[0] != ENGINE_FE_PER_TICK * MEASURED) {
                        helper.fail("the engine made " + drawn[0] + " FE over " + MEASURED + " ticks, not "
                                + ENGINE_FE_PER_TICK * MEASURED, ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** Fed under what it burns, an engine makes what the steam is worth, give or take a millibucket and a buffer. */
    private static void starved(GameTestHelper helper) {
        place(helper);
        int supply = 1;
        long[] drawn = {0L};
        int[] tick = {0};
        helper.onEachTick(() -> {
            insert(helper, steam(), supply);
            int taken = drain(helper);
            if (tick[0] >= SETTLE && tick[0] < SETTLE + MEASURED) {
                drawn[0] += taken;
            }
            tick[0]++;
        });
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> {
                    long worth = supply * FE_PER_MB * MEASURED;
                    long slack = ENGINE_FE_PER_TICK + FE_PER_MB;
                    if (drawn[0] > ENGINE_FE_PER_TICK * MEASURED || Math.abs(drawn[0] - worth) > slack) {
                        helper.fail("the engine made " + drawn[0] + " FE over " + MEASURED + " ticks from steam worth "
                                + worth, ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** Steam enters the north end of a row of three and passes south, so each engine makes its own 450 FE/t. */
    private static void fedRow(GameTestHelper helper) {
        placeRow(helper);
        long[] drawn = drawEachTick(helper, () -> topUpAt(helper, ROW[0], Direction.NORTH));
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> expectEachMadeItsRate(helper, drawn))
                .thenSucceed();
    }

    /** Which end the steam comes in at decides which way it passes. */
    private static void fedRowFromTheOtherEnd(GameTestHelper helper) {
        placeRow(helper);
        long[] drawn = drawEachTick(helper, () -> topUpAt(helper, ROW[2], Direction.SOUTH));
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> expectEachMadeItsRate(helper, drawn))
                .thenSucceed();
    }

    /**
     * A row fed less than it burns shares what arrives: none passes its rate, the engine the steam
     * reaches first has the most, and together they make what the steam is worth, give or take a
     * millibucket and a buffer each.
     */
    private static void starvedRow(GameTestHelper helper) {
        placeRow(helper);
        long[] drawn = drawEachTick(helper, () -> insertAt(helper, ROW[0], Direction.NORTH, STARVED_SUPPLY));
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> {
                    long total = 0L;
                    for (int i = 0; i < drawn.length; i++) {
                        if (drawn[i] > ENGINE_FE_PER_TICK * MEASURED) {
                            helper.fail("engine " + i + " made " + drawn[i] + " FE over " + MEASURED
                                    + " ticks, past its " + ENGINE_FE_PER_TICK * MEASURED, ROW[i]);
                        }
                        total += drawn[i];
                    }
                    if (drawn[0] < drawn[2]) {
                        helper.fail("the engine the steam reaches last made " + drawn[2]
                                + " FE, more than the first's " + drawn[0], ROW[2]);
                    }
                    long worth = STARVED_SUPPLY * FE_PER_MB * MEASURED;
                    long slack = ROW.length * (ENGINE_FE_PER_TICK + FE_PER_MB);
                    if (Math.abs(total - worth) > slack) {
                        helper.fail("the row made " + total + " FE over " + MEASURED + " ticks from steam worth "
                                + worth, ROW[0]);
                    }
                })
                .thenSucceed();
    }

    /** Steam put in with no end named, or at the side, fills that engine alone and is passed to no neighbour. */
    private static void passesNothingFromNoEnd(GameTestHelper helper) {
        placeRow(helper);
        helper.onEachTick(() -> {
            topUpAt(helper, ROW[1], null);
            topUpAt(helper, ROW[1], Direction.EAST);
        });
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    for (int i : new int[] {0, 2}) {
                        int held = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(ROW[i]), null).getAmountAsInt(0);
                        if (held != 0) {
                            helper.fail("the engine beside the fed one holds " + held + " mB of steam that no row end gave", ROW[i]);
                        }
                    }
                })
                .thenSucceed();
    }

    private static void remembersTheWayAcrossAReload(GameTestHelper helper) {
        placeRow(helper);
        insertAt(helper, ROW[0], Direction.NORTH, 10);
        SteamEngineBlockEntity engine = engineAt(helper, ROW[0]);
        CompoundTag saved = engine.saveWithFullMetadata(helper.getLevel().registryAccess());
        if (!"south".equals(saved.getStringOr("PassOn", ""))) {
            helper.fail("steam that came in at the north end is saved as passing " + saved.getStringOr("PassOn", "nowhere"), ROW[0]);
            return;
        }
        BlockEntity loaded = BlockEntity.loadStatic(engine.getBlockPos(), engine.getBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof SteamEngineBlockEntity reloaded)
                || !"south".equals(reloaded.saveWithFullMetadata(helper.getLevel().registryAccess())
                        .getStringOr("PassOn", ""))) {
            helper.fail("the engine did not keep the way its steam passes across a reload", ROW[0]);
            return;
        }
        helper.succeed();
    }
}
