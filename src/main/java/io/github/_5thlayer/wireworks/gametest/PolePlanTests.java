// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.WireworksRefusal;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A held pole's Placement Plan against what the click does (FactoryWorks ADR-0069): an accepted plan
 * must put every block down in the state it named, and a refused one must change nothing, read
 * before the click as well as after.
 */
final class PolePlanTests {

    private static final BlockPos FLOOR = new BlockPos(3, 0, 3);
    private static final BlockPos ABOVE_FLOOR = new BlockPos(3, 1, 3);

    private PolePlanTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("plan_matches_placement_for_a_pole", 20, PolePlanTests::matchesPlacement);
        tests.test("plan_extends_a_pole_column", 20, PolePlanTests::extendsColumn);
        tests.test("plan_refuses_a_full_pole_column", 20, PolePlanTests::columnFull);
        tests.test("plan_refuses_a_blocked_pole_top", 20, PolePlanTests::blockedTop);
        tests.test("plan_refuses_another_tier_at_a_pole", 20, PolePlanTests::otherTier);
    }

    private static void matchesPlacement(GameTestHelper helper) {
        check(helper, pole(PoleTier.SMALL), FLOOR, Direction.UP, false);
        helper.succeed();
    }

    private static void extendsColumn(GameTestHelper helper) {
        column(helper, 2);
        PlacementPlan plan = check(helper, pole(PoleTier.SMALL), ABOVE_FLOOR, Direction.NORTH, false);
        if (plan.blocks().getFirst().pos().getY() != helper.absolutePos(ABOVE_FLOOR).getY() + 2) {
            helper.fail("a pole aimed at a column's base planned a segment somewhere other than "
                    + "the top of the column", ABOVE_FLOOR);
        }
        helper.succeed();
    }

    private static void columnFull(GameTestHelper helper) {
        column(helper, PoleColumn.MAX_SEGMENTS);
        refusal(helper, check(helper, pole(PoleTier.SMALL), ABOVE_FLOOR, Direction.NORTH, true),
                WireworksRefusal.COLUMN_FULL);
        helper.succeed();
    }

    private static void blockedTop(GameTestHelper helper) {
        column(helper, 1);
        helper.setBlock(ABOVE_FLOOR.above(), Blocks.STONE);
        refusal(helper, check(helper, pole(PoleTier.SMALL), ABOVE_FLOOR, Direction.NORTH, true),
                WireworksRefusal.BLOCKED_TOP);
        helper.succeed();
    }

    /** Drawn at the column's top, where the player was plainly asking for a segment. */
    private static void otherTier(GameTestHelper helper) {
        column(helper, 3);
        PlacementPlan plan = check(helper, pole(PoleTier.SUBSTATION), ABOVE_FLOOR, Direction.NORTH, true);
        refusal(helper, plan, WireworksRefusal.OTHER_TIER);
        if (plan.blocks().getFirst().pos().getY() != helper.absolutePos(ABOVE_FLOOR).getY() + 3) {
            helper.fail("the other-tier refusal was drawn somewhere other than the top of the "
                    + "column that was aimed at", ABOVE_FLOOR);
        }
        helper.succeed();
    }

    private static PlacementPlan check(GameTestHelper helper, ItemStack stack, BlockPos target,
                                       Direction face, boolean expectRefused) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(target);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);

        PlacementPlan plan = Placements.planFor(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
        if (plan == null) {
            helper.fail("no plan at all where one was expected", target);
            throw new IllegalStateException("unreachable");
        }
        if (plan.isRefused() != expectRefused) {
            helper.fail("the plan " + (plan.isRefused() ? "refused" : "accepted")
                    + " where the opposite was expected", target);
        }
        List<BlockState> before = new ArrayList<>(plan.blocks().size());
        for (PlacementPlan.Placed placed : plan.blocks()) {
            before.add(helper.getLevel().getBlockState(placed.pos()));
        }

        helper.useBlock(target, player, hit);

        for (int i = 0; i < plan.blocks().size(); i++) {
            PlacementPlan.Placed placed = plan.blocks().get(i);
            BlockState now = helper.getLevel().getBlockState(placed.pos());
            BlockPos relative = helper.relativePos(placed.pos());
            if (plan.isRefused()) {
                if (!now.equals(before.get(i))) {
                    helper.fail("a refused plan changed the world at one of its positions", relative);
                }
            } else if (!now.equals(placed.state())) {
                helper.fail("the plan promised " + placed.state() + " but placing left " + now, relative);
            }
        }
        return plan;
    }

    private static void refusal(GameTestHelper helper, PlacementPlan plan, WireworksRefusal expected) {
        if (plan.refusal() != expected) {
            helper.fail("expected the refusal " + expected + " but the plan gave " + plan.refusal(), ABOVE_FLOOR);
        }
    }

    private static void column(GameTestHelper helper, int segments) {
        for (int i = 0; i < segments; i++) {
            helper.setBlock(ABOVE_FLOOR.above(i), WireworksRegistries.pole(PoleTier.SMALL).get());
        }
    }

    private static ItemStack pole(PoleTier tier) {
        return new ItemStack(WireworksRegistries.pole(tier).get());
    }
}
