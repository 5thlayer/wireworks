// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Steam, the fluid the Boiler makes and the Steam Engine eats: Factorio's 165 degree steam, one
 * fluid type with a still and a flowing fluid and a liquid block. It has no bucket, so steam stays in
 * tanks and pipes.
 */
public final class SteamFluids {

    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Wireworks.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Wireworks.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wireworks.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + Wireworks.MOD_ID + ".steam")
                    .lightLevel(0)
                    .density(-10)
                    // FluidType counts in kelvin.
                    .temperature(BoilerSpec.TARGET_TEMPERATURE + 273)
                    .viscosity(800)
                    .rarity(Rarity.COMMON)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM_SOURCE =
            FLUIDS.register("steam", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> STEAM_FLOWING =
            FLUIDS.register("flowing_steam", () -> new BaseFlowingFluid.Flowing(properties()));

    public static final DeferredHolder<Block, SteamBlock> STEAM_BLOCK =
            BLOCKS.registerBlock("steam", props -> new SteamBlock(STEAM_SOURCE.get(), liquid(props)));

    private SteamFluids() {
    }

    /** The liquid block's constructor is protected. */
    public static final class SteamBlock extends LiquidBlock {
        SteamBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
            super(fluid, properties);
        }
    }

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM_SOURCE, STEAM_FLOWING).block(STEAM_BLOCK);
    }

    private static BlockBehaviour.Properties liquid(BlockBehaviour.Properties props) {
        return props
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollision()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
    }

    static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
    }
}
