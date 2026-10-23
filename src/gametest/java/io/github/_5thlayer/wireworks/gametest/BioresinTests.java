// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.wireworks.BioresinBrewing;
import io.github._5thlayer.wireworks.WireworksRegistries;
import io.github._5thlayer.wireworks.WireworksTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/**
 * Bioresin (#28), a Plastic Resin made from plants: brewed in a vanilla brewing stand from a water bottle and sugar
 * cane or a vine, crafted from honeycomb and kelp, and on no pairing vanilla already brews.
 */
final class BioresinTests {

    /** The first brewing stand; each pairing's stand stands {@link #STAND_SPACING} blocks east of the last. */
    private static final BlockPos FIRST_STAND = new BlockPos(2, 1, 3);
    private static final int STAND_SPACING = 2;
    /** The platform's length east to west, which the stands must fit in. */
    private static final int PLATFORM_LENGTH = 23;

    private BioresinTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        // A brew takes 400 ticks, run by the stand itself.
        tests.test("each_brewing_pairing_gives_bioresin", 460, BioresinTests::brews);
        tests.test("vanillas_brewing_table_has_no_mix_on_a_bioresin_pairing", 1, BioresinTests::vanillaHasNoMix);
        tests.test("honeycomb_and_kelp_craft_bioresin", 1, BioresinTests::crafts);
        tests.test("bioresin_is_a_plastic_resin", 1, BioresinTests::isAPlasticResin);
    }

    /**
     * Each pairing in a stand of its own: three bottles, the ingredient and blaze powder to fuel it, and three bioresin
     * come out.
     */
    private static void brews(GameTestHelper helper) {
        List<BioresinBrewing.Pairing> pairings = BioresinBrewing.PAIRINGS;
        helper.assertTrue(FIRST_STAND.getX() + STAND_SPACING * (pairings.size() - 1) < PLATFORM_LENGTH, "the platform fits no stand for every pairing");
        List<BrewingStandBlockEntity> stands = new ArrayList<>();
        for (int i = 0; i < pairings.size(); i++) {
            BioresinBrewing.Pairing pairing = pairings.get(i);
            BlockPos pos = FIRST_STAND.east(STAND_SPACING * i);
            helper.setBlock(pos, Blocks.BREWING_STAND);
            BrewingStandBlockEntity stand = helper.getBlockEntity(pos, BrewingStandBlockEntity.class);
            for (int slot = 0; slot < 3; slot++) {
                stand.setItem(slot, pairing.bottleStack());
            }
            stand.setItem(3, new ItemStack(pairing.ingredient()));
            stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
            stands.add(stand);
        }

        helper.succeedWhen(() -> {
            for (int i = 0; i < stands.size(); i++) {
                for (int slot = 0; slot < 3; slot++) {
                    ItemStack held = stands.get(i).getItem(slot);
                    helper.assertTrue(held.is(WireworksRegistries.BIORESIN.get()),
                            "brewing with " + pairings.get(i).ingredient() + ", slot " + slot + " holds " + held);
                }
            }
        });
    }

    /**
     * Vanilla's table, built without the event mods register on and with every feature flag, experimental ones too,
     * mixes none of the pairings: a vanilla mix added on one later fails here rather than being shadowed by Wireworks'
     * recipe, which the stand tries first.
     */
    private static void vanillaHasNoMix(GameTestHelper helper) {
        helper.assertFalse(BioresinBrewing.PAIRINGS.isEmpty(), "bioresin has no brewing pairing");
        var builder = new PotionBrewing.Builder(FeatureFlags.REGISTRY.allFlags());
        PotionBrewing.addVanillaMixes(builder);
        PotionBrewing vanilla = builder.build();
        for (BioresinBrewing.Pairing pairing : BioresinBrewing.PAIRINGS) {
            ItemStack ingredient = new ItemStack(pairing.ingredient());
            helper.assertFalse(vanilla.hasMix(pairing.bottleStack(), ingredient), "vanilla already brews " + pairing.bottleStack() + " with " + ingredient);
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
