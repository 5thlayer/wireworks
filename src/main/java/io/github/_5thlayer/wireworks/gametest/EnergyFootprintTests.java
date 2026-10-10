// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * The Solar Panel, the Accumulator, the Steam Engine and the Boiler as footprints: each placed whole from its one item, broken as
 * one with one item back whichever block is hit, and, where they hold energy, answering the energy
 * capability on every block with the anchor's buffer.
 *
 * <p>The player stands west of the anchor looking east and clicks the floor under it, so the
 * footprint faces west.
 */
final class EnergyFootprintTests {

    private static final BlockPos ANCHOR = new BlockPos(5, 1, 3);
    private static final Direction PLACED_FACING = Direction.WEST;

    private EnergyFootprintTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        for (Kind kind : List.of(solar(), accumulator(), steamEngine(), boiler())) {
            tests.test(kind.name + "_is_placed_whole_from_its_item", 20, helper -> placedWhole(helper, kind));
            tests.test(kind.name + "_breaks_as_one_when_a_part_is_broken", 20,
                    helper -> brokenWhole(helper, kind, kind.lastPart(helper)));
            tests.test(kind.name + "_breaks_as_one_when_its_anchor_is_broken", 20,
                    helper -> brokenWhole(helper, kind, helper.absolutePos(ANCHOR)));
            if (kind.energy) {
                tests.test(kind.name + "_answers_the_energy_capability_on_every_block_with_the_anchors_buffer", 20,
                        helper -> sharesItsBuffer(helper, kind));
            }
        }
    }

    private record Kind(String name, Footprint footprint, Item item, int blocks, boolean energy) {

        BlockPos lastPart(GameTestHelper helper) {
            return footprint.positions(helper.absolutePos(ANCHOR), PLACED_FACING).getLast();
        }
    }

    private static Kind solar() {
        return new Kind("solar_panel", WireworksRegistries.SOLAR_PANEL_FOOTPRINT,
                WireworksRegistries.SOLAR_PANEL_ITEM.get(), 10, true);
    }

    private static Kind accumulator() {
        return new Kind("accumulator", WireworksRegistries.ACCUMULATOR_FOOTPRINT,
                WireworksRegistries.ACCUMULATOR_ITEM.get(), 4, true);
    }

    private static Kind steamEngine() {
        return new Kind("steam_engine", WireworksRegistries.STEAM_ENGINE_FOOTPRINT,
                WireworksRegistries.STEAM_ENGINE_ITEM.get(), 4, true);
    }

    /** Holds no energy: its footprint is placed and broken as the others are. */
    private static Kind boiler() {
        return new Kind("boiler", WireworksRegistries.BOILER_FOOTPRINT,
                WireworksRegistries.BOILER_ITEM.get(), 6, false);
    }

    private static void placedWhole(GameTestHelper helper, Kind kind) {
        ListeningPlayer player = holding(helper, kind, 2);
        PlacementPlan plan = Placements.planFor(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), onTheFloor(helper));
        if (plan == null || plan.isRefused() || plan.blocks().size() != kind.blocks) {
            helper.fail("the plan was " + plan + ", not the whole footprint of " + kind.blocks + " blocks accepted");
        }
        click(helper, player);
        expectStanding(helper, kind);
        if (player.getMainHandItem().getCount() != 1) {
            helper.fail("the placement charged " + (2 - player.getMainHandItem().getCount()) + " items, not 1");
        }
        helper.succeed();
    }

    private static void brokenWhole(GameTestHelper helper, Kind kind, BlockPos broken) {
        ListeningPlayer player = holding(helper, kind, 1);
        click(helper, player);
        expectStanding(helper, kind);
        player.gameMode.destroyBlock(broken);
        for (BlockPos pos : kind.footprint.positions(helper.absolutePos(ANCHOR), PLACED_FACING)) {
            if (!helper.getLevel().getBlockState(pos).isAir()) {
                helper.fail("the break left " + helper.getLevel().getBlockState(pos) + " at " + pos, ANCHOR);
            }
        }
        List<ItemStack> drops = helper.getEntities(EntityType.ITEM).stream().map(ItemEntity::getItem).toList();
        int dropped = drops.stream().filter(stack -> stack.is(kind.item)).mapToInt(ItemStack::getCount).sum();
        if (dropped != 1 || drops.size() != 1) {
            helper.fail("the break dropped " + dropped + " of its item among " + drops.size() + " stacks, not one item");
        }
        helper.succeed();
    }

    private static void sharesItsBuffer(GameTestHelper helper, Kind kind) {
        ListeningPlayer player = holding(helper, kind, 1);
        click(helper, player);
        expectStanding(helper, kind);
        EnergyHandler anchor = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(ANCHOR), null);
        if (anchor == null) {
            helper.fail("the anchor answers no energy capability", ANCHOR);
            return;
        }
        for (BlockPos pos : kind.footprint.positions(helper.absolutePos(ANCHOR), PLACED_FACING)) {
            for (Direction side : new Direction[] {null, Direction.UP, Direction.NORTH}) {
                EnergyHandler there = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, pos, side);
                if (there != anchor) {
                    helper.fail("the block at " + pos + " answers " + there + " on side " + side
                            + ", not the anchor's buffer", ANCHOR);
                }
            }
        }
        helper.succeed();
    }

    private static void expectStanding(GameTestHelper helper, Kind kind) {
        List<BlockPos> positions = kind.footprint.positions(helper.absolutePos(ANCHOR), PLACED_FACING);
        for (int i = 0; i < positions.size(); i++) {
            BlockState there = helper.getLevel().getBlockState(positions.get(i));
            if (!there.equals(kind.footprint.stateAt(i, PLACED_FACING))) {
                helper.fail("block " + i + " of the footprint at " + positions.get(i) + " is " + there, ANCHOR);
            }
        }
    }

    private static void click(GameTestHelper helper, ListeningPlayer player) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTheFloor(helper));
    }

    private static ListeningPlayer holding(GameTestHelper helper, Kind kind, int count) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 3));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(kind.item, count));
        return player;
    }

    /** The top of the floor block under the anchor. */
    private static BlockHitResult onTheFloor(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(ANCHOR.below());
        return new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false);
    }
}
