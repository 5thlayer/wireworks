// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.LevelWires;
import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.PoleNetworks;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.PoleWiring;
import io.github._5thlayer.wireworks.WireSystem;
import io.github._5thlayer.wireworks.WireworksRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Transmission Pole in a running game (ADR 0008): crafted, stacked as a Pole Column, wired only
 * to its own system, and feeding nothing. The rules are in the JVM tests; what they cannot see is
 * the block, its recipe, its loot table and the level's stored wires.
 */
final class TransmissionPoleTests {

    private static final BlockPos A = new BlockPos(1, 1, 3);
    private static final BlockPos B = new BlockPos(21, 1, 3);
    /** A small pole standing beside A: in reach of it, and of the wrong system. */
    private static final BlockPos SMALL = new BlockPos(4, 1, 3);
    private static final BlockPos BASE = new BlockPos(10, 1, 3);

    private static final int SETTLE = Network.SETTLE;

    private TransmissionPoleTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("transmission_pole_is_crafted_from_its_recipe", 1, TransmissionPoleTests::crafted);
        tests.test("transmission_pole_feeds_nothing_in_reach", 120, TransmissionPoleTests::feedsNothing);
        tests.test("transmission_poles_in_reach_wire_and_cut_as_transmission", 20, TransmissionPoleTests::wiresAndCuts);
        tests.test("transmission_pole_and_distribution_pole_are_refused", 20, TransmissionPoleTests::crossSystemRefused);
        tests.test("a_placed_pole_wires_only_inside_its_own_system", 20, TransmissionPoleTests::autoWiresInsideItsSystem);
        tests.test("transmission_pole_stacks_five_and_only_the_base_drops", 60, TransmissionPoleTests::stacksAsAColumn);
        tests.test("transmission_wires_belong_to_the_base_of_a_column", 20, TransmissionPoleTests::wiresAreTheBases);
    }

    private static void crafted(GameTestHelper helper) {
        List<Item> grid = List.of(
                Items.COPPER_INGOT, Items.IRON_BARS, Items.COPPER_INGOT,
                Items.AIR, Items.IRON_INGOT, Items.AIR,
                Items.AIR, Items.IRON_INGOT, Items.AIR);
        CraftingInput input = CraftingInput.of(3, 3, grid.stream().map(ItemStack::new).toList());
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        if (recipe.isEmpty()) {
            helper.fail("no crafting recipe matches CBC / _I_ / _I_");
            return;
        }
        ItemStack result = recipe.get().value().assemble(input);
        if (!result.is(WireworksRegistries.TRANSMISSION_POLE_ITEM.get()) || result.getCount() != 1) {
            helper.fail("the recipe makes " + result + ", not one Transmission Pole");
            return;
        }
        helper.succeed();
    }

    private static void feedsNothing(GameTestHelper helper) {
        // The creative pole's area stops short of the consumer, and a small pole wired to it stands
        // between: only a pole supplying the consumer's own spot could feed it.
        BlockPos pole = new BlockPos(18, 1, 3);
        BlockPos consumer = new BlockPos(19, 1, 3);
        Network.creative(helper, new BlockPos(1, 1, 3));
        Network.small(helper, new BlockPos(8, 1, 3));
        helper.setBlock(pole, WireworksRegistries.TRANSMISSION_POLE.get());
        Network.consumer(helper, consumer);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    if (Network.stored(helper, consumer) != 0L) {
                        helper.fail("a machine beside a Transmission Pole received energy", consumer);
                    }
                })
                .thenSucceed();
    }

    private static void wiresAndCuts(GameTestHelper helper) {
        // Placed with a transmission pole 20 blocks away in reach (the test config's 40): auto-wired.
        helper.setBlock(A, WireworksRegistries.TRANSMISSION_POLE.get());
        helper.setBlock(B, WireworksRegistries.TRANSMISSION_POLE.get());
        PoleNetworks.Wire wire = only(helper, A, B);
        if (wire == null) {
            return;
        }
        if (wire.system() != WireSystem.TRANSMISSION) {
            helper.fail("a wire between Transmission Poles is stored as " + wire.system(), A);
            return;
        }
        click(helper, A, B, PoleWiring.Click.CUT);
        if (wires(helper).contains(helper.absolutePos(A), helper.absolutePos(B))) {
            helper.fail("cutting left the wire standing", A);
            return;
        }
        click(helper, A, B, PoleWiring.Click.WIRED);
        if (only(helper, A, B) == null) {
            return;
        }
        helper.succeed();
    }

    private static void crossSystemRefused(GameTestHelper helper) {
        helper.setBlock(A, WireworksRegistries.TRANSMISSION_POLE.get());
        helper.setBlock(SMALL, WireworksRegistries.pole(PoleTier.SMALL).get());
        click(helper, A, SMALL, PoleWiring.Click.CROSS_SYSTEM);
        click(helper, SMALL, A, PoleWiring.Click.CROSS_SYSTEM);
        if (wires(helper).contains(helper.absolutePos(A), helper.absolutePos(SMALL))) {
            helper.fail("a wire joins a Transmission Pole and a Distribution Pole", A);
            return;
        }
        helper.succeed();
    }

    private static void autoWiresInsideItsSystem(GameTestHelper helper) {
        BlockPos otherSmall = new BlockPos(7, 1, 3);
        helper.setBlock(SMALL, WireworksRegistries.pole(PoleTier.SMALL).get());
        helper.setBlock(A, WireworksRegistries.TRANSMISSION_POLE.get());
        if (wires(helper).contains(helper.absolutePos(A), helper.absolutePos(SMALL))) {
            helper.fail("a placed Transmission Pole wired itself to a Distribution Pole", A);
            return;
        }
        helper.setBlock(B, WireworksRegistries.TRANSMISSION_POLE.get());
        if (!wires(helper).contains(helper.absolutePos(A), helper.absolutePos(B))) {
            helper.fail("a placed Transmission Pole did not wire itself to the one in reach", B);
            return;
        }
        helper.setBlock(otherSmall, WireworksRegistries.pole(PoleTier.SMALL).get());
        if (!wires(helper).contains(helper.absolutePos(SMALL), helper.absolutePos(otherSmall))) {
            helper.fail("a placed Distribution Pole did not wire itself to the Distribution Pole in reach", otherSmall);
            return;
        }
        if (wires(helper).contains(helper.absolutePos(A), helper.absolutePos(otherSmall))) {
            helper.fail("a placed Distribution Pole wired itself to a Transmission Pole", otherSmall);
            return;
        }
        helper.succeed();
    }

    private static void stacksAsAColumn(GameTestHelper helper) {
        Block pole = WireworksRegistries.TRANSMISSION_POLE.get();
        helper.setBlock(BASE, pole);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = new ItemStack(WireworksRegistries.TRANSMISSION_POLE_ITEM.get(), 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        for (int i = 0; i < 6; i++) {
            BlockPos absolute = helper.absolutePos(BASE);
            helper.useBlock(BASE, player, new BlockHitResult(
                    Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false));
        }
        if (held.getCount() != 8) {
            helper.fail("raising a column consumed " + (8 - held.getCount()) + " item(s)", BASE);
            return;
        }
        for (int i = 0; i < PoleColumn.MAX_SEGMENTS; i++) {
            if (!helper.getBlockState(BASE.above(i)).is(pole)) {
                helper.fail("segment " + i + " of the column is missing", BASE.above(i));
                return;
            }
        }
        if (!helper.getBlockState(BASE.above(PoleColumn.MAX_SEGMENTS)).is(Blocks.AIR)) {
            helper.fail("the column grew past " + PoleColumn.MAX_SEGMENTS + " segments", BASE.above(PoleColumn.MAX_SEGMENTS));
            return;
        }
        helper.getLevel().destroyBlock(helper.absolutePos(BASE), true, null);
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    int dropped = 0;
                    for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            helper.getBounds().inflate(2.0))) {
                        if (item.getItem().is(WireworksRegistries.TRANSMISSION_POLE_ITEM.get())) {
                            dropped += item.getItem().getCount();
                        }
                    }
                    if (dropped != 1) {
                        helper.fail("breaking a five-tall column dropped " + dropped + " pole(s), not 1", BASE);
                    }
                    if (!helper.getBlockState(BASE.above()).is(Blocks.AIR)) {
                        helper.fail("breaking the base left a segment standing", BASE.above());
                    }
                })
                .thenSucceed();
    }

    private static void wiresAreTheBases(GameTestHelper helper) {
        Block pole = WireworksRegistries.TRANSMISSION_POLE.get();
        helper.setBlock(A, pole);
        helper.setBlock(A.above(), pole);
        helper.setBlock(A.above(2), pole);
        helper.setBlock(B, pole);
        click(helper, A.above(2), B, PoleWiring.Click.CUT);
        if (wires(helper).contains(helper.absolutePos(A), helper.absolutePos(B))) {
            helper.fail("clicking the column's top segment did not reach the base's wire", A);
            return;
        }
        helper.succeed();
    }

    private static PoleNetworks.Wire only(GameTestHelper helper, BlockPos a, BlockPos b) {
        for (PoleNetworks.Wire wire : wires(helper).wires().all()) {
            PoleNetworks.Pos pa = pos(helper.absolutePos(a));
            PoleNetworks.Pos pb = pos(helper.absolutePos(b));
            if ((wire.a().equals(pa) && wire.b().equals(pb)) || (wire.a().equals(pb) && wire.b().equals(pa))) {
                return wire;
            }
        }
        helper.fail("no wire joins " + a + " and " + b, a);
        return null;
    }

    private static PoleNetworks.Pos pos(BlockPos p) {
        return new PoleNetworks.Pos(p.getX(), p.getY(), p.getZ());
    }

    private static void click(GameTestHelper helper, BlockPos anchor, BlockPos target, PoleWiring.Click expected) {
        PoleWiring.Click got = wires(helper).click(helper.getLevel(),
                helper.absolutePos(anchor), helper.absolutePos(target));
        if (got != expected) {
            helper.fail("clicking " + anchor + " then " + target + " was " + got + ", not " + expected, target);
        }
    }

    private static LevelWires wires(GameTestHelper helper) {
        return LevelWires.of(helper.getLevel());
    }
}
