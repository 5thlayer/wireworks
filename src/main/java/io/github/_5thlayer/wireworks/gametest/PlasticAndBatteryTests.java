// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;
import java.util.Map;

import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksRegistries;
import io.github._5thlayer.wireworks.WireworksTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * Plastic and the battery (#27): plastic blasted 1:1 from any Plastic Resin, the battery crafted from a Cathode, an
 * Anode and an Electrolyte, and the machines that take each.
 */
final class PlasticAndBatteryTests {

    private PlasticAndBatteryTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("every_plastic_resin_blasts_into_one_plastic", 1, PlasticAndBatteryTests::blastsPlastic);
        tests.test("battery_is_crafted_from_a_cathode_an_anode_and_an_electrolyte", 1, PlasticAndBatteryTests::craftsBattery);
        tests.test("the_battery_tags_hold_their_vanilla_defaults", 1, PlasticAndBatteryTests::batteryTagDefaults);
        tests.test("the_machines_take_plastic_or_batteries_and_the_small_and_medium_poles_neither", 1,
                PlasticAndBatteryTests::machinesTake);
    }

    private static void blastsPlastic(GameTestHelper helper) {
        List<RecipeHolder<?>> blasting = ownRecipes(helper).stream().filter(holder -> holder.value().getType() == RecipeType.BLASTING).toList();
        helper.assertValueEqual(blasting.size(), 1, "blasting recipes of " + Wireworks.MOD_ID);
        var resins = BuiltInRegistries.ITEM.getTagOrEmpty(WireworksTags.PLASTIC_RESINS);
        helper.assertTrue(resins.iterator().hasNext(), "#" + WireworksTags.PLASTIC_RESINS.location() + " is empty");
        for (Holder<Item> resin : resins) {
            var input = new SingleRecipeInput(new ItemStack(resin));
            var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.BLASTING, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent(), "no blasting recipe takes " + resin.getRegisteredName());
            ItemStack made = recipe.orElseThrow().value().assemble(input);
            helper.assertTrue(made.is(WireworksRegistries.PLASTIC.get()) && made.getCount() == 1,
                    resin.getRegisteredName() + " blasts into " + made + ", not one plastic");
        }
        helper.succeed();
    }

    private static void craftsBattery(GameTestHelper helper) {
        var input = CraftingInput.of(3, 1, List.of(new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE)));
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "no recipe crafts a copper ingot, an iron ingot and redstone");
        ItemStack made = recipe.orElseThrow().value().assemble(input);
        helper.assertTrue(made.is(WireworksRegistries.BATTERY.get()) && made.getCount() == 1, "they craft " + made + ", not one battery");
        helper.succeed();
    }

    private static void batteryTagDefaults(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(Items.COPPER_INGOT).is(WireworksTags.CATHODES), "a copper ingot is not a cathode");
        helper.assertTrue(new ItemStack(Items.IRON_INGOT).is(WireworksTags.ANODES), "an iron ingot is not an anode");
        helper.assertTrue(new ItemStack(Items.REDSTONE).is(WireworksTags.ELECTROLYTES), "redstone is not an electrolyte");
        helper.succeed();
    }

    /** Each machine's recipe takes plastic or batteries as listed, and nothing else of the two. */
    private static void machinesTake(GameTestHelper helper) {
        Item plastic = WireworksRegistries.PLASTIC.get();
        Item battery = WireworksRegistries.BATTERY.get();
        Map<String, List<Item>> takes = Map.of(
                "solar_panel", List.of(plastic),
                "transformer", List.of(plastic),
                "large_pole", List.of(plastic),
                "transmission_pole", List.of(plastic),
                "accumulator", List.of(battery),
                "small_pole", List.of(),
                "medium_pole", List.of());
        for (var machine : takes.entrySet()) {
            var key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, machine.getKey()));
            var holder = helper.getLevel().getServer().getRecipeManager().byKey(key);
            helper.assertTrue(holder.isPresent(), "no recipe " + key.identifier());
            List<Ingredient> ingredients = holder.orElseThrow().value().placementInfo().ingredients();
            for (Item part : List.of(plastic, battery)) {
                boolean taken = ingredients.stream().anyMatch(ingredient -> ingredient.test(new ItemStack(part)));
                helper.assertValueEqual(taken, machine.getValue().contains(part), machine.getKey() + " takes " + BuiltInRegistries.ITEM.getKey(part));
            }
        }
        helper.succeed();
    }

    private static List<RecipeHolder<?>> ownRecipes(GameTestHelper helper) {
        return helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().identifier().getNamespace().equals(Wireworks.MOD_ID)).toList();
    }
}
