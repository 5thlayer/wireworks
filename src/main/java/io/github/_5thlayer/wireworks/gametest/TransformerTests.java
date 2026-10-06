// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.LevelWires;
import io.github._5thlayer.wireworks.NetworkExchange;
import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.PoleNetworks;
import io.github._5thlayer.wireworks.PoleWiring;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;
import io.github._5thlayer.wireworks.WireSystem;
import io.github._5thlayer.wireworks.WireworksRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.List;

/**
 * The Transformer in a running game (ADR 0008): crafted, wired to a pole of either system, and
 * settling Districts through a Transmission Line. The rules are in the JVM tests; what they cannot
 * see is the block, the recipe, the stored wires and the network tick that applies a whole Electric
 * Network's flows in one transaction.
 *
 * <h2>The layout</h2>
 *
 * <p>On one row, z 3: District A's side at x 1 to 3, a Transformer at x 8, Transmission Poles at x 11
 * and 15, a second Transformer at x 17 and a small pole at x 20 standing for District B, with its
 * consumer two blocks south. Each Transformer wires to the nearest pole of each system in reach, which
 * is why the tests that place one run alone ({@code Registrar.isolated}), out of reach of a neighbour.
 */
final class TransformerTests {

    private static final BlockPos CREATIVE = new BlockPos(1, 1, 3);
    private static final BlockPos A_POLE = new BlockPos(3, 1, 3);
    private static final BlockPos ACCUMULATOR = new BlockPos(1, 1, 3);
    private static final BlockPos A_CONSUMER = new BlockPos(3, 1, 5);
    private static final BlockPos A_TRANSFORMER = new BlockPos(8, 1, 3);
    private static final BlockPos LINE_NEAR = new BlockPos(11, 1, 3);
    private static final BlockPos LINE_FAR = new BlockPos(15, 1, 3);
    private static final BlockPos B_TRANSFORMER = new BlockPos(17, 1, 3);
    private static final BlockPos B_POLE = new BlockPos(20, 1, 3);
    private static final BlockPos B_CONSUMER = new BlockPos(20, 1, 5);

    private static final int SETTLE = Network.SETTLE;

    private TransformerTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("transformer_is_crafted_from_its_recipe", 1, TransformerTests::crafted);
        tests.isolated("transformer_wires_one_pole_of_each_system_and_never_another_transformer", 20,
                TransformerTests::wiresEachSide);
        tests.isolated("creative_pole_feeds_another_district_through_transformers", 120,
                TransformerTests::creativeFeedsAnotherDistrict);
        tests.isolated("a_district_keeps_its_own_power_and_exports_only_what_is_left", 120,
                TransformerTests::keepsItsOwnPowerFirst);
        tests.isolated("a_distribution_pole_counts_and_lists_only_its_own_district", 120,
                TransformerTests::countsItsOwnDistrict);
        tests.isolated("breaking_the_transformer_leaves_its_district_on_its_own_book", 200,
                TransformerTests::breakingLeavesDistrictsApart);
        tests.isolated("transformer_is_one_block_not_a_column", 20, TransformerTests::notAColumn);
    }

    private static void crafted(GameTestHelper helper) {
        List<Item> grid = List.of(
                Items.IRON_INGOT, Items.COPPER_INGOT, Items.IRON_INGOT,
                Items.COPPER_INGOT, Items.IRON_BLOCK, Items.COPPER_INGOT,
                Items.IRON_INGOT, Items.COPPER_INGOT, Items.IRON_INGOT);
        CraftingInput input = CraftingInput.of(3, 3, grid.stream().map(ItemStack::new).toList());
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        if (recipe.isEmpty()) {
            helper.fail("no crafting recipe matches ICI / CBC / ICI");
            return;
        }
        ItemStack result = recipe.get().value().assemble(input);
        if (!result.is(WireworksRegistries.TRANSFORMER_ITEM.get()) || result.getCount() != 1) {
            helper.fail("the recipe makes " + result + ", not one Transformer");
            return;
        }
        helper.succeed();
    }

    private static void wiresEachSide(GameTestHelper helper) {
        Network.small(helper, A_POLE);
        helper.setBlock(LINE_NEAR, WireworksRegistries.TRANSMISSION_POLE.get());
        helper.setBlock(LINE_FAR, WireworksRegistries.TRANSMISSION_POLE.get());
        helper.setBlock(A_TRANSFORMER, WireworksRegistries.TRANSFORMER.get());
        if (!wired(helper, A_TRANSFORMER, A_POLE, WireSystem.DISTRIBUTION)
                || !wired(helper, A_TRANSFORMER, LINE_NEAR, WireSystem.TRANSMISSION)) {
            return;
        }
        if (wires(helper).contains(helper.absolutePos(A_TRANSFORMER), helper.absolutePos(LINE_FAR))) {
            helper.fail("a placed Transformer wired a second Transmission Pole", LINE_FAR);
            return;
        }
        helper.setBlock(B_TRANSFORMER, WireworksRegistries.TRANSFORMER.get());
        PoleWiring.Click click = wires(helper).click(helper.getLevel(),
                helper.absolutePos(A_TRANSFORMER), helper.absolutePos(B_TRANSFORMER));
        if (click != PoleWiring.Click.REFUSED) {
            helper.fail("wiring a Transformer to another was " + click + ", not REFUSED", B_TRANSFORMER);
            return;
        }
        helper.succeed();
    }

    /** A creative pole's District is fully fed, and its surplus is all the other District has. */
    private static void creativeFeedsAnotherDistrict(GameTestHelper helper) {
        Network.creative(helper, CREATIVE);
        Network.consumer(helper, A_CONSUMER);
        line(helper);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    if (Network.stored(helper, A_CONSUMER) != TestConsumer.CAPACITY) {
                        helper.fail("the self-sufficient District's machine holds "
                                + Network.stored(helper, A_CONSUMER) + " FE, not a full "
                                + TestConsumer.CAPACITY, A_CONSUMER);
                    }
                    if (Network.stored(helper, B_CONSUMER) != TestConsumer.CAPACITY) {
                        helper.fail("the other District's machine holds " + Network.stored(helper, B_CONSUMER)
                                + " FE across two Transformers and a line, not a full " + TestConsumer.CAPACITY,
                                B_CONSUMER);
                    }
                    // Asking every tick from here, so each tick has something to cross.
                    helper.onEachTick(() -> Network.drain(helper, B_CONSUMER));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    NetworkExchange exported = helper.getBlockEntity(CREATIVE, SupplyAreaPoleBlockEntity.class)
                            .networkExchange();
                    NetworkExchange imported = helper.getBlockEntity(B_POLE, SupplyAreaPoleBlockEntity.class)
                            .networkExchange();
                    if (exported.exported() <= 0L) {
                        helper.fail("the District with the creative pole reads " + exported.exported()
                                + " FE exported", CREATIVE);
                    }
                    if (imported.imported() <= 0L) {
                        helper.fail("the other District reads " + imported.imported() + " FE imported", B_POLE);
                    }
                })
                .thenSucceed();
    }

    /**
     * District A has an accumulator holding 10,000 FE and nothing else to make power, and a machine
     * asking for 1,000. District B has a machine asking for 1,000 and nothing at all. Both end full
     * and the accumulator gave exactly the 2,000 they took: nothing is made or lost crossing.
     */
    private static void keepsItsOwnPowerFirst(GameTestHelper helper) {
        Network.footprint(helper, WireworksRegistries.ACCUMULATOR_FOOTPRINT, ACCUMULATOR, Direction.NORTH);
        EnergyHandler face = Network.energy(helper, ACCUMULATOR);
        if (!Network.fill(face, 10_000L)) {
            helper.fail("the accumulator took less than 10000 FE", ACCUMULATOR);
            return;
        }
        Network.small(helper, A_POLE);
        Network.consumer(helper, A_CONSUMER);
        line(helper);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    long a = Network.stored(helper, A_CONSUMER);
                    long b = Network.stored(helper, B_CONSUMER);
                    long left = face.getAmountAsLong();
                    if (a != TestConsumer.CAPACITY || b != TestConsumer.CAPACITY) {
                        helper.fail("the Districts' machines hold " + a + " and " + b + " FE, not "
                                + TestConsumer.CAPACITY + " each", B_CONSUMER);
                    }
                    if (left != 10_000L - a - b) {
                        helper.fail("the accumulator holds " + left + " FE after giving " + (10_000L - left)
                                + " to machines that took " + (a + b), ACCUMULATOR);
                    }
                })
                .thenSucceed();
    }

    /** Two Districts of one pole each, joined by a line: a pole's reading and book are its District's. */
    private static void countsItsOwnDistrict(GameTestHelper helper) {
        Network.small(helper, A_POLE);
        Network.consumer(helper, A_CONSUMER);
        line(helper);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    SupplyAreaPoleBlockEntity pole = helper.getBlockEntity(A_POLE, SupplyAreaPoleBlockEntity.class);
                    int counted = pole.networkReading().poles();
                    if (counted != 1) {
                        helper.fail("the pole reads " + counted + " poles, not the 1 of its District", A_POLE);
                    }
                    int listed = io.github._5thlayer.wireworks.ElectricNetworks.of(helper.getLevel())
                            .networkOf(pole).size();
                    if (listed != 1) {
                        helper.fail("networkOf lists " + listed + " poles, not the 1 of its District", A_POLE);
                    }
                })
                .thenSucceed();
    }

    private static void breakingLeavesDistrictsApart(GameTestHelper helper) {
        Network.creative(helper, CREATIVE);
        Network.consumer(helper, A_CONSUMER);
        line(helper);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    if (Network.stored(helper, B_CONSUMER) <= 0L) {
                        helper.fail("the other District was never fed, so the break proves nothing", B_CONSUMER);
                    }
                    helper.setBlock(A_TRANSFORMER, Blocks.AIR);
                    Network.drain(helper, A_CONSUMER);
                    Network.drain(helper, B_CONSUMER);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    if (Network.stored(helper, A_CONSUMER) <= 0L) {
                        helper.fail("the District with the creative pole stopped settling on its own book",
                                A_CONSUMER);
                    }
                    long b = Network.stored(helper, B_CONSUMER);
                    if (b != 0L) {
                        helper.fail("the other District took " + b + " FE after its Transformer to the first"
                                + " District was broken", B_CONSUMER);
                    }
                })
                .thenSucceed();
    }

    private static void notAColumn(GameTestHelper helper) {
        Block transformer = WireworksRegistries.TRANSFORMER.get();
        helper.setBlock(A_TRANSFORMER, transformer);
        helper.setBlock(A_TRANSFORMER.above(), transformer);
        if (PoleColumn.height(helper.getLevel(), helper.absolutePos(A_TRANSFORMER)) != 1
                || !PoleColumn.isBase(helper.getLevel(), helper.absolutePos(A_TRANSFORMER.above()))) {
            helper.fail("two stacked Transformers are one column", A_TRANSFORMER);
            return;
        }
        helper.getLevel().destroyBlock(helper.absolutePos(A_TRANSFORMER), false, null);
        if (!helper.getBlockState(A_TRANSFORMER.above()).is(transformer)) {
            helper.fail("breaking a Transformer dropped the one above it", A_TRANSFORMER.above());
            return;
        }
        helper.succeed();
    }

    /** Both sides of the line, placed after the Districts so each Transformer wires into them. */
    private static void line(GameTestHelper helper) {
        Network.small(helper, B_POLE);
        Network.consumer(helper, B_CONSUMER);
        helper.setBlock(LINE_NEAR, WireworksRegistries.TRANSMISSION_POLE.get());
        helper.setBlock(LINE_FAR, WireworksRegistries.TRANSMISSION_POLE.get());
        helper.setBlock(A_TRANSFORMER, WireworksRegistries.TRANSFORMER.get());
        helper.setBlock(B_TRANSFORMER, WireworksRegistries.TRANSFORMER.get());
    }

    private static boolean wired(GameTestHelper helper, BlockPos a, BlockPos b, WireSystem system) {
        for (PoleNetworks.Wire wire : wires(helper).wires().all()) {
            PoleNetworks.Pos pa = pos(helper.absolutePos(a));
            PoleNetworks.Pos pb = pos(helper.absolutePos(b));
            if ((wire.a().equals(pa) && wire.b().equals(pb)) || (wire.a().equals(pb) && wire.b().equals(pa))) {
                if (wire.system() != system) {
                    helper.fail("the wire from " + a + " to " + b + " is stored as " + wire.system()
                            + ", not " + system, a);
                    return false;
                }
                return true;
            }
        }
        helper.fail("no wire joins " + a + " and " + b, a);
        return false;
    }

    private static PoleNetworks.Pos pos(BlockPos p) {
        return new PoleNetworks.Pos(p.getX(), p.getY(), p.getZ());
    }

    private static LevelWires wires(GameTestHelper helper) {
        return LevelWires.of(helper.getLevel());
    }
}
