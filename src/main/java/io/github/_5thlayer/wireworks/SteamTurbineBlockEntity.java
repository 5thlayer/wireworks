// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The Steam Turbine's anchor: it burns Superheated Steam, and only that, at {@link SteamTurbineSpec}'s rate. */
public class SteamTurbineBlockEntity extends SteamGeneratorBlockEntity {

    private static final SteamEngineSpec SPEC = SteamTurbineSpec.FACTORIO;
    /** How long the fan spins on after the last burn: a second. */
    private static final int RUNNING_HOLD_TICKS = 20;

    /** Ticks since the Turbine last burnt, up to {@link #RUNNING_HOLD_TICKS}; it starts still. */
    private int ticksSinceBurnt = RUNNING_HOLD_TICKS;

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.STEAM_TURBINE_ENTITY.get(), pos, state, SPEC,
                SteamFluids.SUPERHEATED_STEAM_SOURCE.get(), WireworksRegistries.STEAM_TURBINE_FOOTPRINT);
    }

    /**
     * Spins the fan while the Turbine burns and for {@link #RUNNING_HOLD_TICKS} after, so one at part
     * load, which burns on some ticks and not others, does not flicker.
     */
    @Override
    void afterTick(boolean burning) {
        ticksSinceBurnt = burning ? 0 : Math.min(ticksSinceBurnt + 1, RUNNING_HOLD_TICKS);
        boolean running = ticksSinceBurnt < RUNNING_HOLD_TICKS;
        BlockState state = getBlockState();
        if (level != null && state.getValue(SteamTurbineBlock.RUNNING) != running) {
            level.setBlock(worldPosition, state.setValue(SteamTurbineBlock.RUNNING, running), Block.UPDATE_ALL);
        }
    }

    public static SteamEngineSpec spec() {
        return SPEC;
    }
}
