// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The Steam Turbine's anchor: the bottom middle of its 3x1x2. */
public class SteamTurbineBlock extends SteamGeneratorBlock {

    public static final String BLOCK_NAME = "steam_turbine";

    public SteamTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    Footprint footprint() {
        return WireworksRegistries.STEAM_TURBINE_FOOTPRINT;
    }

    @Override
    BlockEntityType<SteamTurbineBlockEntity> entityType() {
        return WireworksRegistries.STEAM_TURBINE_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamTurbineBlockEntity(pos, state);
    }
}
