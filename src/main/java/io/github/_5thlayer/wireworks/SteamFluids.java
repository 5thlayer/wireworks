// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
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
import org.jspecify.annotations.Nullable;

/**
 * The two steams, each one fluid type with a still and a flowing fluid and a liquid block. Steam is
 * what the Boiler makes and the Steam Engine eats, Factorio's 165 degree steam, and has a bucket the
 * Boiler fills (#42); Superheated Steam has none and stays in tanks and pipes. Superheated Steam is what the Reactor makes and only the Steam
 * Turbine eats, Factorio's 500 degree steam, a second fluid rather than a temperature (ADR 0012).
 */
public final class SteamFluids {

    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Wireworks.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Wireworks.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wireworks.MOD_ID);

    /** Factorio's superheated steam, in degrees Celsius. */
    static final int SUPERHEATED_TEMPERATURE = 500;

    private static final Steam STEAM = new Steam("steam", BoilerSpec.TARGET_TEMPERATURE, WireworksRegistries.STEAM_BUCKET);
    private static final Steam SUPERHEATED_STEAM = new Steam("superheated_steam", SUPERHEATED_TEMPERATURE, null);

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = STEAM.type;
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM_SOURCE = STEAM.source;
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> STEAM_FLOWING = STEAM.flowing;
    public static final DeferredHolder<Block, SteamBlock> STEAM_BLOCK = STEAM.block;

    public static final DeferredHolder<FluidType, FluidType> SUPERHEATED_STEAM_TYPE = SUPERHEATED_STEAM.type;
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SUPERHEATED_STEAM_SOURCE = SUPERHEATED_STEAM.source;
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> SUPERHEATED_STEAM_FLOWING = SUPERHEATED_STEAM.flowing;
    public static final DeferredHolder<Block, SteamBlock> SUPERHEATED_STEAM_BLOCK = SUPERHEATED_STEAM.block;

    private SteamFluids() {
    }

    /** The liquid block's constructor is protected. */
    public static final class SteamBlock extends LiquidBlock {
        SteamBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
            super(fluid, properties);
        }
    }

    /** One steam's four registrations, named after it, and its bucket if it has one. */
    private static final class Steam {

        final DeferredHolder<FluidType, FluidType> type;
        final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        final DeferredHolder<Block, SteamBlock> block;
        private final @Nullable Supplier<? extends Item> bucket;

        Steam(String name, int celsius, @Nullable Supplier<? extends Item> bucket) {
            this.bucket = bucket;
            type = FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + Wireworks.MOD_ID + "." + name)
                    .lightLevel(0)
                    .density(-10)
                    // FluidType counts in kelvin.
                    .temperature(celsius + 273)
                    .viscosity(800)
                    .rarity(Rarity.COMMON)));
            source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
            flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties()));
            block = BLOCKS.registerBlock(name, props -> new SteamBlock(source.get(), liquid(props)));
        }

        private BaseFlowingFluid.Properties properties() {
            BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(type, source, flowing).block(block);
            return bucket == null ? properties : properties.bucket(bucket);
        }
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
