// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The Steam Engine's anchor: it burns Steam at {@link SteamEngineSpec#FACTORIO}'s rate. */
public class SteamEngineBlockEntity extends SteamGeneratorBlockEntity {

    private static final SteamEngineSpec SPEC = SteamEngineSpec.FACTORIO;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.STEAM_ENGINE_ENTITY.get(), pos, state, SPEC,
                SteamFluids.STEAM_SOURCE.get(), WireworksRegistries.STEAM_ENGINE_FOOTPRINT);
    }

    public static SteamEngineSpec spec() {
        return SPEC;
    }
}
