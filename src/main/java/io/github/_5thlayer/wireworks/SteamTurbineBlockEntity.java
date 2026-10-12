// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The Steam Turbine's anchor: it burns Superheated Steam, and only that, at {@link SteamTurbineSpec}'s rate. */
public class SteamTurbineBlockEntity extends SteamGeneratorBlockEntity {

    private static final SteamEngineSpec SPEC = SteamTurbineSpec.FACTORIO;

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.STEAM_TURBINE_ENTITY.get(), pos, state, SPEC,
                SteamFluids.SUPERHEATED_STEAM_SOURCE.get(), WireworksRegistries.STEAM_TURBINE_FOOTPRINT);
    }

    public static SteamEngineSpec spec() {
        return SPEC;
    }
}
