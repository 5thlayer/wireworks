// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.wireworks.BioresinBrewing;
import io.github._5thlayer.wireworks.WireworksRegistries;
import io.github._5thlayer.wireworks.WireworksTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/**
 * Bioresin (#28), a Plastic Resin made from plants: brewed in a vanilla brewing stand from a water bottle and sugar
 * cane, crafted from honeycomb and kelp, and on no pairing vanilla already brews.
 */
final class BioresinTests {

    private static final BlockPos STAND = new BlockPos(4, 1, 4);

    private BioresinTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        // A brew takes 400 ticks, run by the stand itself.
        tests.test("a_water_bottle_brewed_with_sugar_cane_gives_bioresin", 460, BioresinTests::brews);
        tests.test("vanillas_brewing_table_has_no_mix_on_a_bioresin_pairing", 1, BioresinTests::replacesNoVanillaMix);
        tests.test("honeycomb_and_kelp_craft_bioresin", 1, BioresinTests::crafts);
        tests.test("bioresin_is_a_plastic_resin", 1, BioresinTests::isAPlasticResin);
    }

    /** Three water bottles and sugar cane, with blaze powder to fuel the stand: three bioresin come out. */
    private static void brews(GameTestHelper helper) {
        BioresinBrewing.Pairing sugarCane = BioresinBrewing.PAIRINGS.stream()
                .filter(pairing -> pairing.ingredient() == Items.SUGAR_CANE).findFirst().orElseThrow();
        helper.setBlock(STAND, Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = helper.getBlockEntity(STAND, BrewingStandBlockEntity.class);
        for (int slot = 0; slot < 3; slot++) {
            stand.setItem(slot, sugarCane.input());
        }
        stand.setItem(3, new ItemStack(Items.SUGAR_CANE));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));

        helper.succeedWhen(() -> {
            for (int slot = 0; slot < 3; slot++) {
                helper.assertTrue(stand.getItem(slot).is(WireworksRegistries.BIORESIN.get()), "slot " + slot + " holds " + stand.getItem(slot));
            }
        });
    }

    /**
     * Vanilla's table, built without the event mods register on, mixes none of the pairings: a vanilla mix added on
     * one later fails here rather than being shadowed by Wireworks' recipe, which the stand tries first.
     */
    private static void replacesNoVanillaMix(GameTestHelper helper) {
        helper.assertFalse(BioresinBrewing.PAIRINGS.isEmpty(), "bioresin has no brewing pairing");
        var builder = new PotionBrewing.Builder(helper.getLevel().enabledFeatures());
        PotionBrewing.addVanillaMixes(builder);
        PotionBrewing vanilla = builder.build();
        for (BioresinBrewing.Pairing pairing : BioresinBrewing.PAIRINGS) {
            ItemStack ingredient = new ItemStack(pairing.ingredient());
            helper.assertFalse(vanilla.hasMix(pairing.input(), ingredient), "vanilla already brews " + pairing.input() + " with " + ingredient);
            helper.assertTrue(helper.getLevel().potionBrewing().mix(ingredient, pairing.input()).is(WireworksRegistries.BIORESIN.get()),
                    "the server's table does not brew " + pairing.input() + " with " + ingredient + " into bioresin");
        }
        helper.succeed();
    }

    private static void crafts(GameTestHelper helper) {
        var input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.HONEYCOMB), new ItemStack(Items.KELP)));
        var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "no recipe crafts honeycomb and kelp");
        ItemStack made = recipe.orElseThrow().value().assemble(input);
        helper.assertTrue(made.is(WireworksRegistries.BIORESIN.get()), "honeycomb and kelp craft " + made);
        helper.succeed();
    }

    private static void isAPlasticResin(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(WireworksRegistries.BIORESIN.get()).is(WireworksTags.PLASTIC_RESINS),
                "bioresin is not in #" + WireworksTags.PLASTIC_RESINS.location());
        helper.succeed();
    }
}
