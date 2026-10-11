// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;
import java.util.Set;

import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

/**
 * The Library plays alone on Groundworks (ADR-0127): nothing else loads, every ingredient its recipes
 * consume has a vanilla item, and every tag its code reads holds an entry of Minecraft or Wireworks.
 */
final class PlaysAloneTests {

    // libworks is libworks-runtime, nested in the Library's own jar.
    private static final Set<String> BASE = Set.of("minecraft", "neoforge", "groundworks", "libworks", Wireworks.MOD_ID);

    private PlaysAloneTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("only_groundworks_is_loaded_beside_the_library", 1, PlaysAloneTests::onlyGroundworks);
        tests.test("every_ingredient_of_the_librarys_recipes_has_a_vanilla_item", 1, PlaysAloneTests::vanillaIngredients);
        tests.test("every_tag_wireworks_reads_holds_an_entry_of_minecraft_or_wireworks", 1, PlaysAloneTests::tagsPlayAlone);
    }

    private static void onlyGroundworks(GameTestHelper helper) {
        List<String> extra = ModList.get().getMods().stream().map(m -> m.getModId()).filter(id -> !BASE.contains(id)).toList();
        helper.assertTrue(extra.isEmpty(), "the run loads " + extra + " beside Groundworks");
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

    // c:crude_oil is left out: it is Fieldworks' crude, so alone the tar interaction that reads it stays
    // inert, and plastic still has vanilla's resin clump (ADR-0127).
    private static void tagsPlayAlone(GameTestHelper helper) {
        List<TagKey<?>> read = List.of(WireworksTags.WIRE_TOOLS, WireworksTags.GENERATORS, WireworksTags.ACCUMULATORS);
        Set<String> alone = Set.of("minecraft", Wireworks.MOD_ID);
        for (TagKey<?> tag : read) {
            helper.assertTrue(reachesAlone(helper, tag, alone), tag.location() + " holds no entry of " + alone);
        }
        helper.succeed();
    }

    private static <T> boolean reachesAlone(GameTestHelper helper, TagKey<T> tag, Set<String> namespaces) {
        return helper.getLevel().registryAccess().lookupOrThrow(tag.registry()).get(tag).stream()
                .flatMap(set -> set.stream())
                .anyMatch(holder -> holder.unwrapKey().filter(key -> namespaces.contains(key.identifier().getNamespace())).isPresent());
    }
}
