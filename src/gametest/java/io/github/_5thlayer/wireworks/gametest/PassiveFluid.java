// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * A stand-in for a Pipeworks pipe where vanilla has none, as the water cauldron is for water: a
 * tank behind a copper block that answers the fluid capability on every side and pushes nothing.
 * Registered only in the Module's own runs, on a vanilla block no other test uses.
 */
final class PassiveFluid {

    private static final Map<BlockPos, FluidStacksResourceHandler> TANKS = new HashMap<>();

    private PassiveFluid() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(PassiveFluid::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Fluid.BLOCK,
                (level, pos, state, entity, side) -> TANKS.get(pos), Blocks.COPPER_BLOCK);
    }

    /** Puts a tank of {@code capacity} mB at {@code pos}, holding {@code amount} of {@code fluid}. */
    static void stand(GameTestHelper helper, BlockPos pos, FluidResource fluid, int amount, int capacity) {
        BlockPos absolute = helper.absolutePos(pos);
        FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, capacity);
        if (amount > 0) {
            tank.set(0, fluid, amount);
        }
        TANKS.put(absolute, tank);
        helper.getLevel().setBlock(absolute, Blocks.COPPER_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
    }

    static int amount(GameTestHelper helper, BlockPos pos) {
        ResourceHandler<FluidResource> tank = TANKS.get(helper.absolutePos(pos));
        return tank.getAmountAsInt(0);
    }
}
