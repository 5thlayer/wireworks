// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;

import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.libworks.testkit.WorksTabCheck;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The suite's Works tab holds every item the Library adds, the poles first, no other tab holds
 * one, and the Library has no tab of its own (#23, FactoryWorks ADR-0128).
 */
final class CreativeTabTests {

    private static final List<String> WORKS_TAB_ORDER = List.of(
            "small_pole", "medium_pole", "large_pole", "transmission_pole", "transformer", "creative_pole",
            "solar_panel", "accumulator", "boiler", "steam_engine", "steam_turbine", "reactor",
            "steam_bucket", "bioresin", "plastic", "battery", "fuel_cell", "depleted_fuel_cell");

    private CreativeTabTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("works_tab_shows_every_item_the_poles_first", 1,
                helper -> WorksTabCheck.inOrder(helper, Wireworks.MOD_ID, WORKS_TAB_ORDER));
        tests.test("no_other_tab_holds_a_library_item", 1,
                helper -> WorksTabCheck.noOtherTab(helper, Wireworks.MOD_ID));
        tests.test("library_registers_no_creative_tab_of_its_own", 1, CreativeTabTests::noOwnTab);
    }

    private static void noOwnTab(GameTestHelper helper) {
        var own = BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream()
                .filter(id -> id.getNamespace().equals(Wireworks.MOD_ID))
                .toList();
        if (!own.isEmpty()) {
            helper.fail("the Library registers its own creative tabs " + own);
            return;
        }
        helper.succeed();
    }
}
