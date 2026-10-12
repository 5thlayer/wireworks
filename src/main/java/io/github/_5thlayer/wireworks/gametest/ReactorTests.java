// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.wireworks.ReactorBlockEntity;
import io.github._5thlayer.wireworks.ReactorSlots;
import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.SteamTurbineBlockEntity;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
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
 * each horizontal axis and its steam port two blocks above it.
 */
final class ReactorTests {

    private static final BlockPos ANCHOR = new BlockPos(2, 1, 3);
    private static final Direction FACING = Direction.NORTH;

    /** What a whole tick converts, in millibuckets. A literal, so the test cannot agree with the spec by construction. */
    private static final int MILLIBUCKETS_PER_TICK = 21;
    private static final long TURBINE_FE_PER_TICK = 2_910L;
    private static final int TURBINES = 7;
    private static final int CELL_TICKS = 4_000;
    private static final int STEAM_CAPACITY = 200;

    private static final BlockPos WEST_WATER = ANCHOR.west();
    private static final BlockPos STEAM_PORT = ANCHOR.above(2);

    private static final int WINDOW = 5;
    private static final int MEASURED = 20;

    private ReactorTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("reactor_fills_seven_turbines_end_to_end_at_their_rate", 140, ReactorTests::fillsSevenTurbines);
        tests.test("reactor_offers_its_steam_to_what_stands_on_its_top_centre", 40, ReactorTests::offersSteamUpward);
        tests.test("reactor_makes_21_mb_a_tick_spending_water_unit_for_unit", 40, ReactorTests::makesItsRate);
        tests.test("reactor_burns_nothing_without_water", 40, ReactorTests::stopsWithoutWater);
        tests.test("reactor_burns_nothing_without_room_for_its_steam_and_resumes_when_drawn", 100, ReactorTests::stopsWithoutRoom);
        tests.test("reactor_burnt_cell_leaves_a_depleted_fuel_cell", 40, ReactorTests::leavesDepletedCell);
        tests.test("reactor_full_depleted_slot_stops_it_until_one_is_taken", 40, ReactorTests::fullDepletedSlotStops);
        tests.test("reactor_slots_are_reached_from_every_block_and_depleted_only_lets_out", 20, ReactorTests::slotsFromEveryBlock);
        tests.test("reactor_hoppers_feed_fuel_and_take_only_depleted_cells", 60, ReactorTests::hoppers);
        tests.test("reactor_draws_every_block_of_its_footprint", 20, helper -> {
            Network.expectDrawn(helper, "a Reactor",
                    Network.footprint(helper, WireworksRegistries.REACTOR_FOOTPRINT, ANCHOR, FACING));
            helper.succeed();
        });
        tests.test("reactor_ports_open_onto_their_outer_faces_whatever_its_facing", 20, ReactorTests::portsOpenOutward);
        tests.test("reactor_water_ports_take_water_only_and_the_steam_port_gives_only", 20, ReactorTests::portsTakeAndGive);
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
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = reactor(helper).saveWithoutMetadata(registries);
        saved.putInt("CellTicks", ticks);
        reactor(helper).loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));
    }

    /**
     * Seven Turbines in a row along x, facing west so steam taken in at the west end passes east. The
     * test is the pipe: each tick it tops up the water and moves the Reactor's steam into the first
     * Turbine's west end, then empties every Turbine's buffer.
     */
    private static void fillsSevenTurbines(GameTestHelper helper) {
        place(helper);
        BlockPos[] turbines = new BlockPos[TURBINES];
        for (int i = 0; i < TURBINES; i++) {
            turbines[i] = new BlockPos(5 + i, 1, 3);
            Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, turbines[i], Direction.WEST);
        }
        reactor(helper).setItem(ReactorSlots.FUEL, cells(1));
        long[] drawn = new long[TURBINES];
        int[] tick = {0};
        helper.onEachTick(() -> {
            fillWater(helper);
            ResourceHandlerUtil.move(fluidAt(helper, STEAM_PORT, Direction.UP), fluidAt(helper, turbines[0], Direction.WEST),
                    resource -> true, Integer.MAX_VALUE, null);
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

    /** A Turbine standing on the top centre fills from it with no pipe between. */
    private static void offersSteamUpward(GameTestHelper helper) {
        place(helper);
        BlockPos turbine = STEAM_PORT.above();
        Network.footprint(helper, WireworksRegistries.STEAM_TURBINE_FOOTPRINT, turbine, FACING);
        helper.startSequence()
                .thenExecute(() -> start(helper, 1))
                .thenWaitUntil(() -> {
                    if (helper.getBlockEntity(turbine, SteamTurbineBlockEntity.class).energy().getAmountAsLong() <= 0L) {
                        helper.fail("the turbine on the top centre has made nothing yet", turbine);
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
     * drawing steam from the top centre starts it again.
     */
    private static void stopsWithoutRoom(GameTestHelper helper) {
        place(helper);
        int[] held = new int[2];
        helper.startSequence()
                .thenExecute(() -> start(helper, 1))
                .thenWaitUntil(() -> {
                    if (reactor(helper).steam() <= STEAM_CAPACITY - MILLIBUCKETS_PER_TICK) {
                        helper.fail("the steam tank has not filled yet", ANCHOR);
                    }
                })
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
                    if (extract(fluidAt(helper, STEAM_PORT, Direction.UP), superheated(), 100) != 100) {
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
     * Each water port opens outward from its side of the bottom layer and the steam port upward; every
     * other block, the anchor included, opens onto nothing. The null side answers the port blocks only.
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
                } else if (d.equals(new BlockPos(0, 2, 0))) {
                    open = Direction.UP;
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
     * The four water ports are one 800 mB tank that takes water and nothing else; the steam port is a
     * 200 mB tank that takes nothing in.
     */
    private static void portsTakeAndGive(GameTestHelper helper) {
        place(helper);
        ResourceHandler<FluidResource> west = fluidAt(helper, WEST_WATER, Direction.WEST);
        ResourceHandler<FluidResource> east = fluidAt(helper, ANCHOR.east(), Direction.EAST);
        ResourceHandler<FluidResource> top = fluidAt(helper, STEAM_PORT, Direction.UP);
        helper.assertValueEqual(west.getCapacityAsInt(0, water()), 800, "the water tank's capacity");
        helper.assertValueEqual(top.getCapacityAsInt(0, superheated()), STEAM_CAPACITY, "the steam tank's capacity");
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
