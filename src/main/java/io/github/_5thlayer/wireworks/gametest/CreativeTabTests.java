// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github._5thlayer.libworks.WorksTab;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The suite's Works tab holds every item the Library registers, the poles first, no other tab holds
 * one, and the Library has no tab of its own (#23, FactoryWorks ADR-0128).
 */
final class CreativeTabTests {

    private CreativeTabTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("works_tab_holds_every_item_the_library_adds", 1, CreativeTabTests::holdsEveryItem);
        tests.test("works_tab_shows_the_poles_first", 1, CreativeTabTests::showsItemsInOrder);
        tests.test("library_registers_no_creative_tab_of_its_own", 1, CreativeTabTests::registersNoTab);
        tests.test("no_other_tab_holds_a_library_item", 1, CreativeTabTests::noOtherTab);
    }

    private static void holdsEveryItem(GameTestHelper helper) {
        Set<Item> shown = new HashSet<>(shownItems(helper));
        Set<Item> registered = BuiltInRegistries.ITEM.stream()
                .filter(CreativeTabTests::isOwn)
                .collect(Collectors.toSet());
        if (!shown.equals(registered)) {
            helper.fail("the Works tab shows " + shown + ", not every registered item " + registered);
            return;
        }
        helper.succeed();
    }

    private static void showsItemsInOrder(GameTestHelper helper) {
        List<Item> shown = shownItems(helper);
        List<Item> expected = List.of(WireworksRegistries.poleItem(PoleTier.SMALL).get(),
                WireworksRegistries.poleItem(PoleTier.MEDIUM).get(), WireworksRegistries.poleItem(PoleTier.LARGE).get(),
                WireworksRegistries.TRANSMISSION_POLE_ITEM.get(), WireworksRegistries.TRANSFORMER_ITEM.get(),
                WireworksRegistries.CREATIVE_POLE_ITEM.get(), WireworksRegistries.SOLAR_PANEL_ITEM.get(),
                WireworksRegistries.ACCUMULATOR_ITEM.get(), WireworksRegistries.BOILER_ITEM.get(),
                WireworksRegistries.STEAM_ENGINE_ITEM.get(), WireworksRegistries.BIORESIN.get());
        if (!shown.equals(expected)) {
            helper.fail("the Works tab shows " + shown + ", not " + expected);
            return;
        }
        helper.succeed();
    }

    private static void noOtherTab(GameTestHelper helper) {
        // An empty Works tab would let the loop below pass without the Library's items reaching any tab.
        if (shownItems(helper).isEmpty()) {
            helper.fail("the Works tab shows no Wireworks item, so no tab was filled");
            return;
        }
        for (var entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            CreativeModeTab tab = entry.getValue();
            // The search tab lists every other tab's items.
            if (entry.getKey() == WorksTab.KEY || tab.getType() == CreativeModeTab.Type.SEARCH) {
                continue;
            }
            List<Item> own = ownItems(tab);
            if (!own.isEmpty()) {
                helper.fail("the tab " + entry.getKey().identifier() + " shows " + own);
                return;
            }
        }
        helper.succeed();
    }

    private static List<Item> shownItems(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true,
                helper.getLevel().registryAccess());
        return ownItems(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(WorksTab.KEY));
    }

    private static List<Item> ownItems(CreativeModeTab tab) {
        return tab.getDisplayItems().stream()
                .map(ItemStack::getItem)
                .filter(CreativeTabTests::isOwn)
                .toList();
    }

    private static boolean isOwn(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Wireworks.MOD_ID);
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
