// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.wireworks.Wireworks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Library's game tests, run by the {@code gameTestServer} Gradle run: the Minecraft-integration
 * seam, with a real player on a real server. Each test stands on the {@code gametest/platform}
 * structure, a stone floor that {@code scripts/build-gametest-structures.py} writes, and sets up
 * what it needs itself, so the setup is in the diff.
 */
public final class WireworksGameTests {

    private static final Identifier PLATFORM = id("gametest/platform");

    /**
     * Set by the Library's own {@code gameTestServer} run. A Consumer's game test server also has
     * game tests enabled, and there the test pack's group would be claimed before the Consumer's own.
     */
    private static final String OWN_RUN = "wireworks.gametest";

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, Wireworks.MOD_ID);

    static {
        TEST_TYPES.register("code", () -> CodeGameTest.CODEC);
    }

    private WireworksGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_TYPES.register(modBus);
        if (!GameTestHooks.isGametestEnabled() || !Boolean.getBoolean(OWN_RUN)) {
            return;
        }
        TestConsumer.register(modBus);
        PoleReplaceTests.statePackGroup();
        modBus.addListener(WireworksGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        // Registered rather than borrowed, since the event hands out no lookup for vanilla's.
        var environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        var tests = new Registrar(event, environment);
        LoadTests.register(tests);
        CreativeTabTests.register(tests);
        ConfigTests.register(tests);
        ElectricNetworkTests.register(tests);
        PoleWireTests.register(tests);
        TransmissionPoleTests.register(tests);
        TransformerTests.register(tests);
        PoleColumnCostTests.register(tests);
        WireGestureTests.register(tests);
        PolePlanTests.register(tests);
        PoleReplaceTests.register(tests);
        SolarPanelTests.register(tests);
        AccumulatorTests.register(tests);
        EnergyFootprintTests.register(tests);
        EnergyBlockDataTests.register(tests);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, path);
    }

    /** What a test class is handed: a name, a tick budget and a body per test. */
    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {

        void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
            var id = id(name);
            CodeGameTest.define(id, body);
            event.registerTest(id, new CodeGameTest(id, new TestData<>(environment, PLATFORM, maxTicks, 0, true, Rotation.NONE)));
        }

        /**
         * A test that runs alone. Tests of one environment run together, side by side, and a wire's
         * reach is not bounded by its test's structure: a Transformer reaches 30 blocks, so it would
         * wire itself to a neighbouring test's poles. An environment of its own is a batch of its own.
         */
        void isolated(String name, int maxTicks, Consumer<GameTestHelper> body) {
            var alone = event.registerEnvironment(id("alone/" + name), new TestEnvironmentDefinition.AllOf(List.of()));
            var id = id(name);
            CodeGameTest.define(id, body);
            event.registerTest(id, new CodeGameTest(id, new TestData<>(alone, PLATFORM, maxTicks, 0, true, Rotation.NONE)));
        }
    }
}
