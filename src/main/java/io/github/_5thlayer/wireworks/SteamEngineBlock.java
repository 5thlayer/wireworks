// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The Steam Engine's anchor: the bottom corner of its 2x1x2. */
public class SteamEngineBlock extends EnergyAnchorBlock {

    public static final String BLOCK_NAME = "steam_engine";

    public SteamEngineBlock(Properties properties) {
        super(properties.requiresCorrectToolForDrops());
    }

    @Override
    Footprint footprint() {
        return WireworksRegistries.STEAM_ENGINE_FOOTPRINT;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamEngineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        if (level.isClientSide() || type != WireworksRegistries.STEAM_ENGINE_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof SteamEngineBlockEntity engine) {
                engine.serverTick();
            }
        };
    }
}
