// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksTags;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

/**
 * The Library plays alone on Groundworks (FactoryWorks ADR-0127): nothing else loads, every ingredient its loaded recipes
 * consume has an item of Minecraft or Wireworks, every recipe is a vanilla type, and every tag its code reads or its data names holds an entry of Minecraft or Wireworks.
 */
final class PlaysAloneTests {

    // libworks is libworks-runtime, nested in the Library's own jar.
    private static final Set<String> LOADED_ALONE = Set.of("minecraft", "neoforge", "groundworks", "libworks", Wireworks.MOD_ID);

    private static final Set<String> FILLS_ALONE = Set.of("minecraft", Wireworks.MOD_ID);

    private static final Pattern HASHED_TAG = Pattern.compile("\"#([a-z0-9_.-]+:[a-z0-9_./-]+)\"");
    private static final Pattern TAG_FIELD = Pattern.compile("\"tag\"\\s*:\\s*\"([a-z0-9_.-]+:[a-z0-9_./-]+)\"");

    // A tag entry marked "required": false adds what a pack may fill and consumes nothing alone.
    private static final Pattern OPTIONAL_ENTRY = Pattern.compile(
            "\\{\\s*(\"id\"\\s*:\\s*\"#[^\"]+\"\\s*,\\s*\"required\"\\s*:\\s*false|\"required\"\\s*:\\s*false\\s*,\\s*\"id\"\\s*:\\s*\"#[^\"]+\")\\s*}");

    private PlaysAloneTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("only_groundworks_is_loaded_beside_the_library", 1, PlaysAloneTests::onlyGroundworks);
        tests.test("every_ingredient_of_the_librarys_recipes_has_an_item_of_minecraft_or_wireworks", 1, PlaysAloneTests::ingredientsPlayAlone);
        tests.test("every_recipe_of_the_library_is_a_vanilla_type", 1, PlaysAloneTests::vanillaTypes);
        tests.test("every_tag_wireworks_reads_holds_an_entry_of_minecraft_or_wireworks", 1, PlaysAloneTests::tagsPlayAlone);
        tests.test("every_tag_the_librarys_data_names_holds_an_entry_of_minecraft_or_wireworks", 1, PlaysAloneTests::dataTagsFillAlone);
    }

    private static void onlyGroundworks(GameTestHelper helper) {
        List<String> extra = ModList.get().getMods().stream().map(m -> m.getModId()).filter(id -> !LOADED_ALONE.contains(id)).toList();
        helper.assertTrue(extra.isEmpty(), "the run loads " + extra + " beside Groundworks");
        helper.succeed();
    }

    private static void ingredientsPlayAlone(GameTestHelper helper) {
        int checked = 0;
        for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (!holder.id().identifier().getNamespace().equals(Wireworks.MOD_ID)) {
                continue;
            }
            for (Ingredient ingredient : holder.value().placementInfo().ingredients()) {
                checked++;
                // Its own items too (FactoryWorks ADR-0127), each of which its own tests make from vanilla ones.
                boolean alone = ingredient.items()
                        .anyMatch(item -> FILLS_ALONE.contains(BuiltInRegistries.ITEM.getKey(item.value()).getNamespace()));
                if (!alone) {
                    helper.fail(holder.id().identifier() + " consumes an ingredient with no item of " + FILLS_ALONE);
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

    private static void vanillaTypes(GameTestHelper helper) {
        List<RecipeHolder<?>> own = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().identifier().getNamespace().equals(Wireworks.MOD_ID)).toList();
        helper.assertFalse(own.isEmpty(), "no recipe of " + Wireworks.MOD_ID + " was found");
        for (RecipeHolder<?> holder : own) {
            Recipe<?> recipe = holder.value();
            Identifier type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            Identifier serializer = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
            helper.assertTrue(type.getNamespace().equals("minecraft") && serializer.getNamespace().equals("minecraft"),
                    holder.id().identifier() + " is a " + type + " recipe read by " + serializer);
        }
        helper.succeed();
    }

    private static void tagsPlayAlone(GameTestHelper helper) {
        List<TagKey<?>> consumed = List.of(WireworksTags.WIRE_TOOLS, WireworksTags.GENERATORS, WireworksTags.ACCUMULATORS,
                WireworksTags.PLASTIC_RESINS, WireworksTags.CATHODES, WireworksTags.ANODES, WireworksTags.ELECTROLYTES,
                WireworksTags.FISSION_FUEL, WireworksTags.FISSILE);
        for (TagKey<?> tag : consumed) {
            helper.assertTrue(reachesAlone(helper, tag), tag.location() + " holds no entry of " + FILLS_ALONE);
        }
        helper.succeed();
    }

    /**
     * A recipe that names a tag no mod fills does not load, and one whose tag holds only another mod's items is
     * dropped as unplaceable, so neither reaches the recipe manager: the tags are read from the jar's data.
     */
    private static void dataTagsFillAlone(GameTestHelper helper) {
        Set<Identifier> tags = new TreeSet<>();
        try {
            for (Path root : ModList.get().getModFileById(Wireworks.MOD_ID).getFile().getContents().getContentRoots()) {
                Path data = root.resolve("data");
                if (!Files.isDirectory(data)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(data)) {
                    for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                        String json = OPTIONAL_ENTRY.matcher(Files.readString(file)).replaceAll("");
                        for (Pattern named : List.of(HASHED_TAG, TAG_FIELD)) {
                            named.matcher(json).results().forEach(tag -> tags.add(Identifier.parse(tag.group(1))));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var registries = helper.getLevel().registryAccess();
        List<Identifier> empty = tags.stream()
                .filter(tag -> registries.registries().noneMatch(registry -> fillsAlone(registry.value(), tag))).toList();
        helper.assertTrue(empty.isEmpty(), "these tags hold no entry of " + FILLS_ALONE + ": " + empty);
        helper.succeed();
    }

    private static <T> boolean fillsAlone(Registry<T> registry, Identifier tag) {
        return registry.get(TagKey.create(registry.key(), tag)).stream().flatMap(HolderSet::stream)
                .anyMatch(entry -> entry.unwrapKey().filter(key -> FILLS_ALONE.contains(key.identifier().getNamespace())).isPresent());
    }

    private static <T> boolean reachesAlone(GameTestHelper helper, TagKey<T> tag) {
        return fillsAlone(helper.getLevel().registryAccess().lookupOrThrow(tag.registry()), tag.location());
    }
}
