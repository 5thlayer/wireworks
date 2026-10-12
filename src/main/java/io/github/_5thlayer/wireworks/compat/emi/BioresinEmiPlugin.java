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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * Bioresin's brewing in EMI (#28). EMI draws a NeoForge brewing recipe by brewing a bare potion item, which is no
 * water bottle, so it shows an Uncraftable Potion brewing into nothing: that entry is dropped, and each pairing is
 * shown with its real bottle.
 *
 * <p>{@link EmiBrewingRecipe} is EMI's own class, outside its API, so an EMI update may change it.
 *
 * <p>EMI finds this by its annotation and loads it only when EMI is installed; nothing outside this package names an
 * EMI type, so the Library loads without it.
 */
@EmiEntrypoint
public final class BioresinEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.removeRecipes(BioresinEmiPlugin::isEmisEmptyPairingEntry);
        for (BioresinBrewing.Pairing pairing : BioresinBrewing.PAIRINGS) {
            registry.addRecipe(new EmiBrewingRecipe(EmiStack.of(pairing.bottleStack()), EmiStack.of(pairing.ingredient()),
                    EmiStack.of(WireworksRegistries.BIORESIN.get()), recipeId(pairing)));
        }
    }

    private static Identifier recipeId(BioresinBrewing.Pairing pairing) {
        Identifier ingredient = BuiltInRegistries.ITEM.getKey(pairing.ingredient());
        return Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "/brewing/bioresin/" + ingredient.getNamespace() + "/" + ingredient.getPath());
    }

    /** EMI's own entry for a pairing: a bare potion item brewed with one of bioresin's ingredients, making nothing. */
    private static boolean isEmisEmptyPairingEntry(EmiRecipe recipe) {
        if (recipe.getCategory() != VanillaEmiRecipeCategories.BREWING || !recipe.getOutputs().stream().allMatch(EmiStack::isEmpty)) {
            return false;
        }
        List<ItemStack> inputs = recipe.getInputs().stream().map(EmiIngredient::getEmiStacks).flatMap(List::stream)
                .map(EmiStack::getItemStack).toList();
        return inputs.stream().anyMatch(BioresinEmiPlugin::isBarePotion) && BioresinBrewing.PAIRINGS.stream()
                .anyMatch(pairing -> inputs.stream().anyMatch(stack -> stack.is(pairing.ingredient())));
    }

    private static boolean isBarePotion(ItemStack stack) {
        return stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().isEmpty();
    }
}
