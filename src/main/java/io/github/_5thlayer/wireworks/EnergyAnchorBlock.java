// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;

/**
 * The anchor of a footprint that holds energy. Its block model draws the whole footprint, and it
 * takes the rest of the footprint with it when it goes. A piston does not move it, since the parts
 * it would leave behind have no way back.
 */
abstract class EnergyAnchorBlock extends Block implements EntityBlock {

    EnergyAnchorBlock(Properties properties) {
        super(properties.strength(2.0F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));
        registerDefaultState(getStateDefinition().any().setValue(Footprint.FACING, Direction.NORTH));
    }

    abstract Footprint footprint();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Footprint.FACING);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        footprint().teardown(level, pos, state.getValue(Footprint.FACING), pos);
    }
}
