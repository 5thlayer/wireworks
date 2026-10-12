// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.compat.emi;

import java.util.List;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.EmiBrewingRecipe;
import io.github._5thlayer.wireworks.BioresinBrewing;
import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Bioresin's brewing in EMI (#28). EMI draws a NeoForge brewing recipe by brewing a bare potion item, which is no
 * water bottle, so it shows an Uncraftable Potion brewing into nothing: that entry is dropped, and each pairing is
 * shown with its real bottle.
 *
 * <p>EMI finds this by its annotation and loads it only when EMI is installed; nothing outside this package names an
 * EMI type, so the Library loads without it.
 */
@EmiEntrypoint
public final class BioresinEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.removeRecipes(BioresinEmiPlugin::isBareBottleEntry);
        for (BioresinBrewing.Pairing pairing : BioresinBrewing.PAIRINGS) {
            Identifier ingredient = BuiltInRegistries.ITEM.getKey(pairing.ingredient());
            registry.addRecipe(new EmiBrewingRecipe(EmiStack.of(pairing.bottleStack()), EmiStack.of(pairing.ingredient()),
                    EmiStack.of(WireworksRegistries.BIORESIN.get()),
                    Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "/brewing/bioresin/" + ingredient.getNamespace() + "/" + ingredient.getPath())));
        }
    }

    /** EMI's own entry for a pairing: a brew on one of bioresin's ingredients that makes nothing. */
    private static boolean isBareBottleEntry(EmiRecipe recipe) {
        return recipe.getCategory() == VanillaEmiRecipeCategories.BREWING
                && recipe.getOutputs().stream().allMatch(EmiStack::isEmpty)
                && recipe.getInputs().stream().map(EmiIngredient::getEmiStacks).flatMap(List::stream)
                        .anyMatch(stack -> BioresinBrewing.PAIRINGS.stream().anyMatch(pairing -> stack.getItemStack().is(pairing.ingredient())));
    }
}
