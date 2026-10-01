// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.PoleTier;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * {@code wireworks-server.toml} reaches the tiers in a running game. The build copies
 * {@code src/gametest/wireworks-server.toml} into the server's config, with the substation off its
 * defaults; a tier still on its defaults means the config loaded but never configured it.
 */
final class ConfigTests {

    private ConfigTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("server_config_reaches_the_pole_tiers", 1, ConfigTests::reachesTheTiers);
    }

    private static void reachesTheTiers(GameTestHelper helper) {
        PoleTier tier = PoleTier.SUBSTATION;
        if (tier.supplySize() != 16 || tier.wireReach() != 21.5) {
            helper.fail("the substation supplies " + tier.supplySize() + " and reaches " + tier.wireReach()
                    + ", not the 16 and 21.5 that src/gametest/wireworks-server.toml configures");
            return;
        }
        if (PoleTier.maxWireReach() != 21.5) {
            helper.fail("the longest wire is " + PoleTier.maxWireReach() + ", not the substation's configured 21.5");
            return;
        }
        helper.succeed();
    }
}
