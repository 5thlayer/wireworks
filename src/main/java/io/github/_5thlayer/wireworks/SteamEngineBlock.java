// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Steam Engine's anchor: the bottom corner of its 2x1x2. Its front and back draw one engine across
 * the four blocks, which runs while {@code RUNNING} is set, so every block carries it
 * ({@link SteamEnginePartBlock}).
 */
public class SteamEngineBlock extends SteamGeneratorBlock {

    public static final String BLOCK_NAME = "steam_engine";

    public SteamEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    Footprint footprint() {
        return WireworksRegistries.STEAM_ENGINE_FOOTPRINT;
    }

    @Override
    BlockEntityType<SteamEngineBlockEntity> entityType() {
        return WireworksRegistries.STEAM_ENGINE_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamEngineBlockEntity(pos, state);
    }
}
