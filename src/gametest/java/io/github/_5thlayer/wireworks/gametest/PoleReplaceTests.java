// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github._5thlayer.groundworks.FastReplace;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.wireworks.LevelWires;
import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.PoleColumnReplace;
import io.github._5thlayer.wireworks.PoleNetworks;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlock;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;
import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksRefusal;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pole column's Fast Replace through {@link PoleColumnReplace}: in Wireworks' default group, which
 * holds the three tiers and not the creative pole, and in a group a pack states, which claims its
 * tiers before the default does. Each test clicks through the player's game mode, where Groundworks
 * takes the click, and holds the world, the wires and the inventory to the plan.
 */
final class PoleReplaceTests {

    private static final BlockPos ABOVE_FLOOR = new BlockPos(3, 1, 3);
    /** Within a small pole's 7.5 of the column's base, so the two are wired on placement. */
    private static final BlockPos NEIGHBOUR = ABOVE_FLOOR.east(5);
    /**
     * Nearer the base than {@link #NEIGHBOUR}, but wired only to it, since the two ends of a placed
     * pole's wires never share a neighbour. A base wired afresh would take it first.
     */
    private static final BlockPos BESIDE = ABOVE_FLOOR.east(3);
    private static final BlockPos STAND = new BlockPos(3, 1, 1);
    private static final String OTHER_TIER_KEY = "message.wireworks.other_tier";
    private static final String NO_ROOM_KEY = "message.groundworks.fast_replace_no_room_to_return";

    /** Read on the server thread only, where a test's body runs whole. */
    private static boolean packGrouped;

    private PoleReplaceTests() {
    }

    /**
     * States the group a pack would, small and medium with the large pole left out, as the
     * FactoryWorks Pack's is: at mod construction, so before Wireworks' default. It claims only
     * while a test holds {@link #packGrouped}, so every other test meets the default.
     */
    static void statePackGroup() {
        FastReplace.group(Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "gametest_pack_poles"),
                block -> packGrouped && (block == pole(PoleTier.SMALL) || block == pole(PoleTier.MEDIUM)),
                PoleColumnReplace.BUILDER);
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("replace_small_pole_column_with_medium", 20,
                helper -> replaces(helper, pole(PoleTier.SMALL), pole(PoleTier.MEDIUM), 1));
        tests.test("replace_medium_pole_column_with_small", 20,
                helper -> replaces(helper, pole(PoleTier.MEDIUM), pole(PoleTier.SMALL), 0));
        tests.test("replace_small_pole_column_with_medium_at_its_top", 20,
                helper -> replaces(helper, pole(PoleTier.SMALL), pole(PoleTier.MEDIUM), 2));
        tests.test("replace_small_pole_column_with_large", 20,
                helper -> replaces(helper, pole(PoleTier.SMALL), pole(PoleTier.LARGE), 2));
        tests.test("replace_medium_pole_column_with_large", 20,
                helper -> replaces(helper, pole(PoleTier.MEDIUM), pole(PoleTier.LARGE), 1));
        tests.test("replace_large_pole_column_with_small", 20,
                helper -> replaces(helper, pole(PoleTier.LARGE), pole(PoleTier.SMALL), 0));
        tests.test("replace_pole_refuses_a_medium_pole_on_a_creative_column", 20,
                helper -> refuses(helper, creativePole(), 1, pole(PoleTier.MEDIUM), 0, false,
                        WireworksRefusal.OTHER_TIER, OTHER_TIER_KEY));
        tests.test("replace_pole_refuses_a_creative_pole_on_a_small_column", 20,
                helper -> refuses(helper, pole(PoleTier.SMALL), 3, creativePole(), 2, false,
                        WireworksRefusal.OTHER_TIER, OTHER_TIER_KEY));
        tests.test("replace_pole_refused_with_no_room_changes_nothing", 20,
                helper -> refuses(helper, pole(PoleTier.SMALL), 3, pole(PoleTier.MEDIUM), 2, true,
                        Refusal.FastReplace.NO_ROOM_TO_RETURN, NO_ROOM_KEY));
        tests.test("replace_pole_in_a_pack_group_replaces_small_with_medium", 20,
                helper -> packGrouped(() -> replaces(helper, pole(PoleTier.SMALL), pole(PoleTier.MEDIUM), 1)));
        tests.test("replace_pole_in_a_pack_group_refuses_a_large_pole_on_a_small_column", 20,
                helper -> packGrouped(() -> refuses(helper, pole(PoleTier.SMALL), 3, pole(PoleTier.LARGE), 2,
                        false, WireworksRefusal.OTHER_TIER, OTHER_TIER_KEY)));
    }

    /** Runs a test's body, which plans and clicks within the tick, with the pack's group stated. */
    private static void packGrouped(Runnable body) {
        packGrouped = true;
        try {
            body.run();
        } finally {
            packGrouped = false;
        }
    }

    /** A three-segment column of {@code from}, clicked at segment {@code aimed} with {@code to}. */
    private static void replaces(GameTestHelper helper, SupplyAreaPoleBlock from, SupplyAreaPoleBlock to, int aimed) {
        standing(helper, from, 3);
        BlockPos base = helper.absolutePos(ABOVE_FLOOR);
        Set<PoleNetworks.Wire> wires = wires(helper);
        LevelWires levelWires = LevelWires.of(helper.getLevel());
        if (!levelWires.contains(base, helper.absolutePos(NEIGHBOUR))
                || levelWires.contains(base, helper.absolutePos(BESIDE))) {
            helper.fail("the fixture was not wired base to neighbour only", ABOVE_FLOOR);
        }
        ListeningPlayer player = new ListeningPlayer(helper, STAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(to, 2));
        BlockPos target = ABOVE_FLOOR.above(aimed);
        BlockHitResult hit = hit(helper, target);
        PlacementPlan plan = plan(helper, player, hit, target);

        List<BlockPos> column = List.of(base, base.above(), base.above(2));
        BlockState segment = to.defaultBlockState();
        List<PlacementPlan.Placed> expected = column.stream().map(pos -> new PlacementPlan.Placed(pos, segment)).toList();
        if (plan.isRefused() || !plan.replaces().equals(column) || !plan.blocks().equals(expected)) {
            helper.fail("the plan was not a replace of the whole column by " + to + ": " + plan, target);
        }
        BlockState above = helper.getBlockState(ABOVE_FLOOR.above(3));

        click(helper, player, hit);

        for (PlacementPlan.Placed placed : plan.blocks()) {
            if (!helper.getLevel().getBlockState(placed.pos()).equals(placed.state())) {
                helper.fail("the replace left " + helper.getLevel().getBlockState(placed.pos()),
                        helper.relativePos(placed.pos()));
            }
        }
        if (!helper.getBlockState(ABOVE_FLOOR.above(3)).equals(above)) {
            helper.fail("the replace changed the height of the column", ABOVE_FLOOR.above(3));
        }
        if (!wires(helper).equals(wires)) {
            helper.fail("the replace changed the wires from " + wires + " to " + wires(helper), ABOVE_FLOOR);
        }
        if (helper.getBlockEntity(ABOVE_FLOOR, SupplyAreaPoleBlockEntity.class).tier() != to.tier()) {
            helper.fail("the column's base is not a " + to + " pole", ABOVE_FLOOR);
        }
        ItemStack hand = player.getMainHandItem();
        if (!hand.is(to.asItem()) || hand.getCount() != 1) {
            helper.fail("the hand holds " + hand + " where one " + to + " pole should be left", target);
        }
        int returned = player.getInventory().countItem(from.asItem());
        if (returned != 1) {
            helper.fail(returned + " " + from + " poles came back, not one", target);
        }
        helper.succeed();
    }

    private static void refuses(GameTestHelper helper, Block standing, int height, Block held, int aimed,
                                boolean full, Refusal expected, String reason) {
        standing(helper, standing, height);
        ListeningPlayer player = new ListeningPlayer(helper, STAND);
        if (full) {
            for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            }
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(held, 2));
        BlockPos target = ABOVE_FLOOR.above(aimed);
        BlockHitResult hit = hit(helper, target);
        PlacementPlan plan = plan(helper, player, hit, target);
        if (plan.refusal() != expected || plan.isReplace() != (expected == Refusal.FastReplace.NO_ROOM_TO_RETURN)) {
            helper.fail("expected the refusal " + expected + " but the plan was " + plan, target);
        }
        Map<BlockPos, BlockState> world = world(helper);
        Set<PoleNetworks.Wire> wires = wires(helper);
        List<ItemStack> inventory = inventory(player);

        click(helper, player, hit);

        if (!world(helper).equals(world) || !wires(helper).equals(wires)
                || !ItemStack.listMatches(inventory(player), inventory)) {
            helper.fail("a refused replace changed the world, the wires or the inventory", target);
        }
        if (!player.heard.contains(reason)) {
            helper.fail("a refused replace did not name " + reason + " on the action bar, only " + player.heard, target);
        }
        helper.succeed();
    }

    /** A column of {@code pole} on the floor, a small pole wired to its base, and one wired past it. */
    private static void standing(GameTestHelper helper, Block pole, int height) {
        for (int i = 0; i < height; i++) {
            helper.setBlock(ABOVE_FLOOR.above(i), pole);
        }
        helper.setBlock(NEIGHBOUR, pole(PoleTier.SMALL));
        helper.setBlock(BESIDE, pole(PoleTier.SMALL));
    }

    private static PlacementPlan plan(GameTestHelper helper, ListeningPlayer player, BlockHitResult hit, BlockPos target) {
        PlacementPlan plan = Placements.planFor(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), hit);
        if (plan == null) {
            helper.fail("no plan at all where one was expected", target);
            throw new IllegalStateException("unreachable");
        }
        return plan;
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockHitResult hit) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false);
    }

    /** The column's positions, one above it, and the block north of each, where a fall-through would place. */
    private static Map<BlockPos, BlockState> world(GameTestHelper helper) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (int i = 0; i <= PoleColumn.MAX_SEGMENTS; i++) {
            BlockPos pos = ABOVE_FLOOR.above(i);
            states.put(pos, helper.getBlockState(pos));
            states.put(pos.north(), helper.getBlockState(pos.north()));
        }
        return states;
    }

    /** The wires with an end in this fixture: the level's are shared with every test beside it. */
    private static Set<PoleNetworks.Wire> wires(GameTestHelper helper) {
        Set<PoleNetworks.Pos> ends = new HashSet<>();
        for (BlockPos pos : List.of(ABOVE_FLOOR, NEIGHBOUR, BESIDE)) {
            BlockPos absolute = helper.absolutePos(pos);
            ends.add(new PoleNetworks.Pos(absolute.getX(), absolute.getY(), absolute.getZ()));
        }
        return LevelWires.of(helper.getLevel()).wires().all().stream()
                .filter(wire -> ends.contains(wire.a()) || ends.contains(wire.b()))
                .collect(Collectors.toSet());
    }

    private static List<ItemStack> inventory(ListeningPlayer player) {
        return player.getInventory().getNonEquipmentItems().stream().map(ItemStack::copy).toList();
    }

    private static SupplyAreaPoleBlock pole(PoleTier tier) {
        return WireworksRegistries.pole(tier).get();
    }

    private static SupplyAreaPoleBlock creativePole() {
        return WireworksRegistries.CREATIVE_POLE.get();
    }
}
