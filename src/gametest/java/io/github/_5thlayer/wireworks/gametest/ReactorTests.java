// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.libworks.ghost.Ghosts;
import io.github._5thlayer.wireworks.ReactorBlockEntity;
import io.github._5thlayer.wireworks.ReactorMenu;
import io.github._5thlayer.wireworks.ReactorSlots;
import io.github._5thlayer.wireworks.ReactorStatus;
import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.SteamTurbineBlockEntity;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Reactor in a world (#40): it turns water into Superheated Steam at 21 mB/t, enough for seven
 * Turbines, burns a Fuel Cell for 4,000 ticks of running and nothing while it lacks water or room, and
 * leaves a Depleted Fuel Cell where a full depleted slot stops it. Its ports open onto their outer
 * faces, and its two slots are reached from every block. The numbers are {@code ReactorSpecTest}'s and
 * the stop {@code ReactorCycleTest}'s; what is asserted here is that they survive a running block entity.
 *
 * <p>The Reactor's anchor is its bottom centre, so its water ports are one block off the anchor along
 * each horizontal axis. It faces west, so its steam port, the back middle of its middle layer, opens
 * east, along the platform's long side.
 */
final class ReactorTests {

    private static final BlockPos ANCHOR = new BlockPos(2, 1, 3);
    private static final Direction FACING = Direction.WEST;

    /** What a whole tick converts, in millibuckets. A literal, so the test cannot agree with the spec by construction. */
    private static final int MILLIBUCKETS_PER_TICK = 21;
    private static final long TURBINE_FE_PER_TICK = 2_910L;
    private static final int TURBINES = 7;
    private static final int CELL_TICKS = 4_000;
    private static final int TANK_CAPACITY = 10_000;

    private static final BlockPos WEST_WATER = ANCHOR.west();
    private static final BlockPos STEAM_PORT = ANCHOR.offset(1, 1, 0);
    private static final Direction STEAM_SIDE = Direction.EAST;

    private static final int WINDOW = 5;
    private static final int MEASURED = 20;

    private ReactorTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("reactor_fills_seven_steam_turbines_in_a_row_against_its_steam_port_at_their_rate", 140, ReactorTests::fillsSevenTurbines);
        tests.test("reactor_makes_21_mb_a_tick_spending_water_unit_for_unit", 40, ReactorTests::makesItsRate);
        tests.test("reactor_burns_nothing_without_water", 40, ReactorTests::stopsWithoutWater);
        tests.test("reactor_burns_nothing_without_room_for_its_steam_and_resumes_when_drawn", 100, ReactorTests::stopsWithoutRoom);
        tests.test("reactor_burnt_cell_leaves_a_depleted_fuel_cell", 40, ReactorTests::leavesDepletedCell);
        tests.test("reactor_other_fission_fuel_leaves_its_crafting_remainder_or_nothing", 40, ReactorTests::otherFuelLeavesItsRemainder);
        tests.test("reactor_full_depleted_slot_stops_it_until_one_is_taken", 40, ReactorTests::fullDepletedSlotStops);
        tests.test("reactor_slots_are_reached_from_every_block_and_depleted_only_lets_out", 20, ReactorTests::slotsFromEveryBlock);
        tests.test("reactor_hoppers_feed_fuel_and_take_only_depleted_cells", 60, ReactorTests::hoppers);
        tests.test("reactor_draws_every_block_of_its_footprint", 20, helper -> {
            Network.expectDrawn(helper, "a Reactor",
                    Network.footprint(helper, WireworksRegistries.REACTOR_FOOTPRINT, ANCHOR, FACING));
            helper.succeed();
        });
        tests.test("reactor_hoppers_feed_a_water_bucket_and_take_the_empty_one", 60, ReactorTests::hoppersRouteBuckets);
        tests.test("reactor_slots_take_only_what_they_are_for", 20, ReactorTests::slotsTakeWhatTheyAreFor);
        tests.test("reactor_menu_shift_click_sends_fuel_and_water_home_and_nothing_to_an_output", 20, ReactorTests::shiftClick);
        tests.test("reactor_menu_ghosts_show_what_each_slot_takes", 20, ReactorTests::ghosts);
        tests.test("reactor_item_face_puts_a_water_bucket_in_its_slot_and_gives_the_empty_one", 20, ReactorTests::itemFaceRoutesBuckets);
        tests.test("reactor_empties_a_water_bucket_into_its_tank_and_the_bucket_to_the_bucket_output", 20, ReactorTests::emptiesAWaterBucket);
        tests.test("reactor_holds_a_water_bucket_while_its_bucket_output_has_no_room", 20, ReactorTests::holdsAWaterBucket);
        tests.test("reactor_says_why_it_is_not_running", 20, ReactorTests::saysWhy);
        tests.test("reactor_ports_open_onto_their_outer_faces_whatever_its_facing", 20, ReactorTests::portsOpenOutward);
        tests.test("reactor_pulls_water_from_a_passive_handler_against_a_water_port", 40, ReactorTests::pullsWater);
        tests.test("reactor_leaves_a_boilers_water_port_alone", 40, ReactorTests::leavesABoilersWaterPort);
        tests.test("reactor_water_ports_take_water_only_and_the_steam_port_gives_only", 20, ReactorTests::portsTakeAndGive);
    }

    /** A pipe pushes nothing, so the Reactor pulls: a full water cauldron against a water port empties into it. */
    private static void pullsWater(GameTestHelper helper) {
        place(helper);
        helper.getLevel().setBlock(helper.absolutePos(WEST_WATER.west()),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), Block.UPDATE_ALL);
        helper.runAfterDelay(20, () -> {
            int held = reactor(helper).water();
            if (held != 1_000) {
                helper.fail("the Reactor drew " + held + " mB from a full water cauldron against its water port, not 1000", ANCHOR);
            }
            helper.succeed();
        });
    }

    /**
     * A Boiler's west water port stands against the Reactor's east one, the Boiler holding water: the
     * Reactor does not draw it, since the Boiler pulls its own and the two would only trade it.
     */
    private static void leavesABoilersWaterPort(GameTestHelper helper) {
        place(helper);
        BlockPos boiler = new BlockPos(5, 1, 3);
        Network.footprint(helper, WireworksRegistries.BOILER_FOOTPRINT, boiler, Direction.NORTH);
        insert(fluidAt(helper, boiler.west(), Direction.WEST), water(), 1_000);
        helper.runAfterDelay(20, () -> {
            if (reactor(helper).water() != 0) {
                helper.fail("the Reactor drew " + reactor(helper).water() + " mB from a Boiler's water port", ANCHOR);
            }
            helper.succeed();
        });
    }

    private static void place(GameTestHelper helper) {
        Network.footprint(helper, WireworksRegistries.REACTOR_FOOTPRINT, ANCHOR, FACING);
    }

    private static ReactorBlockEntity reactor(GameTestHelper helper) {
        return helper.getBlockEntity(ANCHOR, ReactorBlockEntity.class);
    }

    private static ResourceHandler<FluidResource> fluidAt(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), side);
    }

    private static int insert(ResourceHandler<FluidResource> face, FluidResource fluid, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = face.insert(fluid, amount, transaction);
            transaction.commit();
            return taken;
        }
    }

    private static int extract(ResourceHandler<FluidResource> face, FluidResource fluid, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int given = face.extract(fluid, amount, transaction);
            transaction.commit();
            return given;
        }
    }

    private static FluidResource water() {
        return FluidResource.of(Fluids.WATER);
    }

    private static FluidResource superheated() {
        return FluidResource.of(SteamFluids.SUPERHEATED_STEAM_SOURCE.get());
    }

    private static FluidResource steam() {
        return FluidResource.of(SteamFluids.STEAM_SOURCE.get());
    }

    private static ItemStack cells(int count) {
        return new ItemStack(WireworksRegistries.FUEL_CELL.get(), count);
    }

    private static void fillWater(GameTestHelper helper) {
        ResourceHandler<FluidResource> port = fluidAt(helper, WEST_WATER, Direction.WEST);
        int room = port.getCapacityAsInt(0, water()) - port.getAmountAsInt(0);
        if (room > 0) {
            insert(port, water(), room);
        }
    }

    /** Fuel and full water, and the Reactor started. */
    private static void start(GameTestHelper helper, int cells) {
        reactor(helper).setItem(ReactorSlots.FUEL, cells(cells));
        fillWater(helper);
    }

    /** Sets the loaded cell's ticks left as a saved Reactor would load them, so a test need not run 4,000 ticks. */
    private static void setCellTicksLeft(GameTestHelper helper, int ticks) {
        loadSaved(helper, "CellTicks", ticks);
    }

    /** Sets one saved number of the Reactor's and loads it back, as a saved Reactor would load. */
    private static void loadSaved(GameTestHelper helper, String key, int value) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = reactor(helper).saveWithoutMetadata(registries);
        saved.putInt(key, value);
        reactor(helper).loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));
    }

    /**
     * Seven Turbines in a row along x, the first standing against the steam port, so the Reactor hands
     * them its steam with no pipe. Each tick the test tops up the water and empties every Turbine's buffer.
     */
    private static void fillsSevenTurbines(GameTestHelper helper) {
        place(helper);
        BlockPos[] turbines = new BlockPos[TURBINES];
        for (int i = 0; i < TURBINES; i++) {
            turbines[i] = STEAM_PORT.relative(STEAM_SIDE, 1 + i).below();
            Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, turbines[i], Direction.WEST);
        }
        reactor(helper).setItem(ReactorSlots.FUEL, cells(1));
        long[] drawn = new long[TURBINES];
        int[] tick = {0};
        helper.onEachTick(() -> {
            fillWater(helper);
            boolean measured = tick[0] >= Network.SETTLE && tick[0] < Network.SETTLE + MEASURED;
            for (int i = 0; i < TURBINES; i++) {
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
        helper.startSequence()
                .thenIdle(Network.SETTLE + MEASURED + 1)
                .thenExecute(() -> {
                    for (int i = 0; i < TURBINES; i++) {
                        if (drawn[i] != TURBINE_FE_PER_TICK * MEASURED) {
                            helper.fail("turbine " + i + " made " + drawn[i] + " FE over " + MEASURED + " ticks, not "
                                    + TURBINE_FE_PER_TICK * MEASURED, turbines[i]);
                        }
                    }
                    if (!reactor(helper).getItem(ReactorSlots.FUEL).isEmpty()) {
                        helper.fail("the reactor never loaded its cell", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** Water in, a cell in, room for steam: 21 mB a tick, and the water falls to match. */
    private static void makesItsRate(GameTestHelper helper) {
        place(helper);
        int[] before = new int[2];
        helper.startSequence()
                .thenExecute(() -> start(helper, 1))
                .thenIdle(1)
                .thenExecute(() -> {
                    before[0] = reactor(helper).water();
                    before[1] = reactor(helper).steam();
                    if (reactor(helper).cellTicksLeft() != CELL_TICKS - 1) {
                        helper.fail("the reactor has " + reactor(helper).cellTicksLeft() + " ticks of its cell left after one, not "
                                + (CELL_TICKS - 1), ANCHOR);
                    }
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    int made = reactor(helper).steam() - before[1];
                    if (made != MILLIBUCKETS_PER_TICK * WINDOW) {
                        helper.fail("the reactor made " + made + " mB of superheated steam over " + WINDOW + " ticks, not "
                                + MILLIBUCKETS_PER_TICK * WINDOW, ANCHOR);
                    }
                    int spent = before[0] - reactor(helper).water();
                    if (spent != made) {
                        helper.fail("the reactor spent " + spent + " mB of water making " + made + " mB of steam", ANCHOR);
                    }
                    if (reactor(helper).cellTicksLeft() != CELL_TICKS - 1 - WINDOW) {
                        helper.fail("the cell counted " + (CELL_TICKS - 1 - reactor(helper).cellTicksLeft()) + " ticks over "
                                + (WINDOW + 1) + " of running", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** With a cell in the slot and no water, it loads nothing and makes nothing. */
    private static void stopsWithoutWater(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenExecute(() -> reactor(helper).setItem(ReactorSlots.FUEL, cells(1)))
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    if (reactor(helper).getItem(ReactorSlots.FUEL).getCount() != 1 || reactor(helper).cellTicksLeft() != 0) {
                        helper.fail("a dry reactor loaded its cell", ANCHOR);
                    }
                    if (reactor(helper).steam() != 0) {
                        helper.fail("a dry reactor made steam", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /**
     * Once the steam tank has no room for a tick, the cell's ticks and the water hold still; a pipe
     * drawing steam from the steam port starts it again. The tank is loaded nearly full rather than
     * filled, which would take as long as the water lasts.
     */
    private static void stopsWithoutRoom(GameTestHelper helper) {
        place(helper);
        int[] held = new int[2];
        helper.startSequence()
                .thenExecute(() -> start(helper, 1))
                .thenIdle(1)
                .thenExecute(() -> loadSaved(helper, "Steam", TANK_CAPACITY - MILLIBUCKETS_PER_TICK + 1))
                .thenExecute(() -> {
                    held[0] = reactor(helper).cellTicksLeft();
                    held[1] = reactor(helper).water();
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    if (reactor(helper).cellTicksLeft() != held[0]) {
                        helper.fail("a reactor with no room burnt " + (held[0] - reactor(helper).cellTicksLeft()) + " ticks of its cell", ANCHOR);
                    }
                    if (reactor(helper).water() != held[1]) {
                        helper.fail("a reactor with no room spent water", ANCHOR);
                    }
                    expectStatus(helper, ReactorStatus.STEAM_BACKED_UP);
                    if (extract(fluidAt(helper, STEAM_PORT, STEAM_SIDE), superheated(), 100) != 100) {
                        helper.fail("a pipe could not draw 100 mB from the full steam tank", ANCHOR);
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    if (reactor(helper).cellTicksLeft() >= held[0]) {
                        helper.fail("the reactor did not resume once its steam was drawn", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** The last tick of a cell leaves a Depleted Fuel Cell, and the next tick loads the next cell. */
    private static void leavesDepletedCell(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenExecute(() -> start(helper, 2))
                .thenIdle(1)
                .thenExecute(() -> setCellTicksLeft(helper, 2))
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    ItemStack depleted = reactor(helper).getItem(ReactorSlots.DEPLETED);
                    if (!depleted.is(WireworksRegistries.DEPLETED_FUEL_CELL.get()) || depleted.getCount() != 1) {
                        helper.fail("the burnt cell left " + depleted + ", not one depleted fuel cell", ANCHOR);
                    }
                    if (!reactor(helper).getItem(ReactorSlots.FUEL).isEmpty()) {
                        helper.fail("the reactor did not load its second cell", ANCHOR);
                    }
                    int left = reactor(helper).cellTicksLeft();
                    if (left <= CELL_TICKS - WINDOW || left >= CELL_TICKS) {
                        helper.fail("the second cell has " + left + " ticks left", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /**
     * A Fission Fuel other than the Fuel Cell leaves its own crafting remainder, a lava bucket its
     * bucket, and one with none, a blaze rod, leaves nothing; neither leaves a Depleted Fuel Cell. The
     * game-test pack adds both to the tag.
     */
    private static void otherFuelLeavesItsRemainder(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    reactor(helper).setItem(ReactorSlots.FUEL, new ItemStack(Items.LAVA_BUCKET));
                    fillWater(helper);
                })
                .thenIdle(1)
                .thenExecute(() -> setCellTicksLeft(helper, 2))
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    ItemStack left = reactor(helper).getItem(ReactorSlots.DEPLETED);
                    helper.assertTrue(left.is(Items.BUCKET) && left.getCount() == 1, "a burnt lava bucket left " + left + ", not its bucket");
                    reactor(helper).setItem(ReactorSlots.FUEL, new ItemStack(Items.BLAZE_ROD));
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(reactor(helper).getItem(ReactorSlots.FUEL).isEmpty(), "the reactor did not load the blaze rod");
                    setCellTicksLeft(helper, 2);
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    // Still the lava bucket's one bucket: the blaze rod added nothing.
                    ItemStack left = reactor(helper).getItem(ReactorSlots.DEPLETED);
                    helper.assertTrue(left.is(Items.BUCKET) && left.getCount() == 1, "a burnt blaze rod left something: " + left);
                    helper.assertValueEqual(reactor(helper).cellTicksLeft(), 0, "ticks left once the blaze rod burnt out");
                })
                .thenSucceed();
    }

    /** A depleted slot of 64 cells stops the Reactor mid-cell; one taken out by a pipe starts it again. */
    private static void fullDepletedSlotStops(GameTestHelper helper) {
        place(helper);
        int[] held = new int[1];
        helper.startSequence()
                .thenExecute(() -> start(helper, 1))
                .thenIdle(1)
                .thenExecute(() -> {
                    reactor(helper).setItem(ReactorSlots.DEPLETED, new ItemStack(WireworksRegistries.DEPLETED_FUEL_CELL.get(), 64));
                    held[0] = reactor(helper).cellTicksLeft();
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    if (reactor(helper).cellTicksLeft() != held[0]) {
                        helper.fail("a reactor with a full depleted slot kept burning", ANCHOR);
                    }
                    expectStatus(helper, ReactorStatus.DEPLETED_FULL);
                    ResourceHandler<ItemResource> face =
                            helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(STEAM_PORT), Direction.UP);
                    try (Transaction transaction = Transaction.openRoot()) {
                        int taken = face.extract(ItemResource.of(WireworksRegistries.DEPLETED_FUEL_CELL.get()), 1, transaction);
                        transaction.commit();
                        if (taken != 1) {
                            helper.fail("a pipe took " + taken + " depleted cells, not one", ANCHOR);
                        }
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    if (reactor(helper).cellTicksLeft() >= held[0]) {
                        helper.fail("the reactor did not resume once its depleted slot had room", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /**
     * A hopper or a pipe on any of the 27 blocks, on any side, puts Fission Fuel in and takes Depleted
     * Fuel Cells out, and nothing the other way, whether or not it names a slot.
     */
    private static void slotsFromEveryBlock(GameTestHelper helper) {
        place(helper);
        reactor(helper).setItem(ReactorSlots.FUEL, cells(1));
        reactor(helper).setItem(ReactorSlots.DEPLETED, new ItemStack(WireworksRegistries.DEPLETED_FUEL_CELL.get(), 1));
        ItemResource cell = ItemResource.of(WireworksRegistries.FUEL_CELL.get());
        ItemResource depleted = ItemResource.of(WireworksRegistries.DEPLETED_FUEL_CELL.get());
        ItemResource glowstone = ItemResource.of(Items.GLOWSTONE_DUST);
        List<BlockPos> blocks = WireworksRegistries.REACTOR_FOOTPRINT.positions(helper.absolutePos(ANCHOR), FACING);
        for (BlockPos pos : blocks) {
            for (Direction side : new Direction[] {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST}) {
                ResourceHandler<ItemResource> face = helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, side);
                String where = "the block at " + helper.relativePos(pos) + " on side " + side;
                if (face == null) {
                    helper.fail(where + " has no item face", helper.relativePos(pos));
                    return;
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    helper.assertTrue(face.insert(cell, 1, transaction) == 1, where + " did not take a fuel cell");
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    helper.assertTrue(face.extract(depleted, 1, transaction) == 1, where + " did not give a depleted cell");
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    helper.assertTrue(face.insert(depleted, 1, transaction) == 0, where + " took a depleted cell");
                    helper.assertTrue(face.insert(ReactorSlots.DEPLETED, depleted, 1, transaction) == 0,
                            where + " took a depleted cell into the depleted slot");
                    helper.assertTrue(face.insert(glowstone, 1, transaction) == 0, where + " took an item that is not fission fuel");
                    helper.assertTrue(face.extract(cell, 1, transaction) == 0, where + " gave back a fuel cell");
                    helper.assertTrue(face.extract(ReactorSlots.FUEL, cell, 1, transaction) == 0,
                            where + " gave back a fuel cell from its slot");
                }
            }
        }
        helper.succeed();
    }

    /**
     * Real hoppers, on a Reactor raised a block: one above the top centre pushes a Fuel Cell in, and
     * one under the anchor and one under a corner each pull the Depleted Fuel Cells out and never the
     * Fuel Cells. The anchor's is the one that reaches the block entity itself, a vanilla container.
     */
    private static void hoppers(GameTestHelper helper) {
        BlockPos raised = ANCHOR.above();
        Network.footprint(helper, WireworksRegistries.REACTOR_FOOTPRINT, raised, FACING);
        BlockPos above = raised.above(3);
        BlockPos underAnchor = ANCHOR;
        BlockPos underCorner = ANCHOR.offset(1, 0, 1);
        for (BlockPos hopper : List.of(above, underAnchor, underCorner)) {
            helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState());
        }
        helper.getBlockEntity(above, HopperBlockEntity.class).setItem(0, cells(1));
        ReactorBlockEntity reactor = helper.getBlockEntity(raised, ReactorBlockEntity.class);
        reactor.setItem(ReactorSlots.FUEL, cells(4));
        reactor.setItem(ReactorSlots.DEPLETED, new ItemStack(WireworksRegistries.DEPLETED_FUEL_CELL.get(), 2));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(reactor.getItem(ReactorSlots.DEPLETED).isEmpty(), "the hoppers have not taken both depleted cells yet");
                    helper.assertValueEqual(reactor.getItem(ReactorSlots.FUEL).getCount(), 5, "fuel cells in the reactor");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertValueEqual(reactor.getItem(ReactorSlots.FUEL).getCount(), 5, "fuel cells in the reactor after the hoppers ran on");
                    for (BlockPos under : List.of(underAnchor, underCorner)) {
                        HopperBlockEntity hopper = helper.getBlockEntity(under, HopperBlockEntity.class);
                        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
                            helper.assertFalse(hopper.getItem(slot).is(WireworksRegistries.FUEL_CELL.get()),
                                    "a hopper at " + under + " pulled a fuel cell out");
                        }
                    }
                })
                .thenSucceed();
    }

    /**
     * Real hoppers on a Reactor raised a block, with no fuel so it never runs: one above the top centre
     * pushes a water bucket in, which empties into the tank, and the one under the anchor pulls the empty
     * bucket out of the bucket output.
     */
    private static void hoppersRouteBuckets(GameTestHelper helper) {
        BlockPos raised = ANCHOR.above();
        Network.footprint(helper, WireworksRegistries.REACTOR_FOOTPRINT, raised, FACING);
        BlockPos above = raised.above(3);
        BlockPos underAnchor = ANCHOR;
        helper.setBlock(above, Blocks.HOPPER.defaultBlockState());
        helper.setBlock(underAnchor, Blocks.HOPPER.defaultBlockState());
        helper.getBlockEntity(above, HopperBlockEntity.class).setItem(0, new ItemStack(Items.WATER_BUCKET));
        ReactorBlockEntity reactor = helper.getBlockEntity(raised, ReactorBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertValueEqual(reactor.water(), 1_000, "water in the tank");
                    helper.assertTrue(helper.getBlockEntity(underAnchor, HopperBlockEntity.class).getItem(0).is(Items.BUCKET),
                            "the hopper under the anchor has not pulled the empty bucket yet");
                })
                .thenExecute(() -> {
                    helper.assertTrue(reactor.getItem(ReactorSlots.WATER).isEmpty(), "the water slot still holds a bucket");
                    helper.assertTrue(reactor.getItem(ReactorSlots.BUCKETS).isEmpty(), "the bucket output still holds a bucket");
                })
                .thenSucceed();
    }

    /** Fission Fuel in the fuel slot, a water container in the water slot, and nothing in either output. */
    private static void slotsTakeWhatTheyAreFor(GameTestHelper helper) {
        place(helper);
        Object[][] cases = {
                {WireworksRegistries.FUEL_CELL.get(), ReactorSlots.FUEL},
                {Items.WATER_BUCKET, ReactorSlots.WATER},
                {Items.BUCKET, -1},
                {WireworksRegistries.DEPLETED_FUEL_CELL.get(), -1},
                {Items.GLOWSTONE_DUST, -1},
        };
        for (Object[] each : cases) {
            ItemStack stack = new ItemStack((Item) each[0]);
            for (int slot = 0; slot < ReactorSlots.SIZE; slot++) {
                boolean expected = slot == (int) each[1];
                helper.assertValueEqual(reactor(helper).canPlaceItem(slot, stack), expected, "slot " + slot + " taking " + stack);
            }
        }
        helper.succeed();
    }

    private static ReactorMenu menu(GameTestHelper helper, ListeningPlayer player) {
        return new ReactorMenu(0, player.getInventory(), reactor(helper), reactor(helper).data());
    }

    /** Shift-click sends a Fuel Cell to the fuel slot and a water bucket to the water slot, and an empty bucket nowhere. */
    private static void shiftClick(GameTestHelper helper) {
        place(helper);
        reactor(helper).setItem(ReactorSlots.BUCKETS, new ItemStack(Items.BUCKET));
        ListeningPlayer player = new ListeningPlayer(helper, ANCHOR.above(3));
        player.getInventory().setItem(0, cells(3));
        player.getInventory().setItem(1, new ItemStack(Items.WATER_BUCKET));
        player.getInventory().setItem(2, new ItemStack(Items.BUCKET, 15));
        ReactorMenu menu = menu(helper, player);
        int hotbarFirst = ReactorSlots.SIZE + 27;
        for (int i = 0; i < 3; i++) {
            menu.quickMoveStack(player, hotbarFirst + i);
        }
        helper.assertValueEqual(reactor(helper).getItem(ReactorSlots.FUEL).getCount(), 3, "fuel cells in the fuel slot");
        helper.assertTrue(reactor(helper).getItem(ReactorSlots.WATER).is(Items.WATER_BUCKET), "the water bucket is not in the water slot");
        helper.assertValueEqual(reactor(helper).getItem(ReactorSlots.BUCKETS).getCount(), 1, "buckets in the bucket output");
        helper.assertTrue(reactor(helper).getItem(ReactorSlots.DEPLETED).isEmpty(), "something reached the depleted slot");
        helper.succeed();
    }

    /** The fuel and water slots' ghosts are what each takes; the depleted slot's is the Depleted Fuel Cell the screen adds. */
    private static void ghosts(GameTestHelper helper) {
        place(helper);
        ReactorMenu menu = menu(helper, new ListeningPlayer(helper, ANCHOR.above(3)));
        List<ItemStack> fuel = Ghosts.accepted(menu.slots.get(ReactorSlots.FUEL));
        helper.assertTrue(fuel.stream().anyMatch(stack -> stack.is(WireworksRegistries.FUEL_CELL.get())), "the fuel slot's ghosts lack the fuel cell");
        helper.assertFalse(fuel.stream().anyMatch(stack -> stack.is(Items.WATER_BUCKET)), "the fuel slot's ghosts show a water bucket");
        List<ItemStack> water = Ghosts.accepted(menu.slots.get(ReactorSlots.WATER));
        helper.assertTrue(water.stream().anyMatch(stack -> stack.is(Items.WATER_BUCKET)), "the water slot's ghosts lack the water bucket");
        helper.assertTrue(Ghosts.accepted(menu.slots.get(ReactorSlots.DEPLETED)).isEmpty(), "the depleted slot takes something");
        helper.assertTrue(Ghosts.accepted(menu.slots.get(ReactorSlots.BUCKETS)).isEmpty(), "the bucket output takes something");
        helper.succeed();
    }

    /** A pipe that names no slot puts a water bucket in the water slot, takes the empty one out, and never the full one. */
    private static void itemFaceRoutesBuckets(GameTestHelper helper) {
        place(helper);
        reactor(helper).setItem(ReactorSlots.BUCKETS, new ItemStack(Items.BUCKET));
        ResourceHandler<ItemResource> face = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(STEAM_PORT), null);
        ItemResource waterBucket = ItemResource.of(new ItemStack(Items.WATER_BUCKET));
        ItemResource bucket = ItemResource.of(new ItemStack(Items.BUCKET));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(face.insert(waterBucket, 1, transaction), 1, "water buckets put in");
            helper.assertValueEqual(face.insert(bucket, 1, transaction), 0, "empty buckets put in");
            helper.assertValueEqual(face.extract(bucket, 1, transaction), 1, "empty buckets drawn slot-less");
            helper.assertValueEqual(face.extract(waterBucket, 1, transaction), 0, "water buckets drawn back out");
            transaction.commit();
        }
        helper.assertTrue(reactor(helper).getItem(ReactorSlots.WATER).is(Items.WATER_BUCKET), "the water bucket is not in the water slot");
        helper.succeed();
    }

    private static void emptiesAWaterBucket(GameTestHelper helper) {
        place(helper);
        reactor(helper).setItem(ReactorSlots.WATER, new ItemStack(Items.WATER_BUCKET));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(reactor(helper).water(), 1_000, "water in the tank"))
                .thenExecute(() -> {
                    helper.assertTrue(reactor(helper).getItem(ReactorSlots.WATER).isEmpty(), "the water slot still holds a bucket");
                    helper.assertTrue(reactor(helper).getItem(ReactorSlots.BUCKETS).is(Items.BUCKET), "the empty bucket is not in the bucket output");
                    helper.assertTrue(reactor(helper).getItem(ReactorSlots.DEPLETED).isEmpty(), "a bucket reached the depleted slot");
                })
                .thenSucceed();
    }

    /** With something else in the bucket output, the water bucket waits, and the Reactor says why. */
    private static void holdsAWaterBucket(GameTestHelper helper) {
        place(helper);
        reactor(helper).setItem(ReactorSlots.WATER, new ItemStack(Items.WATER_BUCKET));
        reactor(helper).setItem(ReactorSlots.BUCKETS, new ItemStack(Items.STONE));
        helper.startSequence()
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    helper.assertValueEqual(reactor(helper).water(), 0, "water in the tank");
                    helper.assertTrue(reactor(helper).getItem(ReactorSlots.WATER).is(Items.WATER_BUCKET), "the water bucket left its slot");
                    // Not "no water": the water is there, waiting on the bucket output.
                    expectStatus(helper, ReactorStatus.BUCKETS_FULL);
                })
                .thenSucceed();
    }

    /**
     * The status the screen and Jade name: no water, then no fuel, then running. Steam backed up and
     * a full depleted slot are asserted where those stops are set up, and a full bucket output above.
     */
    private static void saysWhy(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    expectStatus(helper, ReactorStatus.NO_WATER);
                    fillWater(helper);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    expectStatus(helper, ReactorStatus.NO_FUEL);
                    reactor(helper).setItem(ReactorSlots.FUEL, cells(1));
                })
                .thenIdle(1)
                .thenExecute(() -> expectStatus(helper, ReactorStatus.RUNNING))
                .thenSucceed();
    }

    private static void expectStatus(GameTestHelper helper, ReactorStatus expected) {
        helper.assertValueEqual(reactor(helper).status(), expected, "the Reactor's status");
        helper.assertValueEqual(reactor(helper).data().get(ReactorBlockEntity.DATA_STATUS), expected.ordinal(), "the status sent to the menu");
    }

    /**
     * Each water port opens outward from its side of the bottom layer and the steam port backward from
     * the back middle of the middle layer; every other block, the top centre and the anchor included,
     * opens onto nothing. The null side answers the port blocks only.
     * Worked out from where each block stands, not from its part number.
     */
    private static void portsOpenOutward(GameTestHelper helper) {
        Direction[] sides = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        BlockPos anchor = helper.absolutePos(ANCHOR);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> blocks = Network.footprint(helper, WireworksRegistries.REACTOR_FOOTPRINT, ANCHOR, facing);
            for (BlockPos pos : blocks) {
                BlockPos d = pos.subtract(anchor);
                Direction open = null;
                if (d.getY() == 0 && Math.abs(d.getX()) + Math.abs(d.getZ()) == 1) {
                    open = Direction.getApproximateNearest(d.getX(), 0, d.getZ());
                } else if (d.equals(BlockPos.ZERO.above().relative(facing.getOpposite()))) {
                    open = facing.getOpposite();
                }
                for (Direction side : sides) {
                    boolean answers = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, side) != null;
                    boolean expected = open != null && (side == null || side == open);
                    if (answers != expected) {
                        helper.fail("facing " + facing + ", the block " + d + (answers ? " answers" : " does not answer")
                                + " the fluid capability on side " + side, helper.relativePos(pos));
                        return;
                    }
                }
            }
            for (BlockPos pos : blocks) {
                helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        helper.succeed();
    }

    /**
     * The four water ports are one 10,000 mB tank that takes water and nothing else; the steam port is a
     * 10,000 mB tank that takes nothing in.
     */
    private static void portsTakeAndGive(GameTestHelper helper) {
        place(helper);
        ResourceHandler<FluidResource> west = fluidAt(helper, WEST_WATER, Direction.WEST);
        ResourceHandler<FluidResource> east = fluidAt(helper, ANCHOR.east(), Direction.EAST);
        ResourceHandler<FluidResource> top = fluidAt(helper, STEAM_PORT, STEAM_SIDE);
        helper.assertValueEqual(west.getCapacityAsInt(0, water()), TANK_CAPACITY, "the water tank's capacity");
        helper.assertValueEqual(top.getCapacityAsInt(0, superheated()), TANK_CAPACITY, "the steam tank's capacity");
        helper.assertValueEqual(insert(west, steam(), 100), 0, "steam taken by a water port");
        helper.assertValueEqual(insert(west, superheated(), 100), 0, "superheated steam taken by a water port");
        helper.assertValueEqual(insert(west, water(), 100), 100, "water taken by a water port");
        helper.assertValueEqual(east.getAmountAsInt(0), 100, "water put in the west port, seen from the east one");
        helper.assertValueEqual(insert(top, superheated(), 100), 0, "superheated steam put into the steam port");
        helper.assertValueEqual(insert(top, water(), 100), 0, "water put into the steam port");
        helper.assertValueEqual(top.getAmountAsInt(0), 0, "steam in the steam port before any was made");
        helper.succeed();
    }
}
