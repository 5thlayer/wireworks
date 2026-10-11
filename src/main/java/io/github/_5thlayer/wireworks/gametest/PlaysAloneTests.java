// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;
import java.util.Set;

import io.github._5thlayer.wireworks.Wireworks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

/** The Library plays alone on Groundworks (ADR-0127): nothing else loads, and every ingredient its recipes consume has a vanilla item. */
final class PlaysAloneTests {

    private static final Set<String> BASE = Set.of("minecraft", "neoforge", "groundworks", Wireworks.MOD_ID);

    private PlaysAloneTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("only_groundworks_is_loaded_beside_the_library", 1, PlaysAloneTests::onlyGroundworks);
        tests.test("every_ingredient_of_the_librarys_recipes_has_a_vanilla_item", 1, PlaysAloneTests::vanillaIngredients);
    }

    private static void onlyGroundworks(GameTestHelper helper) {
        List<String> extra = ModList.get().getMods().stream().map(m -> m.getModId()).filter(id -> !BASE.contains(id)).toList();
        if (!extra.isEmpty()) {
            helper.fail("the run loads " + extra + " beside Groundworks");
            return;
        }
        helper.succeed();
    }

    private static void vanillaIngredients(GameTestHelper helper) {
        int checked = 0;
        for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (!holder.id().identifier().getNamespace().equals(Wireworks.MOD_ID)) {
                continue;
            }
            for (Ingredient ingredient : holder.value().placementInfo().ingredients()) {
                checked++;
                boolean vanilla = ingredient.items()
                        .anyMatch(item -> BuiltInRegistries.ITEM.getKey(item.value()).getNamespace().equals("minecraft"));
                if (!vanilla) {
                    helper.fail(holder.id().identifier() + " consumes an ingredient with no vanilla item");
                    return;
                }
            }
        }
        if (checked == 0) {
            helper.fail("no recipe of " + Wireworks.MOD_ID + " was found");
            return;
        }
        helper.succeed();
    }
}
