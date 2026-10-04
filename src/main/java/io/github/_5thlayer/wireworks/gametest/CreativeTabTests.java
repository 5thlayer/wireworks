// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The Library's creative tab holds every pole, the tiers in order and the creative pole last, then the Solar Panel and the Accumulator. */
final class CreativeTabTests {

    private CreativeTabTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("creative_tab_holds_every_block_the_library_adds", 1, CreativeTabTests::holdsEveryItem);
    }

    private static void holdsEveryItem(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true,
                helper.getLevel().registryAccess());
        List<Item> shown = WireworksRegistries.CREATIVE_TAB.get().getDisplayItems().stream()
                .map(ItemStack::getItem)
                .toList();
        List<Item> expected = List.of(WireworksRegistries.poleItem(PoleTier.SMALL).get(),
                WireworksRegistries.poleItem(PoleTier.MEDIUM).get(), WireworksRegistries.poleItem(PoleTier.LARGE).get(),
                WireworksRegistries.CREATIVE_POLE_ITEM.get(), WireworksRegistries.SOLAR_PANEL_ITEM.get(),
                WireworksRegistries.ACCUMULATOR_ITEM.get());
        if (!shown.equals(expected)) {
            helper.fail("the creative tab shows " + shown + ", not " + expected);
            return;
        }
        helper.succeed();
    }
}
