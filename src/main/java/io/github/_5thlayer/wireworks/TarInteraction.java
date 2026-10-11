// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Crude meeting lava sets as tar: a source block of any fluid in {@code c:crude_oil} that touches
 * lava, on any side, becomes a tar block, and the lava is left as it was. Flowing crude does nothing,
 * so a source cannot make tar without end.
 *
 * <p>A fluid interaction runs only for the liquid whose neighbour changed, and looks at its sides and
 * the block above it, so it is added from both ends. Lava's answers crude arriving beside it, on it
 * or under it. Crude's answers lava arriving, but which fluids are crude is a tag, unknown until the
 * data loads, so that one is added to every fluid type and asks the tag each time.
 */
final class TarInteraction {

    /** The fizz lava makes setting water as stone. */
    private static final int FIZZ = 1501;

    private TarInteraction() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(TarInteraction::addInteractions);
    }

    private static void addInteractions(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FluidInteractionRegistry.addInteraction(NeoForgeMod.LAVA_TYPE.value(),
                    new InteractionInformation(TarInteraction::crudeArrived, TarInteraction::setCrudeAround));
            InteractionInformation lavaArrived = new InteractionInformation(TarInteraction::lavaArrived,
                    WireworksRegistries.TAR.get().defaultBlockState());
            for (FluidType type : NeoForgeRegistries.FLUID_TYPES) {
                FluidInteractionRegistry.addInteraction(type, lavaArrived);
            }
        });
    }

    /** Lava at {@code lava} has a crude source at {@code beside}, or under it, where the registry does not look. */
    private static boolean crudeArrived(Level level, BlockPos lava, BlockPos beside, FluidState lavaState) {
        return isCrudeSource(level, beside) || isCrudeSource(level, lava.below());
    }

    private static void setCrudeAround(Level level, BlockPos lava, BlockPos beside, FluidState lavaState) {
        BlockState tar = WireworksRegistries.TAR.get().defaultBlockState();
        for (Direction side : Direction.values()) {
            BlockPos at = lava.relative(side);
            if (isCrudeSource(level, at)) {
                level.setBlockAndUpdate(at, EventHooks.fireFluidPlaceBlockEvent(level, at, lava, tar));
                level.levelEvent(FIZZ, at, 0);
            }
        }
    }

    /** A crude source at {@code crude} has lava at {@code beside}, or under it, where the registry does not look. */
    private static boolean lavaArrived(Level level, BlockPos crude, BlockPos beside, FluidState crudeState) {
        return crudeState.isSource() && crudeState.is(WireworksTags.CRUDE_OIL)
                && (isLava(level, beside) || isLava(level, crude.below()));
    }

    /** Only a liquid block: a fluid held in another block, as water in a waterlogged one, never sets. */
    private static boolean isCrudeSource(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.isSource() && fluid.is(WireworksTags.CRUDE_OIL)
                && level.getBlockState(pos).getBlock() instanceof LiquidBlock;
    }

    private static boolean isLava(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.LAVA);
    }
}
