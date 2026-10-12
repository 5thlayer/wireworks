// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.libworks.WorksTab;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The suite's Works tab holds every item the Library adds, the poles first and tar last, and the Library has no tab of its own. */
final class CreativeTabTests {

    private CreativeTabTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("works_tab_holds_every_item_the_library_adds", 1, CreativeTabTests::holdsEveryItem);
        tests.test("library_registers_no_creative_tab_of_its_own", 1, CreativeTabTests::registersNoTab);
    }

    private static void holdsEveryItem(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true,
                helper.getLevel().registryAccess());
        List<Item> shown = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(WorksTab.KEY).getDisplayItems().stream()
                .map(ItemStack::getItem)
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Wireworks.MOD_ID))
                .toList();
        List<Item> expected = List.of(WireworksRegistries.poleItem(PoleTier.SMALL).get(),
                WireworksRegistries.poleItem(PoleTier.MEDIUM).get(), WireworksRegistries.poleItem(PoleTier.LARGE).get(),
                WireworksRegistries.TRANSMISSION_POLE_ITEM.get(), WireworksRegistries.TRANSFORMER_ITEM.get(),
                WireworksRegistries.CREATIVE_POLE_ITEM.get(), WireworksRegistries.SOLAR_PANEL_ITEM.get(),
                WireworksRegistries.ACCUMULATOR_ITEM.get(), WireworksRegistries.BOILER_ITEM.get(),
                WireworksRegistries.STEAM_ENGINE_ITEM.get(), WireworksRegistries.TAR_ITEM.get());
        if (!shown.equals(expected)) {
            helper.fail("the Works tab shows " + shown + ", not " + expected);
            return;
        }
        helper.succeed();
    }

    private static void registersNoTab(GameTestHelper helper) {
        List<String> own = BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream()
                .filter(id -> id.getNamespace().equals(Wireworks.MOD_ID))
                .map(Object::toString)
                .toList();
        if (!own.isEmpty()) {
            helper.fail("the Library registers its own creative tabs " + own);
            return;
        }
        helper.succeed();
    }
}
