// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.libworks.ghost.Ghosts;
import io.github._5thlayer.wireworks.BoilerBlockEntity;
import io.github._5thlayer.wireworks.BoilerMenu;
import io.github._5thlayer.wireworks.BoilerSlots;
import io.github._5thlayer.wireworks.BoilerStatus;
import io.github._5thlayer.wireworks.SteamEngineBlockEntity;
import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

/**
 * The Boiler in a world: it boils water into steam at Factorio's rate and stalls when the steam
 * backs up, its ports open onto the faces they should, its item face takes fuel and gives none back,
 * and it feeds a Steam Engine standing against its steam port. What the numbers are is
 * {@code BoilerSpecTest}'s and what the stall is {@code BoilerCycleTest}'s; what is asserted here is
 * that they survive the trip through a running block entity.
 *
 * <p>The Boiler faces north: its front row runs along x at the anchor's z, its steam port is the
 * back middle block, one south of the anchor, and the steam leaves southward.
 */
final class BoilerTests {

    private static final BlockPos ANCHOR = new BlockPos(10, 1, 1);
    private static final Direction FACING = Direction.NORTH;

    /** The parts' numbers: the front row's two ends, the back corners, the back middle. */
    private static final int WEST_END = 1;
    private static final int EAST_END = 2;
    private static final int BACK_WEST = 3;
    private static final int STEAM_PART = 4;
    private static final int BACK_EAST = 5;

    /**
     * What a whole tick converts, in millibuckets: Factorio's 60 a second over twenty ticks. A
     * literal, so the test cannot agree with the spec by construction.
     */
    private static final int MILLIBUCKETS_PER_TICK = 3;
    private static final int WINDOW = 10;

    /** Each tank's two buckets: literals, so the test cannot agree with the spec by construction. */
    private static final int WATER_CAPACITY = 2_000;
    private static final int STEAM_CAPACITY = 2_000;

    private BoilerTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("boiler_boils", 100, BoilerTests::boils);
        tests.test("boiler_draws_every_block_of_its_footprint", 20, BoilerTests::drawsEveryBlock);
        tests.test("boiler_stalls_when_the_steam_backs_up_and_burns_nothing", 800, BoilerTests::stalls);
        tests.test("boiler_ports_open_onto_the_faces_adr_says", 20, BoilerTests::portsOpenWhereTheyShould);
        tests.test("boiler_ports_follow_the_facing", 20, BoilerTests::portsFollowTheFacing);
        tests.test("boiler_water_port_takes_water_only", 20, BoilerTests::waterPortTakesWaterOnly);
        tests.test("boiler_steam_port_gives_steam_and_takes_none", 20, BoilerTests::steamPortGivesSteamOnly);
        tests.test("boiler_item_face_takes_fuel_only", 20, BoilerTests::itemFaceTakesFuelOnly);
        tests.test("boiler_parts_reach_the_fuel_slot", 20, BoilerTests::partsReachTheFuelSlot);
        tests.test("boiler_hoppers_feed_fuel_and_take_only_the_output", 60, BoilerTests::hoppers);
        tests.test("boiler_item_face_gives_the_output_and_takes_nothing_into_it", 20, BoilerTests::itemFaceGivesTheOutput);
        tests.test("boiler_puts_a_burnt_fuels_remainder_in_its_output", 40, BoilerTests::remainderGoesToTheOutput);
        tests.test("boiler_lights_no_fuel_while_its_output_holds_another_item", 40,
                helper -> stallsOnAFullOutput(helper, new ItemStack(Items.STONE)));
        tests.test("boiler_lights_no_fuel_while_its_output_is_a_full_stack", 40,
                helper -> stallsOnAFullOutput(helper, new ItemStack(Items.BUCKET, Items.BUCKET.getDefaultMaxStackSize())));
        tests.test("boiler_menu_shift_click_puts_nothing_in_the_output", 20, BoilerTests::shiftClickSkipsTheOutput);
        tests.test("boiler_menu_shift_click_sends_each_container_to_its_slot", 20, BoilerTests::shiftClickSendsContainersHome);
        tests.test("boiler_slots_take_only_what_they_are_for", 20, BoilerTests::slotsTakeWhatTheyAreFor);
        tests.test("boiler_item_face_puts_buckets_in_their_slots_and_takes_none_back", 20, BoilerTests::itemFaceRoutesBuckets);
        tests.test("boiler_empties_a_water_bucket_into_its_tank", 20, BoilerTests::emptiesAWaterBucket);
        tests.test("boiler_holds_a_water_bucket_while_its_tank_has_no_room", 20, BoilerTests::holdsAWaterBucketWithoutRoom);
        tests.test("boiler_holds_a_water_bucket_while_its_output_has_no_room", 20, BoilerTests::holdsAWaterBucketWithoutOutputRoom);
        tests.test("boiler_fills_an_empty_bucket_with_steam", 500, BoilerTests::fillsABucketWithSteam);
        tests.test("boiler_says_why_it_is_not_boiling", 60, BoilerTests::saysWhy);
        tests.test("boiler_menu_ghosts_show_what_each_input_slot_takes", 20, BoilerTests::ghostsShowWhatSlotsTake);
        tests.test("steam_has_a_bucket", 20, helper -> {
            helper.assertTrue(SteamFluids.STEAM_SOURCE.get().getBucket() == WireworksRegistries.STEAM_BUCKET.get(),
                    "steam's bucket is " + SteamFluids.STEAM_SOURCE.get().getBucket());
            helper.succeed();
        });
        tests.test("boiler_feeds_a_steam_engine_against_its_steam_port", 200, BoilerTests::feedsAnEngine);
    }

    private static BlockPos place(GameTestHelper helper) {
        Network.footprint(helper, WireworksRegistries.BOILER_FOOTPRINT, ANCHOR, FACING);
        return helper.absolutePos(ANCHOR);
    }

    private static BoilerBlockEntity boiler(GameTestHelper helper) {
        return helper.getBlockEntity(ANCHOR, BoilerBlockEntity.class);
    }

    private static BlockPos part(GameTestHelper helper, int number) {
        return WireworksRegistries.BOILER_FOOTPRINT.positions(helper.absolutePos(ANCHOR), FACING).get(number);
    }

    private static ResourceHandler<FluidResource> fluidAt(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, side);
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

    private static FluidResource steam() {
        return FluidResource.of(SteamFluids.STEAM_SOURCE.get());
    }

    private static void drawsEveryBlock(GameTestHelper helper) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> blocks = Network.footprint(helper, WireworksRegistries.BOILER_FOOTPRINT, ANCHOR, facing);
            Network.expectDrawn(helper, "a Boiler facing " + facing, blocks);
            for (BlockPos block : blocks) {
                helper.getLevel().setBlock(block, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        helper.succeed();
    }

    /** Water in, fuel in, room for steam: it boils, and the water falls to match unit for unit. */
    private static void boils(GameTestHelper helper) {
        place(helper);
        int[] before = new int[2];
        helper.startSequence()
                .thenExecute(() -> {
                    boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 8));
                    insert(fluidAt(helper, part(helper, WEST_END), null), water(), WATER_CAPACITY);
                })
                // One tick to light, so the window is whole ticks of boiling and not the one that paid for the first.
                .thenIdle(1)
                .thenExecute(() -> {
                    before[0] = boiler(helper).data().get(BoilerBlockEntity.DATA_WATER);
                    before[1] = boiler(helper).data().get(BoilerBlockEntity.DATA_STEAM);
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    int made = boiler(helper).data().get(BoilerBlockEntity.DATA_STEAM) - before[1];
                    if (made != MILLIBUCKETS_PER_TICK * WINDOW) {
                        helper.fail("the boiler made " + made + " mB of steam over " + WINDOW
                                + " ticks, not " + MILLIBUCKETS_PER_TICK * WINDOW, ANCHOR);
                    }
                    int spent = before[0] - boiler(helper).data().get(BoilerBlockEntity.DATA_WATER);
                    if (spent != made) {
                        helper.fail("the boiler spent " + spent + " mB of water making " + made + " mB of steam", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** A steam tank with no room for another tick makes no steam, burns no fuel and lights nothing, and it resumes the moment a pipe drains it. */
    private static void stalls(GameTestHelper helper) {
        place(helper);
        int[] held = new int[3];
        helper.startSequence()
                .thenExecute(() -> {
                    // 2,000 mB is 667 ticks of boiling, and a coal 44.
                    boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 20));
                    insert(fluidAt(helper, part(helper, WEST_END), null), water(), WATER_CAPACITY);
                })
                .thenWaitUntil(() -> {
                    if (boiler(helper).data().get(BoilerBlockEntity.DATA_STEAM) < STEAM_CAPACITY - MILLIBUCKETS_PER_TICK + 1) {
                        helper.fail("the steam tank has not filled yet", ANCHOR);
                    }
                })
                .thenExecute(() -> {
                    // The tanks are the same size, so the water has run down as the steam filled.
                    insert(fluidAt(helper, part(helper, WEST_END), null), water(), WATER_CAPACITY);
                    held[0] = boiler(helper).data().get(BoilerBlockEntity.DATA_FUEL);
                    held[1] = boiler(helper).data().get(BoilerBlockEntity.DATA_WATER);
                    held[2] = boiler(helper).getItem(BoilerSlots.FUEL).getCount();
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    BoilerBlockEntity boiler = boiler(helper);
                    if (boiler.data().get(BoilerBlockEntity.DATA_FUEL) != held[0]
                            || boiler.getItem(BoilerSlots.FUEL).getCount() != held[2]) {
                        helper.fail("a blocked Boiler spent fuel", ANCHOR);
                    }
                    if (boiler.data().get(BoilerBlockEntity.DATA_WATER) != held[1]) {
                        helper.fail("a blocked Boiler spent water", ANCHOR);
                    }
                    expectStatus(helper, BoilerStatus.STEAM_BACKED_UP);
                    if (extract(fluidAt(helper, part(helper, STEAM_PART), Direction.SOUTH), steam(), 30) != 30) {
                        helper.fail("a pipe could not draw 30 mB from the full steam tank", ANCHOR);
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    if (boiler(helper).data().get(BoilerBlockEntity.DATA_WATER) >= held[1]) {
                        helper.fail("the Boiler did not resume once its steam was drawn", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /**
     * The front row's two ends open outward onto the water tank, the back middle opens backward onto
     * the steam tank, and the anchor and the back corners open onto nothing. The null side answers
     * the port blocks only.
     */
    private static void portsOpenWhereTheyShould(GameTestHelper helper) {
        place(helper);
        Direction[] sides = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        expectOpenOn(helper, "the west end", part(helper, WEST_END), sides, Direction.WEST);
        expectOpenOn(helper, "the east end", part(helper, EAST_END), sides, Direction.EAST);
        expectOpenOn(helper, "the steam port", part(helper, STEAM_PART), sides, Direction.SOUTH);
        for (int corner : new int[] {BACK_WEST, BACK_EAST}) {
            expectOpenOn(helper, "back corner " + corner, part(helper, corner), sides, null);
        }
        expectOpenOn(helper, "the anchor", helper.absolutePos(ANCHOR), sides, null);

        ResourceHandler<FluidResource> west = fluidAt(helper, part(helper, WEST_END), Direction.WEST);
        ResourceHandler<FluidResource> east = fluidAt(helper, part(helper, EAST_END), Direction.EAST);
        ResourceHandler<FluidResource> steamPort = fluidAt(helper, part(helper, STEAM_PART), Direction.SOUTH);
        if (west.getCapacityAsInt(0, water()) != WATER_CAPACITY || steamPort.getCapacityAsInt(0, steam()) != STEAM_CAPACITY) {
            helper.fail("the water row holds " + west.getCapacityAsInt(0, water()) + " mB and the steam port "
                    + steamPort.getCapacityAsInt(0, steam()) + " mB, not " + WATER_CAPACITY + " and " + STEAM_CAPACITY, ANCHOR);
        }
        insert(west, water(), 100);
        if (east.getAmountAsInt(0) != 100) {
            helper.fail("water put in the west end does not reach the east end", ANCHOR);
        }
        if (steamPort.getAmountAsInt(0) != 0) {
            helper.fail("water put in the row reached the steam port", ANCHOR);
        }
        helper.succeed();
    }

    /** The ports turn with the Boiler: water out of each end of the front row, steam out of the back. */
    private static void portsFollowTheFacing(GameTestHelper helper) {
        Direction[] sides = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        BlockPos anchor = new BlockPos(10, 1, 6);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> blocks = Network.footprint(helper, WireworksRegistries.BOILER_FOOTPRINT, anchor, facing);
            expectOpenOn(helper, facing + " west end", blocks.get(WEST_END), sides, facing.getCounterClockWise());
            expectOpenOn(helper, facing + " east end", blocks.get(EAST_END), sides, facing.getClockWise());
            expectOpenOn(helper, facing + " steam port", blocks.get(STEAM_PART), sides, facing.getOpposite());
            expectOpenOn(helper, facing + " back west", blocks.get(BACK_WEST), sides, null);
            expectOpenOn(helper, facing + " back east", blocks.get(BACK_EAST), sides, null);
            expectOpenOn(helper, facing + " anchor", blocks.get(0), sides, null);
            for (BlockPos block : blocks) {
                helper.getLevel().setBlock(block, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
        helper.succeed();
    }

    /** @param open the one side a port block answers on besides the null side, or null for a block that answers on none */
    private static void expectOpenOn(GameTestHelper helper, String name, BlockPos pos, Direction[] sides, Direction open) {
        for (Direction side : sides) {
            boolean answers = fluidAt(helper, pos, side) != null;
            boolean expected = open != null && (side == null || side == open);
            if (answers != expected) {
                helper.fail(name + (answers ? " answers" : " does not answer") + " the fluid capability on side "
                        + side + ", expected the opposite", helper.relativePos(pos));
            }
        }
    }

    private static void waterPortTakesWaterOnly(GameTestHelper helper) {
        place(helper);
        ResourceHandler<FluidResource> west = fluidAt(helper, part(helper, WEST_END), Direction.WEST);
        if (insert(west, steam(), 100) != 0) {
            helper.fail("the water port took steam", ANCHOR);
        }
        if (insert(west, water(), 100) != 100) {
            helper.fail("the water port refused water", ANCHOR);
        }
        if (extract(fluidAt(helper, part(helper, EAST_END), Direction.EAST), water(), 40) != 40) {
            helper.fail("water does not pass through the row to its other end", ANCHOR);
        }
        helper.succeed();
    }

    private static void steamPortGivesSteamOnly(GameTestHelper helper) {
        place(helper);
        ResourceHandler<FluidResource> steamPort = fluidAt(helper, part(helper, STEAM_PART), Direction.SOUTH);
        if (insert(steamPort, steam(), 100) != 0 || insert(steamPort, water(), 100) != 0) {
            helper.fail("the steam port took a fluid in", ANCHOR);
        }
        helper.startSequence()
                .thenExecute(() -> {
                    boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 1));
                    insert(fluidAt(helper, part(helper, WEST_END), null), water(), 100);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    if (extract(steamPort, steam(), 5) != 5) {
                        helper.fail("a pipe could not draw the steam the Boiler made", ANCHOR);
                    }
                    if (extract(steamPort, water(), 5) != 0) {
                        helper.fail("the steam port gave water", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** Fuel in and nothing out, whether or not a caller names the slot. */
    private static void itemFaceTakesFuelOnly(GameTestHelper helper) {
        place(helper);
        ItemResource coal = ItemResource.of(new ItemStack(Items.COAL));
        ResourceHandler<ItemResource> face =
                helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(ANCHOR), null);
        if (face == null) {
            helper.fail("the Boiler has no item capability", ANCHOR);
            return;
        }
        int in;
        try (Transaction transaction = Transaction.openRoot()) {
            in = face.insert(coal, 1, transaction);
            transaction.commit();
        }
        if (in != 1 || boiler(helper).getItem(BoilerSlots.FUEL).getCount() != 1) {
            helper.fail("the coal did not reach the fuel slot", ANCHOR);
        }
        int bySlot;
        int anywhere;
        try (Transaction transaction = Transaction.openRoot()) {
            bySlot = face.extract(BoilerSlots.FUEL, coal, 1, transaction);
            anywhere = face.extract(coal, 1, transaction);
        }
        if (bySlot != 0 || anywhere != 0) {
            helper.fail("a funnel pulled " + (bySlot + anywhere) + " coal back out of the Boiler (by slot " + bySlot
                    + ", slot-less " + anywhere + ")", ANCHOR);
        }
        if (face.insert(ItemResource.of(new ItemStack(Items.STONE)), 1, null) != 0) {
            helper.fail("the Boiler took an item that is not fuel", ANCHOR);
        }
        helper.succeed();
    }

    /** A hopper on any part finds the anchor's fuel slot. */
    private static void partsReachTheFuelSlot(GameTestHelper helper) {
        place(helper);
        ItemResource coal = ItemResource.of(new ItemStack(Items.COAL));
        List<BlockPos> positions = WireworksRegistries.BOILER_FOOTPRINT.positions(helper.absolutePos(ANCHOR), FACING);
        for (int i = 1; i < positions.size(); i++) {
            ResourceHandler<ItemResource> face =
                    helper.getLevel().getCapability(Capabilities.Item.BLOCK, positions.get(i), null);
            if (face == null) {
                helper.fail("part " + i + " has no item face", helper.relativePos(positions.get(i)));
                return;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                if (face.insert(coal, 1, transaction) <= 0) {
                    helper.fail("part " + i + " did not reach the anchor's fuel slot", helper.relativePos(positions.get(i)));
                }
            }
        }
        helper.succeed();
    }

    /**
     * Real hoppers, on a Boiler raised a block: one above the anchor pushes coal in, and one under the
     * anchor and one under a back corner each pull buckets out of the output and none of the coal. The anchor's is the one that reaches the
     * block entity itself, a vanilla container, rather than the item face. With no water the Boiler
     * burns nothing, so the coal it holds at the end is all the coal it was given.
     */
    private static void hoppers(GameTestHelper helper) {
        BlockPos raised = ANCHOR.above();
        Network.footprint(helper, WireworksRegistries.BOILER_FOOTPRINT, raised, FACING);
        BlockPos above = raised.above();
        BlockPos underAnchor = ANCHOR;
        // Facing north, the back row is one south of the front.
        BlockPos underCorner = ANCHOR.offset(-1, 0, 1);
        helper.assertBlockPresent(WireworksRegistries.BOILER_PART.get(), underCorner.above());
        for (BlockPos hopper : List.of(above, underAnchor, underCorner)) {
            helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState());
        }
        BoilerBlockEntity boiler = helper.getBlockEntity(raised, BoilerBlockEntity.class);
        helper.getBlockEntity(above, HopperBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 1));
        boiler.setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 4));
        boiler.setItem(BoilerSlots.OUTPUT, new ItemStack(Items.BUCKET, 4));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(boiler.getItem(BoilerSlots.OUTPUT).isEmpty(), "the hoppers have not taken the buckets yet");
                    helper.assertValueEqual(boiler.getItem(BoilerSlots.FUEL).getCount(), 5, "coal in the boiler");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertValueEqual(boiler.getItem(BoilerSlots.FUEL).getCount(), 5, "coal in the boiler after the hoppers ran on");
                    for (BlockPos under : List.of(underAnchor, underCorner)) {
                        HopperBlockEntity hopper = helper.getBlockEntity(under, HopperBlockEntity.class);
                        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
                            helper.assertFalse(hopper.getItem(slot).is(Items.COAL), "a hopper at " + under + " pulled coal out");
                        }
                        helper.assertTrue(hopper.countItem(Items.BUCKET) > 0, "the hopper at " + under + " pulled no bucket");
                    }
                })
                .thenSucceed();
    }

    /** A pipe draws the output whether or not it names the slot, and can put nothing into it. */
    private static void itemFaceGivesTheOutput(GameTestHelper helper) {
        place(helper);
        boiler(helper).setItem(BoilerSlots.OUTPUT, new ItemStack(Items.BUCKET, 2));
        ItemResource bucket = ItemResource.of(new ItemStack(Items.BUCKET));
        ResourceHandler<ItemResource> face =
                helper.getLevel().getCapability(Capabilities.Item.BLOCK, part(helper, BACK_EAST), null);
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(face.insert(BoilerSlots.OUTPUT, bucket, 1, transaction), 0, "buckets put in the output by slot");
            helper.assertValueEqual(face.extract(BoilerSlots.OUTPUT, bucket, 1, transaction), 1, "buckets drawn from the output by slot");
            helper.assertValueEqual(face.extract(bucket, 1, transaction), 1, "buckets drawn slot-less");
            transaction.commit();
        }
        helper.assertTrue(boiler(helper).getItem(BoilerSlots.OUTPUT).isEmpty(), "the output still holds a bucket");
        helper.succeed();
    }

    /** A lava bucket burns, and its empty bucket lands in the output, leaving the fuel slot free for the next fuel. */
    private static void remainderGoesToTheOutput(GameTestHelper helper) {
        place(helper);
        boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.LAVA_BUCKET));
        insert(fluidAt(helper, part(helper, WEST_END), null), water(), WATER_CAPACITY);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(boiler(helper).getItem(BoilerSlots.OUTPUT).is(Items.BUCKET),
                        "the empty bucket has not reached the output"))
                .thenExecute(() -> helper.assertTrue(boiler(helper).getItem(BoilerSlots.FUEL).isEmpty(),
                        "the fuel slot still holds " + boiler(helper).getItem(BoilerSlots.FUEL)))
                .thenSucceed();
    }

    /**
     * An output the remainder cannot join stops the Boiler before it lights the fuel, as a full steam
     * tank does: nothing is burnt and nothing is voided.
     */
    private static void stallsOnAFullOutput(GameTestHelper helper, ItemStack blocking) {
        place(helper);
        boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.LAVA_BUCKET));
        boiler(helper).setItem(BoilerSlots.OUTPUT, blocking);
        insert(fluidAt(helper, part(helper, WEST_END), null), water(), WATER_CAPACITY);
        helper.startSequence()
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    helper.assertTrue(boiler(helper).getItem(BoilerSlots.FUEL).is(Items.LAVA_BUCKET), "the lava bucket was lit");
                    helper.assertValueEqual(boiler(helper).data().get(BoilerBlockEntity.DATA_STEAM), 0, "steam made");
                    expectStatus(helper, BoilerStatus.OUTPUT_FULL);
                    boiler(helper).setItem(BoilerSlots.OUTPUT, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> helper.assertTrue(boiler(helper).getItem(BoilerSlots.OUTPUT).is(Items.BUCKET),
                        "the Boiler did not resume once its output was emptied"))
                .thenSucceed();
    }

    /** Shift-clicking a bucket out of the inventory leaves the output's bucket as it was: only the Boiler fills it. */
    private static void shiftClickSkipsTheOutput(GameTestHelper helper) {
        place(helper);
        boiler(helper).setItem(BoilerSlots.OUTPUT, new ItemStack(Items.BUCKET));
        ListeningPlayer player = new ListeningPlayer(helper, ANCHOR.above());
        player.getInventory().setItem(0, new ItemStack(Items.BUCKET, 15));
        BoilerMenu menu = new BoilerMenu(0, player.getInventory(), boiler(helper), boiler(helper).data());
        int hotbarFirst = BoilerSlots.SIZE + 27;
        menu.quickMoveStack(player, hotbarFirst);
        helper.assertValueEqual(boiler(helper).getItem(BoilerSlots.OUTPUT).getCount(), 1, "buckets in the output");
        helper.succeed();
    }

    /** Fuel in the fuel slot, a water container in the water slot, an empty one in the steam slot, and nothing in the output. */
    private static void slotsTakeWhatTheyAreFor(GameTestHelper helper) {
        place(helper);
        BoilerBlockEntity boiler = boiler(helper);
        Object[][] cases = {
                {Items.COAL, BoilerSlots.FUEL},
                {Items.LAVA_BUCKET, BoilerSlots.FUEL},
                {Items.WATER_BUCKET, BoilerSlots.WATER},
                {Items.BUCKET, BoilerSlots.STEAM},
                {WireworksRegistries.STEAM_BUCKET.get(), -1},
                {Items.STONE, -1},
        };
        for (Object[] each : cases) {
            ItemStack stack = new ItemStack((Item) each[0]);
            for (int slot = 0; slot < BoilerSlots.SIZE; slot++) {
                boolean expected = slot == (int) each[1];
                if (boiler.canPlaceItem(slot, stack) != expected) {
                    helper.fail("slot " + slot + (expected ? " refuses " : " takes ") + stack, ANCHOR);
                }
            }
        }
        helper.succeed();
    }

    /** A pipe that names no slot puts each bucket where it goes, and draws neither back out. */
    private static void itemFaceRoutesBuckets(GameTestHelper helper) {
        place(helper);
        ResourceHandler<ItemResource> face =
                helper.getLevel().getCapability(Capabilities.Item.BLOCK, part(helper, BACK_WEST), null);
        ItemResource waterBucket = ItemResource.of(new ItemStack(Items.WATER_BUCKET));
        ItemResource bucket = ItemResource.of(new ItemStack(Items.BUCKET));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(face.insert(waterBucket, 1, transaction), 1, "water buckets put in");
            helper.assertValueEqual(face.insert(bucket, 2, transaction), 2, "empty buckets put in");
            transaction.commit();
        }
        BoilerBlockEntity boiler = boiler(helper);
        helper.assertTrue(boiler.getItem(BoilerSlots.WATER).is(Items.WATER_BUCKET), "the water bucket is not in the water slot");
        helper.assertValueEqual(boiler.getItem(BoilerSlots.STEAM).getCount(), 2, "empty buckets in the steam slot");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(face.extract(BoilerSlots.WATER, waterBucket, 1, transaction), 0, "water buckets drawn by slot");
            helper.assertValueEqual(face.extract(BoilerSlots.STEAM, bucket, 1, transaction), 0, "empty buckets drawn by slot");
            helper.assertValueEqual(face.extract(bucket, 1, transaction), 0, "empty buckets drawn slot-less");
        }
        helper.succeed();
    }

    private static void emptiesAWaterBucket(GameTestHelper helper) {
        place(helper);
        boiler(helper).setItem(BoilerSlots.WATER, new ItemStack(Items.WATER_BUCKET));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(boiler(helper).data().get(BoilerBlockEntity.DATA_WATER), 1_000, "water in the tank"))
                .thenExecute(() -> {
                    helper.assertTrue(boiler(helper).getItem(BoilerSlots.WATER).isEmpty(), "the water slot still holds a bucket");
                    helper.assertTrue(boiler(helper).getItem(BoilerSlots.OUTPUT).is(Items.BUCKET), "the empty bucket is not in the output");
                })
                .thenSucceed();
    }

    /** A bucket empties whole or not at all: with 1,500 mB in a 2,000 mB tank, it waits. */
    private static void holdsAWaterBucketWithoutRoom(GameTestHelper helper) {
        place(helper);
        insert(fluidAt(helper, part(helper, WEST_END), null), water(), 1_500);
        boiler(helper).setItem(BoilerSlots.WATER, new ItemStack(Items.WATER_BUCKET));
        helper.startSequence()
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    helper.assertValueEqual(boiler(helper).data().get(BoilerBlockEntity.DATA_WATER), 1_500, "water in the tank");
                    helper.assertTrue(boiler(helper).getItem(BoilerSlots.WATER).is(Items.WATER_BUCKET), "the water bucket left its slot");
                    helper.assertTrue(boiler(helper).getItem(BoilerSlots.OUTPUT).isEmpty(), "something reached the output");
                })
                .thenSucceed();
    }

    private static void holdsAWaterBucketWithoutOutputRoom(GameTestHelper helper) {
        place(helper);
        boiler(helper).setItem(BoilerSlots.WATER, new ItemStack(Items.WATER_BUCKET));
        boiler(helper).setItem(BoilerSlots.OUTPUT, new ItemStack(Items.STONE));
        helper.startSequence()
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    helper.assertValueEqual(boiler(helper).data().get(BoilerBlockEntity.DATA_WATER), 0, "water in the tank");
                    helper.assertTrue(boiler(helper).getItem(BoilerSlots.WATER).is(Items.WATER_BUCKET), "the water bucket left its slot");
                    // Not "no water": the water is there, waiting on the output.
                    expectStatus(helper, BoilerStatus.OUTPUT_FULL);
                })
                .thenSucceed();
    }

    /** Once the steam tank holds a bucket's worth, one empty bucket fills and goes to the output; the other waits. */
    private static void fillsABucketWithSteam(GameTestHelper helper) {
        place(helper);
        boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 10));
        boiler(helper).setItem(BoilerSlots.STEAM, new ItemStack(Items.BUCKET, 2));
        insert(fluidAt(helper, part(helper, WEST_END), null), water(), WATER_CAPACITY);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(boiler(helper).getItem(BoilerSlots.OUTPUT).is(WireworksRegistries.STEAM_BUCKET.get()),
                        "no steam bucket in the output yet"))
                .thenExecute(() -> {
                    helper.assertValueEqual(boiler(helper).getItem(BoilerSlots.STEAM).getCount(), 1, "empty buckets left in the steam slot");
                    helper.assertTrue(boiler(helper).data().get(BoilerBlockEntity.DATA_STEAM) < 1_000, "the steam tank kept the bucket's steam");
                })
                .thenSucceed();
    }

    /**
     * The status the screen and Jade name: no water first, then fuel, then boiling. The backed-up and
     * output-full states are asserted where those stalls are set up.
     */
    private static void saysWhy(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    expectStatus(helper, BoilerStatus.NO_WATER);
                    insert(fluidAt(helper, part(helper, WEST_END), null), water(), WATER_CAPACITY);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    expectStatus(helper, BoilerStatus.NO_FUEL);
                    boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL));
                })
                .thenIdle(1)
                .thenExecute(() -> expectStatus(helper, BoilerStatus.BOILING))
                .thenSucceed();
    }

    private static void expectStatus(GameTestHelper helper, BoilerStatus expected) {
        BoilerStatus status = boiler(helper).status();
        if (status != expected) {
            helper.fail("the Boiler says " + status + ", not " + expected, ANCHOR);
        }
        int synced = boiler(helper).data().get(BoilerBlockEntity.DATA_STATUS);
        if (synced != expected.ordinal()) {
            helper.fail("the menu is sent status " + synced + ", not " + expected.ordinal(), ANCHOR);
        }
    }

    /** The ghosts the screen draws in each empty input slot are what that slot takes; the output has none. */
    private static void ghostsShowWhatSlotsTake(GameTestHelper helper) {
        place(helper);
        ListeningPlayer player = new ListeningPlayer(helper, ANCHOR.above());
        BoilerMenu menu = new BoilerMenu(0, player.getInventory(), boiler(helper), boiler(helper).data());
        Object[][] cases = {
                {BoilerSlots.FUEL, Items.COAL, Items.WATER_BUCKET},
                {BoilerSlots.WATER, Items.WATER_BUCKET, Items.BUCKET},
                {BoilerSlots.STEAM, Items.BUCKET, Items.COAL},
        };
        for (Object[] each : cases) {
            List<ItemStack> ghosts = Ghosts.accepted(menu.slots.get((int) each[0]));
            helper.assertTrue(ghosts.stream().anyMatch(stack -> stack.is((Item) each[1])), "slot " + each[0] + "'s ghosts lack " + each[1]);
            helper.assertFalse(ghosts.stream().anyMatch(stack -> stack.is((Item) each[2])), "slot " + each[0] + "'s ghosts show " + each[2]);
        }
        helper.assertTrue(Ghosts.accepted(menu.slots.get(BoilerSlots.OUTPUT)).isEmpty(), "the output shows ghosts");
        helper.succeed();
    }

    /** Shift-click sends a water bucket to the water slot and an empty bucket to the steam slot. */
    private static void shiftClickSendsContainersHome(GameTestHelper helper) {
        place(helper);
        ListeningPlayer player = new ListeningPlayer(helper, ANCHOR.above());
        player.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        player.getInventory().setItem(1, new ItemStack(Items.BUCKET, 3));
        BoilerMenu menu = new BoilerMenu(0, player.getInventory(), boiler(helper), boiler(helper).data());
        int hotbarFirst = BoilerSlots.SIZE + 27;
        menu.quickMoveStack(player, hotbarFirst);
        menu.quickMoveStack(player, hotbarFirst + 1);
        helper.assertTrue(boiler(helper).getItem(BoilerSlots.WATER).is(Items.WATER_BUCKET), "the water bucket is not in the water slot");
        helper.assertValueEqual(boiler(helper).getItem(BoilerSlots.STEAM).getCount(), 3, "empty buckets in the steam slot");
        helper.assertTrue(boiler(helper).getItem(BoilerSlots.OUTPUT).isEmpty(), "something reached the output");
        helper.succeed();
    }

    /** The Boiler hands its steam to the Engine standing against its steam port, with no pipe between. */
    private static void feedsAnEngine(GameTestHelper helper) {
        place(helper);
        BlockPos engine = ANCHOR.offset(0, 0, 2);
        Network.footprint(helper, WireworksRegistries.STEAM_ENGINE_FOOTPRINT, engine, FACING);
        helper.startSequence()
                .thenExecute(() -> {
                    boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 4));
                    insert(fluidAt(helper, part(helper, WEST_END), null), water(), WATER_CAPACITY);
                })
                .thenWaitUntil(() -> {
                    if (helper.getBlockEntity(engine, SteamEngineBlockEntity.class).energy().getAmountAsLong() < 450L) {
                        helper.fail("the engine has not made its 450 FE yet", engine);
                    }
                })
                .thenSucceed();
    }
}
