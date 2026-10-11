// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.Wireworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A crude oil for the tests to set as tar, since Wireworks ships none and the tests run without
 * Fieldworks: a fluid of the tests' own, which the {@code gametest/pack} data pack puts in
 * {@code c:crude_oil}. Registered only when game tests are enabled, and the pack only found then.
 */
final class TestCrude {

    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Wireworks.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Wireworks.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wireworks.MOD_ID);

    static final DeferredHolder<FluidType, FluidType> TYPE = FLUID_TYPES.register("gametest_crude",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid_type.wireworks.gametest_crude")));
    static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOURCE =
            FLUIDS.register("gametest_crude", () -> new BaseFlowingFluid.Source(properties()));
    static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING =
            FLUIDS.register("flowing_gametest_crude", () -> new BaseFlowingFluid.Flowing(properties()));
    static final DeferredHolder<Block, CrudeBlock> BLOCK = BLOCKS.registerBlock("gametest_crude",
            props -> new CrudeBlock(SOURCE.get(), props.replaceable().noCollision().noLootTable().liquid()));

    private TestCrude() {
    }

    static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        modBus.addListener(TestCrude::addPack);
    }

    /** The pack under {@code src/main/resources/gametest/pack}, always on, so no test has to switch it on. */
    private static void addPack(AddPackFindersEvent event) {
        event.addPackFinders(Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "gametest/pack"), PackType.SERVER_DATA,
                Component.literal("Wireworks game test data"), PackSource.BUILT_IN, true, Pack.Position.TOP);
    }

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING).block(BLOCK);
    }

    /** The liquid block's constructor is protected. */
    static final class CrudeBlock extends LiquidBlock {
        CrudeBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
            super(fluid, properties);
        }
    }
}
