// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;
import java.util.Map;

import io.github._5thlayer.wireworks.WireworksRegistries;
import io.github._5thlayer.wireworks.WireworksTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

/**
 * What the Solar Panel, the Accumulator, the Boiler, the Steam Engine, the Steam Turbine and the
 * Reactor ship as data: the tags that give them their roles on a network, and recipes that craft the
 * item from the ingredients the tags name; and the Fuel Cell's two recipes and the two nuclear tags.
 */
final class EnergyBlockDataTests {

    private EnergyBlockDataTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("solar_panel_is_a_generator_and_the_accumulator_an_accumulator", 1, EnergyBlockDataTests::tagged);
        tests.test("solar_panel_is_crafted_from_its_recipe", 1, helper -> crafted(helper, 3, List.of(
                Items.GLASS, Items.DAYLIGHT_DETECTOR, Items.GLASS,
                Items.COPPER_INGOT, WireworksRegistries.PLASTIC.get(), Items.COPPER_INGOT,
                Items.AIR, Items.IRON_BLOCK, Items.AIR), WireworksRegistries.SOLAR_PANEL_ITEM.get()));
        tests.test("accumulator_is_crafted_from_its_recipe", 1, helper -> crafted(helper, 3, List.of(
                Items.IRON_BLOCK, WireworksRegistries.BATTERY.get(), Items.IRON_BLOCK,
                WireworksRegistries.BATTERY.get(), Items.COPPER_INGOT, WireworksRegistries.BATTERY.get(),
                Items.IRON_BLOCK, WireworksRegistries.BATTERY.get(), Items.IRON_BLOCK), WireworksRegistries.ACCUMULATOR_ITEM.get()));
        tests.test("boiler_is_crafted_from_its_recipe", 1, helper -> crafted(helper, 3, List.of(
                Items.IRON_INGOT, Items.BUCKET, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.FURNACE, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT), WireworksRegistries.BOILER_ITEM.get()));
        tests.test("steam_engine_is_crafted_from_its_recipe", 1, helper -> crafted(helper, 3, List.of(
                Items.IRON_INGOT, Items.PISTON, Items.IRON_INGOT,
                Items.COPPER_INGOT, Items.IRON_BLOCK, Items.COPPER_INGOT,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT), WireworksRegistries.STEAM_ENGINE_ITEM.get()));
        tests.test("steam_engine_is_a_generator_and_the_boiler_is_not", 1, EnergyBlockDataTests::steamTagged);
        tests.test("steam_turbine_is_crafted_from_its_recipe", 1, helper -> crafted(helper, 3, List.of(
                Items.COPPER_BLOCK, WireworksRegistries.PLASTIC.get(), Items.COPPER_BLOCK,
                Items.PISTON, Items.IRON_BLOCK, Items.PISTON,
                Items.COPPER_BLOCK, WireworksRegistries.PLASTIC.get(), Items.COPPER_BLOCK), WireworksRegistries.STEAM_TURBINE_ITEM.get()));
        tests.test("reactor_is_crafted_from_its_recipe", 1, helper -> crafted(helper, 3, List.of(
                Items.IRON_BLOCK, WireworksRegistries.PLASTIC.get(), Items.IRON_BLOCK,
                Items.NETHERITE_BLOCK, Items.BLAST_FURNACE, Items.NETHERITE_BLOCK,
                Items.IRON_BLOCK, WireworksRegistries.PLASTIC.get(), Items.IRON_BLOCK), WireworksRegistries.REACTOR_ITEM.get()));
        tests.test("fuel_cell_is_crafted_from_iron_round_glowstone_dust", 1, helper -> crafted(helper, 3, List.of(
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.GLOWSTONE_DUST, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT), WireworksRegistries.FUEL_CELL.get()));
        tests.test("depleted_fuel_cell_and_glowstone_dust_refill_a_fuel_cell", 1, helper -> crafted(helper, 2, List.of(
                WireworksRegistries.DEPLETED_FUEL_CELL.get(), Items.GLOWSTONE_DUST,
                Items.AIR, Items.AIR), WireworksRegistries.FUEL_CELL.get()));
        tests.test("the_nuclear_tags_hold_their_defaults_and_differ", 1, EnergyBlockDataTests::nuclearTags);
        tests.test("reactor_is_neither_a_generator_nor_an_accumulator", 1, helper -> {
            var reactor = WireworksRegistries.REACTOR.get().defaultBlockState();
            helper.assertFalse(reactor.is(WireworksTags.GENERATORS) || reactor.is(WireworksTags.ACCUMULATORS),
                    "the reactor makes no FE and must be in neither tag");
            helper.succeed();
        });
        tests.test("each_machine_keeps_its_own_hardness", 1, EnergyBlockDataTests::hardness);
        tests.test("steam_turbine_is_a_generator", 1, helper -> {
            if (!WireworksRegistries.STEAM_TURBINE.get().defaultBlockState().is(WireworksTags.GENERATORS)) {
                helper.fail("the steam turbine is not in wireworks:generators");
            }
            helper.succeed();
        });
    }

    private static void steamTagged(GameTestHelper helper) {
        if (!WireworksRegistries.STEAM_ENGINE.get().defaultBlockState().is(WireworksTags.GENERATORS)) {
            helper.fail("the steam engine is not in wireworks:generators");
        }
        if (WireworksRegistries.BOILER.get().defaultBlockState().is(WireworksTags.GENERATORS)
                || WireworksRegistries.BOILER.get().defaultBlockState().is(WireworksTags.ACCUMULATORS)) {
            helper.fail("the boiler makes no FE and must be in neither tag");
        }
        helper.succeed();
    }

    /** The Fuel Cell is Fission Fuel and glowstone dust a Fissile, and neither is the other. */
    private static void nuclearTags(GameTestHelper helper) {
        ItemStack cell = new ItemStack(WireworksRegistries.FUEL_CELL.get());
        ItemStack glowstone = new ItemStack(Items.GLOWSTONE_DUST);
        helper.assertTrue(cell.is(WireworksTags.FISSION_FUEL), "the fuel cell is not fission fuel");
        helper.assertTrue(glowstone.is(WireworksTags.FISSILE), "glowstone dust is not a fissile");
        helper.assertFalse(cell.is(WireworksTags.FISSILE), "the fuel cell is a fissile");
        helper.assertFalse(glowstone.is(WireworksTags.FISSION_FUEL), "glowstone dust is fission fuel");
        helper.assertFalse(new ItemStack(WireworksRegistries.DEPLETED_FUEL_CELL.get()).is(WireworksTags.FISSION_FUEL),
                "the depleted fuel cell is fission fuel");
        helper.succeed();
    }

    /** The Reactor and the Boiler set their own hardness, and the other machines keep the base's 2. */
    private static void hardness(GameTestHelper helper) {
        Map<Block, Float> expected = Map.of(
                WireworksRegistries.REACTOR.get(), 5.0F,
                WireworksRegistries.BOILER.get(), 3.5F,
                WireworksRegistries.SOLAR_PANEL.get(), 2.0F,
                WireworksRegistries.ACCUMULATOR.get(), 2.0F,
                WireworksRegistries.STEAM_ENGINE.get(), 2.0F,
                WireworksRegistries.STEAM_TURBINE.get(), 2.0F);
        for (var machine : expected.entrySet()) {
            float hardness = machine.getKey().defaultBlockState().getDestroySpeed(helper.getLevel(), BlockPos.ZERO);
            helper.assertValueEqual(hardness, machine.getValue(), machine.getKey().getDescriptionId() + "'s hardness");
        }
        helper.succeed();
    }

    private static void tagged(GameTestHelper helper) {
        if (!WireworksRegistries.SOLAR_PANEL.get().defaultBlockState().is(WireworksTags.GENERATORS)) {
            helper.fail("the solar panel is not in wireworks:generators");
        }
        if (!WireworksRegistries.ACCUMULATOR.get().defaultBlockState().is(WireworksTags.ACCUMULATORS)) {
            helper.fail("the accumulator is not in wireworks:accumulators");
        }
        if (WireworksRegistries.SOLAR_PANEL.get().defaultBlockState().is(WireworksTags.ACCUMULATORS)
                || WireworksRegistries.ACCUMULATOR.get().defaultBlockState().is(WireworksTags.GENERATORS)) {
            helper.fail("a block is in the other's tag");
        }
        helper.succeed();
    }

    private static void crafted(GameTestHelper helper, int side, List<Item> grid, Item expected) {
        CraftingInput input = CraftingInput.of(side, side, grid.stream().map(ItemStack::new).toList());
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        if (recipe.isEmpty()) {
            helper.fail("no crafting recipe matches the ingredients of " + expected);
            return;
        }
        ItemStack result = recipe.get().value().assemble(input);
        if (!result.is(expected) || result.getCount() != 1) {
            helper.fail("the recipe makes " + result + ", not one " + expected);
            return;
        }
        helper.succeed();
    }
}
