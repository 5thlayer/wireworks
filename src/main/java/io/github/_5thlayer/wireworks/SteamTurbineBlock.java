// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Steam Turbine's anchor: the bottom middle of its 3x1x2. Its fan, front and back, spins while
 * {@code RUNNING} is set, while it burns and for a second after. Only the anchor carries it: the fan is
 * on the anchor alone, and nothing gives off light.
 */
public class SteamTurbineBlock extends SteamGeneratorBlock {

    public static final String BLOCK_NAME = "steam_turbine";
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public SteamTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RUNNING);
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
