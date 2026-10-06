// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.TransformerSpec;
import io.github._5thlayer.wireworks.TransmissionSpec;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * {@code wireworks-server.toml} reaches the tiers in a running game. The build copies
 * {@code src/gametest/wireworks-server.toml} into the server's config, with the large pole off its
 * defaults; a tier still on its defaults means the config loaded but never configured it.
 */
final class ConfigTests {

    private ConfigTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("server_config_reaches_the_pole_tiers", 1, ConfigTests::reachesTheTiers);
        tests.test("server_config_reaches_the_transmission_pole", 1, ConfigTests::reachesTheTransmissionPole);
        tests.test("server_config_reaches_the_transformer", 1, ConfigTests::reachesTheTransformer);
    }

    private static void reachesTheTiers(GameTestHelper helper) {
        PoleTier tier = PoleTier.LARGE;
        if (tier.supplySize() != 16 || tier.wireReach() != 21.5) {
            helper.fail("the [large] section did not reach the large pole: it supplies " + tier.supplySize()
                    + " and reaches " + tier.wireReach() + ", not the 16 and 21.5 that src/gametest/wireworks-server.toml configures");
            return;
        }
        if (PoleTier.maxWireReach() != 21.5) {
            helper.fail("the longest wire is " + PoleTier.maxWireReach() + ", not the large pole's configured 21.5");
            return;
        }
        helper.succeed();
    }

    private static void reachesTheTransmissionPole(GameTestHelper helper) {
        double reach = TransmissionSpec.wireReach();
        if (reach != 40.0) {
            helper.fail("the [transmission] section did not reach the Transmission Pole: it reaches " + reach
                    + ", not the 40.0 that src/gametest/wireworks-server.toml configures");
            return;
        }
        helper.succeed();
    }

    private static void reachesTheTransformer(GameTestHelper helper) {
        if (TransformerSpec.lineReach() != 30.0 || TransformerSpec.districtReach() != 11.0) {
            helper.fail("the [transformer] section did not reach the Transformer: it reaches "
                    + TransformerSpec.lineReach() + " and " + TransformerSpec.districtReach()
                    + ", not the 30.0 and 11.0 that src/gametest/wireworks-server.toml configures");
            return;
        }
        helper.succeed();
    }
}
