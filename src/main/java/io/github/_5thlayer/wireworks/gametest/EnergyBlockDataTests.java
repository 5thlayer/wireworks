// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.wireworks.WireworksRegistries;
import io.github._5thlayer.wireworks.WireworksTags;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * What the Solar Panel and the Accumulator ship as data: the tags that give them their roles on a
 * network, and recipes that craft the item from the ingredients the tags name.
 */
final class EnergyBlockDataTests {

    private EnergyBlockDataTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("solar_panel_is_a_generator_and_the_accumulator_an_accumulator", 1, EnergyBlockDataTests::tagged);
        tests.test("solar_panel_is_crafted_from_its_recipe", 1, helper -> crafted(helper, 3, List.of(
                Items.GLASS, Items.DAYLIGHT_DETECTOR, Items.GLASS,
                Items.COPPER_INGOT, Items.REDSTONE, Items.COPPER_INGOT,
                Items.AIR, Items.IRON_BLOCK, Items.AIR), WireworksRegistries.SOLAR_PANEL_ITEM.get()));
        tests.test("accumulator_is_crafted_from_its_recipe", 1, helper -> crafted(helper, 3, List.of(
                Items.IRON_BLOCK, Items.REDSTONE_BLOCK, Items.IRON_BLOCK,
                Items.REDSTONE_BLOCK, Items.COPPER_INGOT, Items.REDSTONE_BLOCK,
                Items.IRON_BLOCK, Items.REDSTONE_BLOCK, Items.IRON_BLOCK), WireworksRegistries.ACCUMULATOR_ITEM.get()));
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
